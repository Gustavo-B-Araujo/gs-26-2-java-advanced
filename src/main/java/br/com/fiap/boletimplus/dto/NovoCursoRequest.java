package br.com.fiap.boletimplus.dto;

/** POST /tipos/curso. Validado no serviço (1 a 30 caracteres, sem repetir). */
public record NovoCursoRequest(String enumerador) {
}
