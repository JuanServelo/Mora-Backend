package com.mora.plan.enums;

/**
 * Módulos disponíveis na plataforma Mora.
 * Cada slug representa uma funcionalidade que pode ser habilitada ou desabilitada por plano.
 */
public enum PlanModule {

    PORTARIA("portaria"),
    REUNIOES("reunioes"),
    VAGAS("vagas"),
    ENTREGAS("entregas"),
    AREAS_COMUNS("areas_comuns"),
    RECLAMACOES("reclamacoes"),
    CONHECIMENTO("conhecimento"),
    VOTACOES("votacoes"),
    VEICULOS("veiculos"),
    CHAVES("chaves");

    private final String slug;

    PlanModule(String slug) {
        this.slug = slug;
    }

    public String getSlug() {
        return slug;
    }

    /**
     * Busca um PlanModule pelo slug. Lança exceção se inválido.
     */
    public static PlanModule fromSlug(String slug) {
        for (PlanModule module : values()) {
            if (module.slug.equalsIgnoreCase(slug)) {
                return module;
            }
        }
        throw new IllegalArgumentException("Módulo inválido: " + slug);
    }

    /**
     * Verifica se um slug é válido.
     */
    public static boolean isValid(String slug) {
        for (PlanModule module : values()) {
            if (module.slug.equalsIgnoreCase(slug)) {
                return true;
            }
        }
        return false;
    }
}
