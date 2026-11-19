package br.com.fiap.boletimplus.service;

import br.com.fiap.boletimplus.exception.ApiException;
import br.com.fiap.boletimplus.security.UsuarioAutenticado;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Idempotência dos POST que criam recurso, pelo header Idempotency-Key (draft IETF "The
 * Idempotency-Key HTTP Header Field"), igual à API .NET. A primeira resposta de sucesso fica
 * guardada por 24 h, por usuário, rota e chave:
 * <ul>
 * <li>sem a chave (ou com mais de 255 caracteres): 400;</li>
 * <li>mesma chave e mesmo corpo: devolve a resposta guardada, sem executar de novo
 * (header Idempotent-Replayed: true);</li>
 * <li>mesma chave e corpo diferente: 422;</li>
 * <li>mesma chave com a primeira ainda em andamento: 409.</li>
 * </ul>
 * Guardado em memória (como o IDistributedCache em memória do .NET): reiniciar a API limpa.
 */
@Service
public class IdempotenciaService {

    public static final String HEADER = "Idempotency-Key";
    public static final String HEADER_REPETICAO = "Idempotent-Replayed";
    public static final int CHAVE_MAX = 255;
    private static final Duration VALIDADE = Duration.ofHours(24);
    private static final Logger log = LoggerFactory.getLogger(IdempotenciaService.class);

    private final JsonMapper jsonMapper;
    private final Map<String, RespostaGuardada> guardadas = new ConcurrentHashMap<>();
    private final Set<String> emAndamento = ConcurrentHashMap.newKeySet();

    public IdempotenciaService(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * Executa `acao` uma vez por chave. `dados` são o que identifica o pedido (cod da rota e corpo):
     * a mesma chave com outros dados responde 422.
     */
    @SuppressWarnings("unchecked")
    public <T> ResponseEntity<T> executar(UsuarioAutenticado quem, HttpServletRequest req, String chave,
                                          List<?> dados, Supplier<ResponseEntity<T>> acao) {
        if (chave == null || chave.isBlank() || chave.length() > CHAVE_MAX) {
            throw ApiException.requisicaoInvalida(
                    "Envie o header " + HEADER + " (até " + CHAVE_MAX + " caracteres), um valor novo por operação.");
        }
        String chaveCompleta = "idem:" + quem.usuarioCod() + ":" + req.getMethod() + ":" + req.getRequestURI() + ":" + chave;
        String impressao = impressao(dados);

        if (!emAndamento.add(chaveCompleta)) {
            throw ApiException.conflito("Uma requisição com esta Idempotency-Key ainda está sendo processada.");
        }
        try {
            removerVencidas();
            RespostaGuardada anterior = guardadas.get(chaveCompleta);
            if (anterior != null) {
                if (!anterior.impressao().equals(impressao)) {
                    throw new ApiException(HttpStatus.valueOf(422),
                            "Esta Idempotency-Key já foi usada com outro corpo. Gere uma chave nova.");
                }
                log.info("Requisição repetida: devolvendo a resposta guardada de {}", chaveCompleta);
                return (ResponseEntity<T>) ResponseEntity.status(anterior.resposta().getStatusCode())
                        .headers(anterior.resposta().getHeaders())
                        .header(HEADER_REPETICAO, "true")
                        .body(anterior.resposta().getBody());
            }

            ResponseEntity<T> resposta = acao.get();
            if (resposta.getStatusCode().is2xxSuccessful()) {
                guardadas.put(chaveCompleta, new RespostaGuardada(resposta, impressao, Instant.now().plus(VALIDADE)));
            }
            return resposta;
        } finally {
            emAndamento.remove(chaveCompleta);
        }
    }

    /** SHA-256 do JSON dos dados do pedido. */
    private String impressao(List<?> dados) {
        try {
            byte[] json = jsonMapper.writeValueAsString(dados).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void removerVencidas() {
        Instant agora = Instant.now();
        guardadas.values().removeIf(r -> r.expiraEm().isBefore(agora));
    }

    private record RespostaGuardada(ResponseEntity<?> resposta, String impressao, Instant expiraEm) {
    }
}
