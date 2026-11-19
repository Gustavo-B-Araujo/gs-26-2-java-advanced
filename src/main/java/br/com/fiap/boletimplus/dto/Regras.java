package br.com.fiap.boletimplus.dto;

/** Expressões de validação usadas nos DTOs. Mesmas regras de `src/lib/validacao.ts` do app. */
public final class Regras {

    public static final String CPF = "^\\d{11}$";
    public static final String NOME_E_SOBRENOME = "^\\s*\\S+(\\s+\\S+)+\\s*$";
    public static final String LETRAS_E_NUMEROS = "^(?=.*[A-Za-z])(?=.*\\d).+$";

    public static final String MSG_CPF = "O CPF tem 11 dígitos, sem máscara.";
    public static final String MSG_NOME = "Informe nome e sobrenome.";
    public static final String MSG_NOME_TAMANHO = "Máximo de 150 caracteres.";
    public static final String MSG_SENHA_TAMANHO = "Mínimo de 8 caracteres.";
    public static final String MSG_SENHA_REGRA = "Use letras e números.";

    private Regras() {
    }
}
