package comunicacao.dto;

import comunicacao.model.enums.TipoNotificacao;

import java.util.List;

/**
 * Categorias que o usuário NÃO quer receber.
 *
 * A lista é o estado completo, não um incremento: o que não vier nela volta a
 * ser recebido. Assim a tela envia o que está marcado e pronto.
 */
public record PreferenciasRequest(List<TipoNotificacao> silenciadas) {}
