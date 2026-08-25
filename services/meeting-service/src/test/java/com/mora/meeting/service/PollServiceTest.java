package com.mora.meeting.service;

import com.mora.meeting.dto.poll.PollRequestDTO;
import com.mora.meeting.dto.poll.PollResponseDTO;
import com.mora.meeting.dto.poll.VoteRequestDTO;
import com.mora.meeting.entity.Meeting;
import com.mora.meeting.entity.Poll;
import com.mora.meeting.entity.PollOption;
import com.mora.meeting.entity.PollVote;
import com.mora.meeting.enums.PollStatus;
import com.mora.meeting.mapper.PollMapper;
import com.mora.meeting.repository.MeetingRepository;
import com.mora.meeting.repository.PollRepository;
import com.mora.meeting.repository.PollVoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollServiceTest {

    @Mock
    private PollRepository pollRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private PollMapper pollMapper;

    @Mock
    private PollVoteRepository pollVoteRepository;

    @InjectMocks
    private PollService pollService;

    private PollRequestDTO pollRequestDTO;
    private Poll poll;
    private Meeting meeting;
    private PollResponseDTO pollResponseDTO;

    @BeforeEach
    void setUp() {
        pollRequestDTO = PollRequestDTO.builder()
                .titulo("Votação Teste")
                .descricao("Descrição da votação de teste")
                .meetingId(1L)
                .opcoes(List.of("Opção 1", "Opção 2"))
                .build();

        meeting = Meeting.builder()
                .id(1L)
                .titulo("Reunião Teste")
                .dataHoraInicio(LocalDateTime.now())
                .build();

        PollOption opt1 = PollOption.builder().id(1L).descricao("Opção 1").build();
        PollOption opt2 = PollOption.builder().id(2L).descricao("Opção 2").build();

        poll = Poll.builder()
                .id(1L)
                .titulo("Votação Teste")
                .descricao("Descrição da votação de teste")
                .status(PollStatus.ABERTA)
                .meeting(meeting)
                .opcoes(new ArrayList<>(List.of(opt1, opt2)))
                .build();

        opt1.setPoll(poll);
        opt2.setPoll(poll);

        pollResponseDTO = PollResponseDTO.builder()
                .id(1L)
                .titulo("Votação Teste")
                .descricao("Descrição da votação de teste")
                .status(PollStatus.ABERTA)
                .meetingId(1L)
                .build();
    }

    @Test
    void createPoll_Success() {
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(pollMapper.toEntity(any(PollRequestDTO.class))).thenReturn(poll);
        when(pollRepository.save(any(Poll.class))).thenReturn(poll);
        when(pollMapper.toResponseDto(any(Poll.class))).thenReturn(pollResponseDTO);

        PollResponseDTO result = pollService.createPoll(pollRequestDTO);

        assertNotNull(result);
        assertEquals("Votação Teste", result.getTitulo());
        verify(pollRepository).save(any(Poll.class));
    }

    @Test
    void createPoll_ThrowsExceptionWhenMeetingNotFound() {
        when(meetingRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            pollService.createPoll(pollRequestDTO);
        });

        assertTrue(exception.getMessage().contains("Reunião não encontrada"));
        verify(pollRepository, never()).save(any(Poll.class));
    }

    @Test
    void readPoll_Success() {
        when(pollRepository.findById(1L)).thenReturn(Optional.of(poll));
        when(pollMapper.toResponseDto(any(Poll.class))).thenReturn(pollResponseDTO);

        PollResponseDTO result = pollService.readPoll(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void closePoll_Success() {
        when(pollRepository.findById(1L)).thenReturn(Optional.of(poll));
        when(pollRepository.save(any(Poll.class))).thenReturn(poll);

        pollService.closePoll(1L);

        assertEquals(PollStatus.ENCERRADA, poll.getStatus());
        verify(pollRepository).save(poll);
    }

    @Test
    void registerVote_Success() {
        VoteRequestDTO voteDTO = VoteRequestDTO.builder()
                .pollOptionId(1L)
                .usuarioId(10L)
                .build();

        when(pollRepository.findById(1L)).thenReturn(Optional.of(poll));
        when(pollVoteRepository.existsByPollIdAndUsuarioId(1L, 10L)).thenReturn(false);

        pollService.registerVote(1L, voteDTO);

        verify(pollVoteRepository).save(any(PollVote.class));
    }

    @Test
    void registerVote_ThrowsExceptionWhenPollClosed() {
        poll.setStatus(PollStatus.ENCERRADA);
        VoteRequestDTO voteDTO = VoteRequestDTO.builder().pollOptionId(1L).usuarioId(10L).build();

        when(pollRepository.findById(1L)).thenReturn(Optional.of(poll));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            pollService.registerVote(1L, voteDTO);
        });

        assertEquals("Esta votação já foi encerrada.", exception.getMessage());
        verify(pollVoteRepository, never()).save(any(PollVote.class));
    }

    @Test
    void registerVote_ThrowsExceptionWhenUserAlreadyVoted() {
        VoteRequestDTO voteDTO = VoteRequestDTO.builder().pollOptionId(1L).usuarioId(10L).build();

        when(pollRepository.findById(1L)).thenReturn(Optional.of(poll));
        when(pollVoteRepository.existsByPollIdAndUsuarioId(1L, 10L)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            pollService.registerVote(1L, voteDTO);
        });

        assertEquals("Usuário já registrou um voto nesta votação.", exception.getMessage());
        verify(pollVoteRepository, never()).save(any(PollVote.class));
    }
}
