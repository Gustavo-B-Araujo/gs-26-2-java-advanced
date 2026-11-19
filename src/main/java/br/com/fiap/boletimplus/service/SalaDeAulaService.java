package br.com.fiap.boletimplus.service;

import br.com.fiap.boletimplus.dto.AlunoDaTurmaResponse;
import br.com.fiap.boletimplus.dto.AvaliacaoDetalheResponse;
import br.com.fiap.boletimplus.dto.AvaliacaoResumoResponse;
import br.com.fiap.boletimplus.dto.ChamadaRequest;
import br.com.fiap.boletimplus.dto.DadosAvaliacaoRequest;
import br.com.fiap.boletimplus.dto.ItemChamadaResponse;
import br.com.fiap.boletimplus.dto.NotaLancadaResponse;
import br.com.fiap.boletimplus.dto.NotasRequest;
import br.com.fiap.boletimplus.dto.TurmaDetalheResponse;
import br.com.fiap.boletimplus.exception.ApiException;
import br.com.fiap.boletimplus.model.Avaliacao;
import br.com.fiap.boletimplus.model.Boletim;
import br.com.fiap.boletimplus.model.ListaPresenca;
import br.com.fiap.boletimplus.model.TipoNota;
import br.com.fiap.boletimplus.model.TipoPresenca;
import br.com.fiap.boletimplus.model.Turma;
import br.com.fiap.boletimplus.model.TurmaAluno;
import br.com.fiap.boletimplus.repository.AvaliacaoRepository;
import br.com.fiap.boletimplus.repository.BoletimRepository;
import br.com.fiap.boletimplus.repository.ListaPresencaRepository;
import br.com.fiap.boletimplus.repository.TipoNotaRepository;
import br.com.fiap.boletimplus.repository.TipoPresencaRepository;
import br.com.fiap.boletimplus.repository.TurmaAlunoRepository;
import br.com.fiap.boletimplus.security.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Sala de aula: detalhe da turma, avaliações, notas (Boletim) e chamada (ListaPresenca). */
@Service
public class SalaDeAulaService {

    private final ConsultaService consultas;
    private final AvaliacaoRepository avaliacoes;
    private final BoletimRepository notas;
    private final ListaPresencaRepository presencas;
    private final TurmaAlunoRepository matriculas;
    private final TipoNotaRepository tiposNota;
    private final TipoPresencaRepository tiposPresenca;

    public SalaDeAulaService(ConsultaService consultas, AvaliacaoRepository avaliacoes, BoletimRepository notas,
                             ListaPresencaRepository presencas, TurmaAlunoRepository matriculas,
                             TipoNotaRepository tiposNota, TipoPresencaRepository tiposPresenca) {
        this.consultas = consultas;
        this.avaliacoes = avaliacoes;
        this.notas = notas;
        this.presencas = presencas;
        this.matriculas = matriculas;
        this.tiposNota = tiposNota;
        this.tiposPresenca = tiposPresenca;
    }

    @Transactional(readOnly = true)
    public TurmaDetalheResponse obterTurma(UsuarioAutenticado quem, Long turmaCod) {
        Turma turma = consultas.exigirLeituraDaTurma(quem, turmaCod);
        return TurmaDetalheResponse.de(consultas.resumo(turma), consultas.alunosDaTurma(turmaCod),
                consultas.avaliacoesDaTurma(turmaCod));
    }

    // --- Avaliações --------------------------------------------------------

    @Transactional
    public Long criarAvaliacao(UsuarioAutenticado quem, Long turmaCod, DadosAvaliacaoRequest dados) {
        Turma turma = consultas.exigirProfessorDaTurma(quem, turmaCod);
        TipoNota tipo = validarAvaliacao(turmaCod, dados, null);
        return avaliacoes.save(new Avaliacao(turma, tipo, dados.nome().trim(), dados.dataAvaliacao())).getCod();
    }

