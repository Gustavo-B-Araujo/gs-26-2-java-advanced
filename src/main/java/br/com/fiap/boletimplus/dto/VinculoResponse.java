package br.com.fiap.boletimplus.dto;

import br.com.fiap.boletimplus.model.AlunoGuardiao;

public record VinculoResponse(
        Long cod,
        Long guardiaoCod,
        String guardiaoNome,
        Long alunoCod,
        String alunoNome,
        Long codTipoParentesco,
        String parentesco) {

    public static VinculoResponse de(AlunoGuardiao v) {
        return new VinculoResponse(
                v.getCod(),
                v.getGuardiao().getCod(),
                v.getGuardiao().getPessoa().getNome(),
                v.getAluno().getCod(),
                v.getAluno().getPessoa().getNome(),
                v.getTipoParentesco().getCod(),
                v.getTipoParentesco().getEnumerador());
    }
}
