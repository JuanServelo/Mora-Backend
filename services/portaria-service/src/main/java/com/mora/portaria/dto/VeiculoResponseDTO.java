package com.mora.portaria.dto;

import lombok.Builder;
import lombok.Data;
import com.mora.portaria.entity.Veiculo;
import com.mora.portaria.enums.CategoriaVeiculo;
import com.mora.portaria.enums.StatusAcesso;
import com.mora.portaria.enums.TipoProprietario;
import java.time.LocalDateTime;

@Data
@Builder
public class VeiculoResponseDTO {

    private String id;
    private String placa;
    private String modelo;
    private CategoriaVeiculo categoria;
    private String proprietarioId;
    private TipoProprietario tipoProprietario;
    private String vagaId;
    private String vagaNumero;
    private String vagaLocalizacao;
    private String apartamentoId;
    private String apartamentoNumero;
    private StatusAcesso status;
    private LocalDateTime dataEntrada;
    private LocalDateTime dataSaida;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public static VeiculoResponseDTO fromEntity(Veiculo v) {
        VeiculoResponseDTOBuilder b = VeiculoResponseDTO.builder()
                .id(v.getId())
                .placa(v.getPlaca())
                .modelo(v.getModelo())
                .categoria(v.getCategoria())
                .proprietarioId(v.getProprietarioId())
                .tipoProprietario(v.getTipoProprietario())
                .status(v.getStatus())
                .dataEntrada(v.getDataEntrada())
                .dataSaida(v.getDataSaida())
                .criadoEm(v.getCriadoEm())
                .atualizadoEm(v.getAtualizadoEm());

        if (v.getVaga() != null) {
            b.vagaId(v.getVaga().getId())
             .vagaNumero(v.getVaga().getNumero())
             .vagaLocalizacao(v.getVaga().getLocalizacao());

            if (v.getVaga().getApartamento() != null) {
                b.apartamentoId(v.getVaga().getApartamento().getId().toString())
                 .apartamentoNumero(v.getVaga().getApartamento().getNumero());
            }
        }

        return b.build();
    }
}