    @Transactional(readOnly = true)
    public AvaliacaoDetalheResponse obterAvaliacao(UsuarioAutenticado quem, Long avaliacaoCod) {
        Avaliacao avaliacao = buscarAvaliacao(avaliacaoCod);
        Turma turma = consultas.exigirLeituraDaTurma(quem, avaliacao.getTurma().getCod());

        AvaliacaoResumoResponse resumo = consultas.avaliacoesDaTurma(turma.getCod()).stream()
                .filter(a -> a.cod().equals(avaliacaoCod)).findFirst().orElseThrow();
        Map<Long, Double> notaPorMatricula = notas.findByAvaliacaoCod(avaliacaoCod).stream()
                .collect(Collectors.toMap(b -> b.getTurmaAluno().getCod(), b -> b.getNota().doubleValue()));
        List<NotaLancadaResponse> lancadas = consultas.alunosDaTurma(turma.getCod()).stream()
                .map(a -> new NotaLancadaResponse(a.turmaAlunoCod(), a.nome(), notaPorMatricula.get(a.turmaAlunoCod())))
                .toList();

        return new AvaliacaoDetalheResponse(resumo.cod(), resumo.nome(), resumo.codTipoNota(), resumo.tipoNota(),
                resumo.dataAvaliacao(), resumo.notasLancadas(), consultas.resumo(turma), lancadas);
    }

    @Transactional
    public void atualizarAvaliacao(UsuarioAutenticado quem, Long avaliacaoCod, DadosAvaliacaoRequest dados) {
        Avaliacao avaliacao = buscarAvaliacao(avaliacaoCod);
        consultas.exigirProfessorDaTurma(quem, avaliacao.getTurma().getCod());
        TipoNota tipo = validarAvaliacao(avaliacao.getTurma().getCod(), dados, avaliacaoCod);
        avaliacao.alterar(tipo, dados.nome().trim(), dados.dataAvaliacao());
    }

    /** Apaga as notas (Boletim) antes da avaliação, por causa da FK cod_avaliacao. Uma transação. */
    @Transactional
    public void excluirAvaliacao(UsuarioAutenticado quem, Long avaliacaoCod) {
        Avaliacao avaliacao = buscarAvaliacao(avaliacaoCod);
        consultas.exigirProfessorDaTurma(quem, avaliacao.getTurma().getCod());
        notas.deleteAll(notas.findByAvaliacaoCod(avaliacaoCod));
        notas.flush();
        avaliacoes.delete(avaliacao);
    }

    /** Upsert por uq_boletim (cod_turma_aluno, cod_avaliacao). Nota null apaga a linha. */
    @Transactional
    public void salvarNotas(UsuarioAutenticado quem, Long avaliacaoCod, NotasRequest dados) {
        Avaliacao avaliacao = buscarAvaliacao(avaliacaoCod);
        consultas.exigirProfessorDaTurma(quem, avaliacao.getTurma().getCod());

        List<NotasRequest.Item> itens = dados.notas() == null ? List.of() : dados.notas();
        Map<Long, TurmaAluno> validos = matriculasDaTurma(avaliacao.getTurma().getCod());
        for (NotasRequest.Item item : itens) {
            if (!validos.containsKey(item.turmaAlunoCod())) throw ApiException.requisicaoInvalida("Aluno fora da turma.");
            RegrasDeNegocio.exigir(RegrasDeNegocio.validarNota(item.nota()));
        }

        Map<Long, Boletim> atuais = notas.findByAvaliacaoCod(avaliacaoCod).stream()
                .collect(Collectors.toMap(b -> b.getTurmaAluno().getCod(), Function.identity()));
        for (NotasRequest.Item item : ultimoPorAluno(itens, NotasRequest.Item::turmaAlunoCod)) {
            Boletim atual = atuais.get(item.turmaAlunoCod());
            if (item.nota() == null) {
                if (atual != null) notas.delete(atual);
            } else if (atual != null) {
                atual.setNota(RegrasDeNegocio.arredondarNota(item.nota()));
            } else {
                notas.save(new Boletim(validos.get(item.turmaAlunoCod()), avaliacao,
                        RegrasDeNegocio.arredondarNota(item.nota())));
            }
        }
    }

