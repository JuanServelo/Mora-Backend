package com.mora.portaria.service;

import com.mora.portaria.entity.Bloco;
import com.mora.portaria.exception.OperacaoInvalidaException;
import com.mora.portaria.exception.RecursoNaoEncontradoException;
import com.mora.portaria.repository.BlocoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlocoServiceTest {

    @Mock
    private BlocoRepository blocoRepository;

    @InjectMocks
    private BlocoService blocoService;

    private Bloco bloco;
    private UUID blocoId;

    @BeforeEach
    void setUp() {
        blocoId = UUID.randomUUID();
        bloco = new Bloco();
        bloco.setId(blocoId);
        bloco.setNome("Bloco A");
        bloco.setCondominioId("cond-123");
        bloco.setAtivo(true);
        bloco.setAndares(10);
        bloco.setApartamentosPorAndar(4);
    }

    @Test
    void cadastrar_Success() {
        when(blocoRepository.findByNomeAndCondominioId("Bloco A", "cond-123")).thenReturn(Optional.empty());
        when(blocoRepository.save(any(Bloco.class))).thenReturn(bloco);

        Bloco result = blocoService.cadastrar(bloco);

        assertNotNull(result);
        assertEquals("Bloco A", result.getNome());
        verify(blocoRepository).save(any(Bloco.class));
    }

    @Test
    void cadastrar_ThrowsExceptionWhenNameExistsInCondominio() {
        when(blocoRepository.findByNomeAndCondominioId("Bloco A", "cond-123")).thenReturn(Optional.of(bloco));

        OperacaoInvalidaException exception = assertThrows(OperacaoInvalidaException.class, () -> {
            blocoService.cadastrar(bloco);
        });

        assertTrue(exception.getMessage().contains("Já existe um bloco com o nome"));
        verify(blocoRepository, never()).save(any(Bloco.class));
    }

    @Test
    void listarTodos_Success() {
        when(blocoRepository.findByCondominioId("cond-123")).thenReturn(List.of(bloco));

        List<Bloco> result = blocoService.listarTodos("cond-123");

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void buscarPorId_Success() {
        when(blocoRepository.findById(blocoId)).thenReturn(Optional.of(bloco));

        Bloco result = blocoService.buscarPorId(blocoId);

        assertNotNull(result);
        assertEquals(blocoId, result.getId());
    }

    @Test
    void buscarPorId_ThrowsExceptionWhenNotFound() {
        when(blocoRepository.findById(blocoId)).thenReturn(Optional.empty());

        RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class, () -> {
            blocoService.buscarPorId(blocoId);
        });

        assertTrue(exception.getMessage().contains("Bloco não encontrado"));
    }

    @Test
    void atualizar_Success() {
        Bloco novosDados = new Bloco();
        novosDados.setNome("Bloco B");
        novosDados.setAndares(12);
        novosDados.setApartamentosPorAndar(4);

        when(blocoRepository.findById(blocoId)).thenReturn(Optional.of(bloco));
        when(blocoRepository.findByNomeAndCondominioId("Bloco B", "cond-123")).thenReturn(Optional.empty());
        when(blocoRepository.save(any(Bloco.class))).thenReturn(bloco);

        Bloco result = blocoService.atualizar(blocoId, novosDados);

        assertEquals("Bloco B", bloco.getNome());
        assertEquals(12, bloco.getAndares());
        verify(blocoRepository).save(bloco);
    }

    @Test
    void desativar_Success() {
        when(blocoRepository.findById(blocoId)).thenReturn(Optional.of(bloco));
        when(blocoRepository.save(any(Bloco.class))).thenReturn(bloco);

        blocoService.desativar(blocoId);

        assertFalse(bloco.isAtivo());
        verify(blocoRepository).save(bloco);
    }
}
