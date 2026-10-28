package ocorrencias.model.enums;

/**
 * Categorias fixas — as mesmas que a tela do morador já oferecia quando a
 * reclamação vivia no auth-api. A coluna guarda o nome como texto, então
 * acrescentar uma categoria aqui não exige mudança de esquema.
 */
public enum CategoriaReclamacao {
    BARULHO,
    INFRAESTRUTURA,
    LIMPEZA,
    GARAGEM,
    ANIMAIS,
    ELEVADORES,
    ILUMINACAO,
    ELETRICA,
    HIDRAULICA,
    PINTURA,
    JARDINAGEM,
    SUGESTAO_MELHORIA,
    OUTROS;

    public static boolean valida(String valor) {
        if (valor == null) return false;
        for (CategoriaReclamacao c : values()) {
            if (c.name().equals(valor)) return true;
        }
        return false;
    }
}
