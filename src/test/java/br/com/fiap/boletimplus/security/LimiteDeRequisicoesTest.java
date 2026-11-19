package br.com.fiap.boletimplus.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimiteDeRequisicoesTest {

    @Test
    void passouDoLimiteDevolveOsSegundosAteAJanelaVirar() {
        var janela = new LimiteDeRequisicoesFilter.Janela(2, Duration.ofSeconds(60));
        Instant inicio = Instant.parse("2026-11-19T12:00:00Z");

        assertTrue(janela.tentar("usuario:1", inicio).isEmpty());
        assertTrue(janela.tentar("usuario:1", inicio.plusSeconds(1)).isEmpty());
        assertEquals(OptionalLong.of(50), janela.tentar("usuario:1", inicio.plusSeconds(10)));
        assertTrue(janela.tentar("usuario:2", inicio.plusSeconds(10)).isEmpty(), "outro cliente tem a própria conta");
        assertTrue(janela.tentar("usuario:1", inicio.plusSeconds(60)).isEmpty(), "janela nova zera a conta");
    }
}