    // --- Chamada ------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ItemChamadaResponse> obterChamada(UsuarioAutenticado quem, Long turmaCod, LocalDate data) {
        consultas.exigirLeituraDaTurma(quem, turmaCod);
        Map<Long, ListaPresenca> registros = presencas.daTurmaNoDia(turmaCod, data).stream()
                .collect(Collectors.toMap(l -> l.getTurmaAluno().getCod(), Function.identity()));
        return consultas.alunosDaTurma(turmaCod).stream()
                .map((AlunoDaTurmaResponse a) -> {
                    ListaPresenca r = registros.get(a.turmaAlunoCod());
                    return new ItemChamadaResponse(a.turmaAlunoCod(), a.nome(),
                            r == null ? null : r.getTipoPresenca().getCod(),
                            r == null ? null : r.getObservacao());
                })
                .toList();
    }

    /** Upsert por uq_lista_presenca (cod_turma_aluno, data_presenca). Sem data futura. */
    @Transactional
    public void salvarChamada(UsuarioAutenticado quem, Long turmaCod, LocalDate data, ChamadaRequest dados) {
        consultas.exigirProfessorDaTurma(quem, turmaCod);
        RegrasDeNegocio.exigir(RegrasDeNegocio.validarDataPresenca(data, RegrasDeNegocio.hoje()));

        List<ChamadaRequest.Item> itens = dados.itens() == null ? List.of() : dados.itens();
        Map<Long, TurmaAluno> validos = matriculasDaTurma(turmaCod);
        Map<Long, TipoPresenca> tipos = tiposPresenca.findAll().stream()
                .collect(Collectors.toMap(TipoPresenca::getCod, Function.identity()));
        for (ChamadaRequest.Item item : itens) {
            if (!validos.containsKey(item.turmaAlunoCod())) throw ApiException.requisicaoInvalida("Aluno fora da turma.");
            if (!tipos.containsKey(item.codTipoPresenca())) {
                throw ApiException.requisicaoInvalida("Tipo de presença inválido.");
            }
            RegrasDeNegocio.exigir(RegrasDeNegocio.validarObservacao(item.observacao()));
        }

        Map<Long, ListaPresenca> atuais = presencas.daTurmaNoDia(turmaCod, data).stream()
                .collect(Collectors.toMap(l -> l.getTurmaAluno().getCod(), Function.identity()));
        for (ChamadaRequest.Item item : ultimoPorAluno(itens, ChamadaRequest.Item::turmaAlunoCod)) {
            String observacao = item.observacao() == null || item.observacao().isBlank()
                    ? null : item.observacao().trim();
            TipoPresenca tipo = tipos.get(item.codTipoPresenca());
            ListaPresenca atual = atuais.get(item.turmaAlunoCod());
            if (atual != null) {
                atual.alterar(tipo, observacao);
            } else {
                presencas.save(new ListaPresenca(validos.get(item.turmaAlunoCod()), tipo, observacao, data));
            }
        }
    }

    // --- Apoio ---------------------------------------------------------------

    private Avaliacao buscarAvaliacao(Long avaliacaoCod) {
        return avaliacoes.findById(avaliacaoCod)
                .orElseThrow(() -> ApiException.naoEncontrado("Avaliação não encontrada."));
    }

    private Map<Long, TurmaAluno> matriculasDaTurma(Long turmaCod) {
        return matriculas.findByTurmaCod(turmaCod).stream()
                .collect(Collectors.toMap(TurmaAluno::getCod, Function.identity()));
    }

    /** Se o mesmo aluno vier 2 vezes no corpo, vale o último. */
    private static <T> List<T> ultimoPorAluno(List<T> itens, Function<T, Long> aluno) {
        Map<Long, T> ultimo = new LinkedHashMap<>();
        itens.forEach(i -> ultimo.put(aluno.apply(i), i));
        return List.copyOf(ultimo.values());
    }

    /** Nome de 1 a 50, tipo de nota existente e uq_avaliacao (cod_turma, nome). */
    private TipoNota validarAvaliacao(Long turmaCod, DadosAvaliacaoRequest dados, Long ignorarCod) {
        RegrasDeNegocio.exigir(
                RegrasDeNegocio.validarNomeAvaliacao(dados.nome()),
                dados.codTipoNota() == null ? "Informe o tipo de nota (codTipoNota)." : null);
        TipoNota tipo = tiposNota.findById(dados.codTipoNota())
                .orElseThrow(() -> ApiException.requisicaoInvalida("Tipo de nota inválido."));
        String nome = dados.nome().trim();
        boolean repetido = ignorarCod == null
                ? avaliacoes.existsByTurmaCodAndNome(turmaCod, nome)
                : avaliacoes.existsByTurmaCodAndNomeAndCodNot(turmaCod, nome, ignorarCod);
        if (repetido) throw ApiException.conflito("Já existe uma avaliação com este nome nesta turma.");
        return tipo;
    }
}
