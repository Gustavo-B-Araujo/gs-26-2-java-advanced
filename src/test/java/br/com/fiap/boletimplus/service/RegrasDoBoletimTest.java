package br.com.fiap.boletimplus.service;

import br.com.fiap.boletimplus.dto.BoletimAlunoResponse;
import br.com.fiap.boletimplus.dto.BoletimTurmaResponse;
import br.com.fiap.boletimplus.dto.TurmaResumoResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RegrasDoBoletimTest {

    @Test
    void mediaIgnoraListaVazia() {
        assertThat(BoletimService.media(List.of())).isNull();
        assertThat(BoletimService.media(List.of(45.0, 60.0))).isEqualTo(52.5);
    }

    @Test
    void soPresenteContaComoPresenca() {
        assertThat(BoletimService.frequencia(List.of())).isNull();
        assertThat(BoletimService.frequencia(List.of("Presente", "Falta", "Falta Justificada"))).isEqualTo(1.0 / 3);
    }

    @Test
    void chaveDoEnumeradorIgualAoApp() {
        assertThat(BoletimService.chave(" Falta  Justificada ")).isEqualTo("FALTA_JUSTIFICADA");
        assertThat(BoletimService.chave("Mãe")).isEqualTo("MAE");
    }

    @Test
    void alertaSoParaMateriaEmAtencao() {
        var boletim = new BoletimAlunoResponse(1L, "Bart Simpson", List.of(
                turma("Português", 52.5, 1.0 / 3),
                turma("História", 100.0, 1.0),
                turma("Matemática", null, null)));

        var alertas = AnaliseBoletimService.alertasDe(boletim);

        assertThat(alertas).hasSize(1);
        assertThat(alertas.getFirst().materia()).isEqualTo("Português");
        assertThat(alertas.getFirst().motivo()).isEqualTo("média 52,5 e frequência 33%");
    }

    @Test
    void promptNaoLevaOCpfNemOSobrenome() {
        var boletim = new BoletimAlunoResponse(1L, "Bart Simpson", List.of(turma("Português", 52.5, 0.5)));

        String texto = AnaliseBoletimService.descrever(boletim);

        assertThat(texto).contains("Aluno: Bart", "- Português: média 52,5 (abaixo de 60); frequência 50% (abaixo de 75%)")
                .doesNotContain("Simpson");
    }

    private static BoletimTurmaResponse turma(String materia, Double media, Double frequencia) {
        var resumo = new TurmaResumoResponse(1L, 1L, "Escola", materia, 1L, "Edna Krabappel", 2, 2026, 2);
        return new BoletimTurmaResponse(resumo, List.of(), media, List.of(), frequencia);
    }
}
