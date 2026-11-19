package br.com.fiap.boletimplus.service;

import br.com.fiap.boletimplus.exception.ApiException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Collator;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Regras de gestão escolar e sala de aula, sem banco: os limites vêm das constraints de 01_ddl.sql
 * e as mensagens são as mesmas da API .NET. Cada `validar*` devolve a mensagem do erro, ou null
 * quando o valor é válido; `exigir` lança 400 com a primeira mensagem.
 * A validação fica no serviço (e não em Bean Validation) para manter a ordem da API .NET:
 * primeiro 403/404 do recurso, depois 400 do corpo.
 */
public final class RegrasDeNegocio {

    public static final int NOME_PESSOA_MAX = 150;
    public static final int NOME_AVALIACAO_MAX = 50;
    public static final int TIPO_CURSO_MAX = 30;
    public static final int OBSERVACAO_MAX = 150;
    public static final int SENHA_MIN = 8;
    public static final int ANO_MIN = 2000;
    public static final BigDecimal NOTA_MIN = BigDecimal.ZERO;
    public static final BigDecimal NOTA_MAX = BigDecimal.valueOf(100);
    public static final LocalDate DATA_PRESENCA_MIN = LocalDate.of(2000, 1, 1);
    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    /** Ordem alfabética do português, sem diferenciar maiúscula. */
    public static final Comparator<String> ORDEM_NOME = ordemNome();

    private static final Pattern LETRA = Pattern.compile("[A-Za-z]");
    private static final Pattern DIGITO = Pattern.compile("\\d");

    private RegrasDeNegocio() {
    }

    /** Lança 400 com a primeira mensagem de erro não nula. */
    public static void exigir(String... erros) {
        Arrays.stream(erros).filter(Objects::nonNull).findFirst().ifPresent(erro -> {
            throw ApiException.requisicaoInvalida(erro);
        });
    }

    public static String validarNome(String nome) {
        String limpo = nome == null ? "" : nome.trim();
        if (limpo.isEmpty() || limpo.split("\\s+").length < 2) return "Informe nome e sobrenome.";
        if (limpo.length() > NOME_PESSOA_MAX) return "Máximo de " + NOME_PESSOA_MAX + " caracteres.";
        return null;
    }

    /** 11 dígitos, sem todos iguais, com os 2 dígitos verificadores certos. */
    public static String validarCpf(String cpf) {
        String d = cpf == null ? "" : cpf;
        if (!d.matches("[0-9]{11}")) return "O CPF tem 11 dígitos, sem máscara.";
        if (d.chars().distinct().count() == 1) return "CPF inválido.";
        if (digito(d, 9) != d.charAt(9) - '0' || digito(d, 10) != d.charAt(10) - '0') return "CPF inválido.";
        return null;
    }

    public static String validarSenha(String senha) {
        if (senha == null || senha.length() < SENHA_MIN) return "Mínimo de " + SENHA_MIN + " caracteres.";
        if (!LETRA.matcher(senha).find() || !DIGITO.matcher(senha).find()) return "Use letras e números.";
        return null;
    }

    public static String validarNomeAvaliacao(String nome) {
        String limpo = nome == null ? "" : nome.trim();
        return limpo.isEmpty() || limpo.length() > NOME_AVALIACAO_MAX
                ? "O nome precisa ter de 1 a " + NOME_AVALIACAO_MAX + " caracteres."
                : null;
    }

    public static String validarTipoCurso(String enumerador) {
        String limpo = enumerador == null ? "" : enumerador.trim();
        return limpo.isEmpty() || limpo.length() > TIPO_CURSO_MAX
                ? "O nome precisa ter de 1 a " + TIPO_CURSO_MAX + " caracteres."
                : null;
    }

    /** CHECK (semestre_referencia IN (1, 2)). */
    public static String validarSemestre(Integer semestre) {
        return semestre != null && (semestre == 1 || semestre == 2) ? null : "Semestre inválido: use 1 ou 2.";
    }

    /** CHECK (ano_referencia >= 2000). */
    public static String validarAno(Integer ano) {
        return ano != null && ano >= ANO_MIN ? null : "O ano precisa ser " + ANO_MIN + " ou depois.";
    }

    /** CHECK (nota BETWEEN 0 AND 100). Nota null é válida: apaga a nota lançada. */
    public static String validarNota(BigDecimal nota) {
        return nota == null || (nota.compareTo(NOTA_MIN) >= 0 && nota.compareTo(NOTA_MAX) <= 0)
                ? null
                : "A nota precisa estar entre " + NOTA_MIN + " e " + NOTA_MAX + ".";
    }

    public static String validarObservacao(String observacao) {
        return observacao != null && observacao.length() > OBSERVACAO_MAX
                ? "Observação com mais de " + OBSERVACAO_MAX + " caracteres."
                : null;
    }

    /** CHECK da data mínima e sem data futura. */
    public static String validarDataPresenca(LocalDate data, LocalDate hoje) {
        if (data.isBefore(DATA_PRESENCA_MIN)) return "Data inválida.";
        if (data.isAfter(hoje)) return "Não é permitido registrar presença em data futura.";
        return null;
    }

    /** Parâmetro ?data= do contrato. Formato diferente de YYYY-MM-DD responde 400. */
    public static LocalDate lerData(String texto) {
        try {
            if (texto != null && texto.length() == 10) return LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            // cai no 400 abaixo
        }
        throw ApiException.requisicaoInvalida("Data inválida: use o formato YYYY-MM-DD.");
    }

    /** NUMBER(5,2): guarda a nota com 2 casas. */
    public static BigDecimal arredondarNota(BigDecimal nota) {
        return nota.setScale(2, RoundingMode.HALF_UP);
    }

    public static LocalDate hoje() {
        return LocalDate.now(FUSO);
    }

    private static int digito(String d, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) soma += (d.charAt(i) - '0') * (tamanho + 1 - i);
        int resto = soma * 10 % 11;
        return resto == 10 ? 0 : resto;
    }

    private static Comparator<String> ordemNome() {
        Collator collator = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        collator.setStrength(Collator.SECONDARY);
        return collator::compare;
    }
}
