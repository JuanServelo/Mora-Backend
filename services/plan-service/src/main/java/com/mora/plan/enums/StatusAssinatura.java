package com.mora.plan.enums;

/** Situação da assinatura de um condomínio. */
public enum StatusAssinatura {
    /** Vigente: o condomínio opera normalmente. */
    ATIVA,
    /** Inadimplente após a tolerância: leitura permitida, escrita bloqueada. */
    SUSPENSA,
    /** Encerrada a pedido ou por inadimplência prolongada. */
    CANCELADA
}
