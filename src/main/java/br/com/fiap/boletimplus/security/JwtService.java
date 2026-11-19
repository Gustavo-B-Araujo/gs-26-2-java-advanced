package br.com.fiap.boletimplus.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Emite e valida o JWT (HS256). O .NET valida o mesmo token com o mesmo `JWT_SECRET`.
 * Claims: `sub` = Usuario.cod, `iss`, `iat`, `exp`.
 */
@Service
public class JwtService {

    private final Algorithm algoritmo;
    private final JWTVerifier verificador;
    private final String emissor;
    private final Duration validade;

    public JwtService(
            @Value("${boletim.jwt.secret}") String segredo,
            @Value("${boletim.jwt.issuer}") String emissor,
            @Value("${boletim.jwt.validade}") Duration validade) {
        if (segredo.getBytes().length < 32) {
            throw new IllegalStateException("JWT_SECRET precisa de pelo menos 32 caracteres.");
        }
        this.algoritmo = Algorithm.HMAC256(segredo);
        this.emissor = emissor;
        this.validade = validade;
        this.verificador = JWT.require(algoritmo).withIssuer(emissor).build();
    }

    public String gerar(Long usuarioCod) {
        Instant agora = Instant.now();
        return JWT.create()
                .withIssuer(emissor)
                .withSubject(usuarioCod.toString())
                .withIssuedAt(agora)
                .withExpiresAt(agora.plus(validade))
                .sign(algoritmo);
    }

    /** Devolve o `sub` (Usuario.cod) se o token for válido; vazio se a assinatura, o emissor ou a validade falharem. */
    public Optional<Long> validar(String token) {
        try {
            return Optional.of(Long.valueOf(verificador.verify(token).getSubject()));
        } catch (JWTVerificationException | NumberFormatException e) {
            return Optional.empty();
        }
    }
}
