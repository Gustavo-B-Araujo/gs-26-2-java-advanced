package br.com.fiap.boletimplus.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-bytes!!";

    @Test
    void tokenGeradoValidaComOMesmoSegredo() {
        var jwt = new JwtService(SEGREDO, "boletim-plus", Duration.ofHours(1));

        assertThat(jwt.validar(jwt.gerar(42L))).contains(42L);
    }

    @Test
    void tokenDeOutroSegredoOuVencidoNaoValida() {
        var emissor = new JwtService(SEGREDO, "boletim-plus", Duration.ofHours(1));
        var outro = new JwtService(SEGREDO + "x", "boletim-plus", Duration.ofHours(1));
        var vencido = new JwtService(SEGREDO, "boletim-plus", Duration.ofSeconds(-120));

        assertThat(outro.validar(emissor.gerar(1L))).isEmpty();
        assertThat(emissor.validar(vencido.gerar(1L))).isEmpty();
        assertThat(emissor.validar("lixo")).isEmpty();
    }

    @Test
    void segredoCurtoNaoSobe() {
        assertThatThrownBy(() -> new JwtService("curto", "boletim-plus", Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class);
    }
}
