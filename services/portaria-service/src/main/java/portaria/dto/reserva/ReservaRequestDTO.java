package portaria.dto.reserva;

import portaria.model.enums.TipoReserva;

import java.time.LocalDateTime;

/**
 * `unidadeId` só é considerado quando quem cria é porteiro ou síndico.
 * Vindo de um morador é ignorado: o backend usa o vínculo do autenticado
 * (RN-05), porque campo somente leitura no frontend não é garantia.
 */
public record ReservaRequestDTO(
        String areaComumId,
        LocalDateTime inicio,
        LocalDateTime fim,
        TipoReserva tipoReserva,
        String unidadeId,
        String responsavelId,
        String descricaoEvento,
        String observacoes
) {}
