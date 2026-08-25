package com.mora.vagas.service;

import com.mora.vagas.dto.VagaRequestDTO;
import com.mora.vagas.entity.Apartamento;
import com.mora.vagas.entity.Vaga;
import com.mora.vagas.enums.TipoVaga;
import com.mora.vagas.exception.OperacaoInvalidaException;
import com.mora.vagas.exception.RecursoNaoEncontradoException;
import com.mora.vagas.repository.ApartamentoRepository;
import com.mora.vagas.repository.VagaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VagaServiceTest {

    @Mock
    private VagaRepository vagaRepository;

    @Mock
    private ApartamentoRepository apartamentoRepository;

    @InjectMocks
    private VagaService vagaService;

    private VagaRequestDTO vagaRequestDTO;
    private Vaga vaga;
    private Apartamento apartamento;
    private UUID apartamentoId;

    @BeforeEach
    void setUp() {
        apartamentoId = UUID.randomUUID();
        
        apartamento = new Apartamento();
        apartamento.setId(apartamentoId);

        vagaRequestDTO = new VagaRequestDTO(
                "A1",
                "SS1",
                TipoVaga.COBERTA,
                apartamentoId
        );

        vaga = new Vaga();
        vaga.setId("vaga-123");
        vaga.setNumero("A1");
        vaga.setLocalizacao("SS1");
        vaga.setTipo(TipoVaga.COBERTA);
        vaga.setApartamento(apartamento);
        vaga.setAtiva(true);
    }

    @Test
    void cadastrar_Success() {
        when(vagaRepository.findByNumero("A1")).thenReturn(Optional.empty());
        when(apartamentoRepository.findById(apartamentoId)).thenReturn(Optional.of(apartamento));
        when(vagaRepository.save(any(Vaga.class))).thenReturn(vaga);

        Vaga result = vagaService.cadastrar(vagaRequestDTO);

        assertNotNull(result);
        assertEquals("A1", result.getNumero());
        verify(vagaRepository).save(any(Vaga.class));
    }

    @Test
    void cadastrar_ThrowsExceptionWhenNumberExists() {
        when(vagaRepository.findByNumero("A1")).thenReturn(Optional.of(vaga));

        OperacaoInvalidaException exception = assertThrows(OperacaoInvalidaException.class, () -> {
            vagaService.cadastrar(vagaRequestDTO);
        });

        assertTrue(exception.getMessage().contains("Já existe uma vaga cadastrada com o número"));
        verify(vagaRepository, never()).save(any(Vaga.class));
    }

    @Test
    void cadastrar_ThrowsExceptionWhenApartamentoNotFound() {
        when(vagaRepository.findByNumero("A1")).thenReturn(Optional.empty());
        when(apartamentoRepository.findById(apartamentoId)).thenReturn(Optional.empty());

        RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class, () -> {
            vagaService.cadastrar(vagaRequestDTO);
        });

        assertTrue(exception.getMessage().contains("Apartamento não encontrado"));
        verify(vagaRepository, never()).save(any(Vaga.class));
    }

    @Test
    void buscarPorId_Success() {
        when(vagaRepository.findById("vaga-123")).thenReturn(Optional.of(vaga));

        Vaga result = vagaService.buscarPorId("vaga-123");

        assertNotNull(result);
        assertEquals("vaga-123", result.getId());
    }

    @Test
    void desativar_Success() {
        when(vagaRepository.findById("vaga-123")).thenReturn(Optional.of(vaga));
        when(vagaRepository.save(any(Vaga.class))).thenReturn(vaga);

        vagaService.desativar("vaga-123");

        assertFalse(vaga.isAtiva());
        verify(vagaRepository).save(vaga);
    }
}
