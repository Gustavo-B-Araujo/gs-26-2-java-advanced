package br.com.fiap.boletimplus.dto;

import java.util.List;

/** PUT /turmas/{cod}/chamada?data=YYYY-MM-DD. */
public record ChamadaRequest(List<Item> itens) {

    public record Item(Long turmaAlunoCod, Long codTipoPresenca, String observacao) {
    }
}
