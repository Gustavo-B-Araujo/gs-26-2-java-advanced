package br.com.fiap.boletimplus.dto;

import java.math.BigDecimal;
import java.util.List;

/** PUT /avaliacoes/{cod}/notas. Nota null apaga a nota lançada. */
public record NotasRequest(List<Item> notas) {

    public record Item(Long turmaAlunoCod, BigDecimal nota) {
    }
}
