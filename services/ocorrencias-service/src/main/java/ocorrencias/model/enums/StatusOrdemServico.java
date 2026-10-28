package ocorrencias.model.enums;

/**
 * `ATRASADA` não é escolhida por ninguém: é o job diário que marca a OS cujo
 * prazo venceu sem conclusão. Uma OS atrasada ainda pode receber andamento e
 * ser concluída — fica fora do indicador de OS no prazo.
 */
public enum StatusOrdemServico {
    ABERTA,
    EM_EXECUCAO,
    ATRASADA,
    CONCLUIDA;

    public boolean emAberto() {
        return this != CONCLUIDA;
    }
}
