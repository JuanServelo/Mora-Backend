package com.mora.meeting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mora.meeting.entity.*;
import com.mora.meeting.enums.*;
import com.mora.meeting.repository.*;
import com.mora.meeting.config.LegacyVoteMigration;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MeetingSecurityIntegrationTest {
    static final String SECRET = "meeting-tests-secret-with-more-than-32-characters";
    static final ObjectMapper JSON = new ObjectMapper();
    static final AtomicInteger PLAN_STATUS = new AtomicInteger(204);
    static HttpServer upstream;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) throws Exception {
        upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        upstream.createContext("/api/plans/access/", ex -> {
            ex.sendResponseHeaders(PLAN_STATUS.get(), -1);
            ex.close();
        });
        upstream.createContext("/api/auth/me", ex -> {
            String raw = ex.getRequestHeaders().getFirst("Authorization").substring(7);
            var claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                    .build().parseSignedClaims(raw).getPayload();
            long id = Long.parseLong(claims.get("id").toString());
            if (id == 99) { ex.sendResponseHeaders(503, -1); ex.close(); return; }
            Map<String, Object> user = new HashMap<>();
            user.put("id", id);
            user.put("perfil", claims.get("perfil"));
            user.put("condominioId", id == 7 ? "tenant-b" : claims.get("condominioId"));
            user.put("status", "active");
            user.put("unidadeId", id == 5 ? null : (id == 4 ? "unit-102" : "unit-101"));
            byte[] bytes = JSON.writeValueAsBytes(Map.of("usuario", user));
            ex.getResponseHeaders().set("Content-Type", "application/json");
            ex.sendResponseHeaders(200, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.close();
        });
        upstream.createContext("/api/user-management/users", ex -> {
            byte[] bytes = JSON.writeValueAsBytes(Map.of("usuarios", List.of(
                    Map.of("id", 2, "condominioId", "tenant-a", "status", "active"),
                    Map.of("id", 3, "condominioId", "tenant-a", "status", "active"),
                    Map.of("id", 9, "condominioId", "tenant-b", "status", "active"))));
            ex.sendResponseHeaders(200, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.close();
        });
        upstream.start();
        String url = "http://127.0.0.1:" + upstream.getAddress().getPort();
        registry.add("PLAN_SERVICE_URL", () -> url);
        registry.add("AUTH_API_URL", () -> url);
    }
    @AfterAll static void stop() { if (upstream != null) upstream.stop(0); }
    @Autowired MockMvc mvc;
    @Autowired MeetingRepository meetings;
    @Autowired PollRepository polls;
    @Autowired PollVoteRepository votes;
    @Autowired AtaRepository atas;
    @Autowired LegacyVoteMigration migration;
    @org.springframework.boot.test.mock.mockito.SpyBean
    com.mora.meeting.service.GoogleMeetOAuthService google;
    Poll pollA, pollB;
    Meeting meetingA, meetingB;

    @BeforeEach void setup() {
        PLAN_STATUS.set(204);
        atas.deleteAll();
        votes.deleteAll();
        polls.deleteAll();
        meetings.deleteAll();
        pollA = ballot("tenant-a");
        pollB = ballot("tenant-b");
        meetingA = meeting("tenant-a");
        meetingB = meeting("tenant-b");
    }
    Poll ballot(String tenant) {
        Poll poll = Poll.builder().titulo("Votação " + tenant).condominioId(tenant).status(PollStatus.ABERTA)
                .dataHoraFim(LocalDateTime.now().plusHours(1)).build();
        poll.addOption("Sim"); poll.addOption("Não");
        return polls.saveAndFlush(poll);
    }
    Meeting meeting(String tenant) {
        return meetings.saveAndFlush(Meeting.builder().titulo("Reunião " + tenant).condominioId(tenant)
                .idOrganizador(1L).status(MeetingStatus.AGENDADA).dataHoraInicio(LocalDateTime.now().plusDays(1))
                .dataHoraFim(LocalDateTime.now().plusDays(1).plusHours(1))
                .convidados(new ArrayList<>(List.of(new MeetingGuests(2L, AttendanceStatus.PENDENTE),
                        new MeetingGuests(3L, AttendanceStatus.PENDENTE)))).build());
    }
    String token(long id, String profile, String tenant) {
        return Jwts.builder().claim("id", id).claim("perfil", profile).claim("condominioId", tenant)
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }
    ResultActions as(MockHttpServletRequestBuilder request, long id, String profile) throws Exception {
        return mvc.perform(request.header("Authorization", "Bearer " + token(id, profile, "tenant-a")));
    }
    String voteBody(Poll poll) { return "{\"pollOptionId\":" + poll.getOpcoes().get(0).getId() + "}"; }

    @Test void unauthenticatedMalformedExpiredAndForgedTokensAreRejectedBeforeBusinessActions() throws Exception {
        for (var request : List.of(get("/api/meetings"), post("/api/meetings"), get("/api/polls"),
                post("/api/polls/1/votes"), post("/api/meetings/1/ata"))) {
            mvc.perform(request).andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/meetings").header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized());
        String forged = Jwts.builder().claim("id", 1).claim("perfil", "ADMIN_SINDICO").claim("condominioId", "tenant-a")
                .signWith(Keys.hmacShaKeyFor("different-secret-with-more-than-32-characters".getBytes(StandardCharsets.UTF_8))).compact();
        mvc.perform(get("/api/meetings").header("Authorization", "Bearer " + forged)).andExpect(status().isUnauthorized());
        String expired = Jwts.builder().claim("id", 1).expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        mvc.perform(get("/api/meetings").header("Authorization", "Bearer " + expired)).andExpect(status().isUnauthorized());
    }

    @Test void managementRequiresManagerProfileAndGuestAndPorterCannotReadOrVote() throws Exception {
        for (String profile : List.of("MORADOR", "DONO_ALUGUEL", "PORTEIRO", "CONVIDADO")) {
            for (var req : List.of(post("/api/meetings"), put("/api/meetings/" + meetingA.getId()),
                    patch("/api/meetings/" + meetingA.getId() + "/cancel"), patch("/api/meetings/" + meetingA.getId() + "/finalizar"),
                    post("/api/meetings/" + meetingA.getId() + "/ata"), put("/api/meetings/" + meetingA.getId() + "/ata"),
                    post("/api/polls"), put("/api/polls/" + pollA.getId()), patch("/api/polls/" + pollA.getId() + "/close"))) {
                as(req, 2, profile).andExpect(status().isForbidden());
            }
        }
        for (String profile : List.of("PORTEIRO", "CONVIDADO")) {
            as(get("/api/meetings"), 2, profile).andExpect(status().isForbidden());
            as(post("/api/polls/" + pollA.getId() + "/votes").contentType("application/json").content(voteBody(pollA)), 2, profile)
                    .andExpect(status().isForbidden());
        }
    }

    @Test void listsIgnoreForeignTenantAndUserParametersAndIdsAreTenantScoped() throws Exception {
        as(get("/api/meetings?usuarioId=999&condominioId=tenant-b"), 2, "MORADOR")
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(meetingA.getId()));
        as(get("/api/polls?condominioId=tenant-b"), 2, "MORADOR")
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(pollA.getId()));
        for (String profile : List.of("MORADOR", "ADMIN_SINDICO")) {
            as(get("/api/meetings/" + meetingB.getId()), 2, profile).andExpect(status().isNotFound());
            as(get("/api/polls/" + pollB.getId()), 2, profile).andExpect(status().isNotFound());
            as(get("/api/meetings/" + meetingB.getId() + "/ata"), 2, profile).andExpect(status().isNotFound());
        }
        as(patch("/api/meetings/" + meetingB.getId() + "/cancel"), 1, "ADMIN_SINDICO").andExpect(status().isNotFound());
        as(patch("/api/polls/" + pollB.getId() + "/close"), 1, "ADMIN_SINDICO").andExpect(status().isNotFound());
        as(get("/api/meetings/" + meetingA.getId()), 4, "MORADOR").andExpect(status().isNotFound());
    }

    @Test void voteIdentityAndUnitComeFromSessionAndSecondResidentOfSameUnitCannotVote() throws Exception {
        String forgedBody = voteBody(pollA).replace("}", ",\"usuarioId\":999,\"unidadeId\":\"fake\"}");
        as(post("/api/polls/" + pollA.getId() + "/votes").contentType("application/json").content(forgedBody), 2, "MORADOR")
                .andExpect(status().isCreated());
        PollVote vote = votes.findAll().get(0);
        assertThat(vote.getUsuarioId()).isEqualTo(2);
        assertThat(vote.getUnidadeId()).isEqualTo("unit-101");
        as(post("/api/polls/" + pollA.getId() + "/votes").contentType("application/json").content(voteBody(pollA)), 3, "DONO_ALUGUEL")
                .andExpect(status().isConflict());
        as(get("/api/polls/" + pollA.getId()), 3, "MORADOR").andExpect(status().isOk())
                .andExpect(jsonPath("$.minhaUnidadeVotou").value(true)).andExpect(jsonPath("$.podeVotar").value(false));
        as(post("/api/polls/" + pollA.getId() + "/votes").contentType("application/json").content(voteBody(pollA)), 4, "MORADOR")
                .andExpect(status().isCreated());
        assertThat(votes.count()).isEqualTo(2);
    }

    @Test void residentListsOnlyUpcomingOrOngoingScheduledMeetingsWhileManagementKeepsHistory() throws Exception {
        Meeting past = meeting("tenant-a");
        past.setDataHoraInicio(LocalDateTime.now().minusDays(2));
        past.setDataHoraFim(LocalDateTime.now().minusDays(1));
        meetings.saveAndFlush(past);
        Meeting cancelled = meeting("tenant-a");
        cancelled.setStatus(MeetingStatus.CANCELADA);
        meetings.saveAndFlush(cancelled);
        Meeting finished = meeting("tenant-a");
        finished.setStatus(MeetingStatus.FINALIZADA);
        meetings.saveAndFlush(finished);
        Meeting ongoing = meeting("tenant-a");
        ongoing.setDataHoraInicio(LocalDateTime.now().minusMinutes(5));
        ongoing.setDataHoraFim(LocalDateTime.now().plusMinutes(30));
        meetings.saveAndFlush(ongoing);
        as(get("/api/meetings"), 2, "MORADOR").andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].id").value(ongoing.getId()))
                .andExpect(jsonPath("$[1].id").value(meetingA.getId()));
        as(get("/api/meetings"), 1, "ADMIN_SINDICO").andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test void confirmingPresenceIsPersistedAndVisibleToManagementWithoutChangingAnotherGuest() throws Exception {
        String path = "/api/meetings/" + meetingA.getId() + "/convidados/2/presenca?status=CONFIRMADO";
        as(patch(path), 2, "MORADOR").andExpect(status().isNoContent());
        as(get("/api/meetings/" + meetingA.getId()), 1, "ADMIN_SINDICO").andExpect(status().isOk())
                .andExpect(jsonPath("$.convidados[?(@.usuarioId == 2)].status").value(org.hamcrest.Matchers.contains("CONFIRMADO")))
                .andExpect(jsonPath("$.convidados[?(@.usuarioId == 3)].status").value(org.hamcrest.Matchers.contains("PENDENTE")));
        as(patch(path), 3, "MORADOR").andExpect(status().isForbidden());
    }

    @Test void concurrentResidentsOfSameUnitProduceExactlyOneVote() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            var results = pool.invokeAll(List.of(() -> as(post("/api/polls/" + pollA.getId() + "/votes")
                    .contentType("application/json").content(voteBody(pollA)), 2, "MORADOR").andReturn().getResponse().getStatus(),
                    () -> as(post("/api/polls/" + pollA.getId() + "/votes").contentType("application/json")
                            .content(voteBody(pollA)), 3, "MORADOR").andReturn().getResponse().getStatus()));
            assertThat(List.of(results.get(0).get(), results.get(1).get())).containsExactlyInAnyOrder(201, 409);
            assertThat(votes.count()).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }

    @Test void noUnitForeignOptionForeignPollAndUnavailableIdentityNeverRecordVotes() throws Exception {
        as(post("/api/polls/" + pollA.getId() + "/votes").contentType("application/json").content(voteBody(pollA)), 5, "MORADOR")
                .andExpect(status().isForbidden());
        as(post("/api/polls/" + pollA.getId() + "/votes").contentType("application/json").content(voteBody(pollA)), 7, "MORADOR")
                .andExpect(status().isForbidden());
        as(post("/api/polls/" + pollA.getId() + "/votes").contentType("application/json").content(voteBody(pollA)), 99, "MORADOR")
                .andExpect(status().isServiceUnavailable());
        as(post("/api/polls/" + pollA.getId() + "/votes").contentType("application/json").content(voteBody(pollB)), 2, "MORADOR")
                .andExpect(status().isBadRequest());
        as(post("/api/polls/" + pollB.getId() + "/votes").contentType("application/json").content(voteBody(pollB)), 2, "MORADOR")
                .andExpect(status().isNotFound());
        assertThat(votes.count()).isZero();
    }

    @Test void closingAndDeadlineRejectVotesAndLegacyHistoryIsPreserved() throws Exception {
        pollA.setDataHoraFim(LocalDateTime.now().minusSeconds(1)); polls.saveAndFlush(pollA);
        as(post("/api/polls/" + pollA.getId() + "/votes").contentType("application/json").content(voteBody(pollA)), 2, "MORADOR")
                .andExpect(status().isConflict());
        votes.saveAndFlush(PollVote.builder().poll(pollB).pollOption(pollB.getOpcoes().get(0)).usuarioId(8L).build());
        migration.run(null); migration.run(null);
        assertThat(polls.findById(pollB.getId()).orElseThrow().getStatus()).isEqualTo(PollStatus.ENCERRADA);
        assertThat(votes.count()).isEqualTo(1);
    }

    @Test void presenceAndEvaluationCannotImpersonateAnotherGuest() throws Exception {
        as(patch("/api/meetings/" + meetingA.getId() + "/convidados/3/presenca?status=CONFIRMADO"), 2, "MORADOR")
                .andExpect(status().isForbidden());
        as(post("/api/meetings/" + meetingA.getId() + "/convidados/3/avaliacoes").contentType("application/json")
                .content("{\"nota\":5,\"comentario\":\"ok\"}"), 2, "MORADOR").andExpect(status().isForbidden());
        as(patch("/api/meetings/" + meetingA.getId() + "/convidados/2/presenca?status=CONFIRMADO"), 2, "MORADOR")
                .andExpect(status().isNoContent());
    }

    @Test void createDerivesOrganizerAndTenantWhileRejectingForeignGuests() throws Exception {
        org.mockito.Mockito.doReturn("https://meet.google.com/test-room").when(google).createLink("tenant-a");
        String body = "{\"titulo\":\"Assembleia\",\"dataHoraInicio\":\"" + LocalDateTime.now().plusDays(1)
                + "\",\"dataHoraFim\":\"" + LocalDateTime.now().plusDays(1).plusHours(1)
                + "\",\"idOrganizador\":999,\"condominioId\":\"tenant-b\",\"idConvidados\":[2]}";
        var result = as(post("/api/meetings").contentType("application/json").content(body), 1, "ADMIN_SINDICO")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.idOrganizador").value(1)).andReturn();
        Long id = JSON.readTree(result.getResponse().getContentAsString()).path("id").asLong();
        assertThat(meetings.findById(id).orElseThrow().getCondominioId()).isEqualTo("tenant-a");
        assertThat(meetings.findById(id).orElseThrow().getMeetLink()).isEqualTo("https://meet.google.com/test-room");
        as(post("/api/meetings").contentType("application/json").content(body.replace("[2]", "[9]")), 1, "ADMIN_SINDICO")
                .andExpect(status().isBadRequest());
    }

    @Test void planOutageFailsClosedAndDocsRemainPublic() throws Exception {
        PLAN_STATUS.set(500);
        as(get("/api/meetings"), 2, "MORADOR").andExpect(status().isServiceUnavailable());
        mvc.perform(get("/api/meetings/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(get("/api/meetings/swagger-ui.html")).andExpect(status().is3xxRedirection());
    }

    @Test void googleAuthorizationRequiresManagerAndContractButCallbackUsesStateInsteadOfJwt() throws Exception {
        mvc.perform(get("/api/meetings/google/status")).andExpect(status().isUnauthorized());
        as(get("/api/meetings/google/status"), 2, "MORADOR").andExpect(status().isForbidden());
        as(post("/api/meetings/google/authorize"), 2, "MORADOR").andExpect(status().isForbidden());
        as(get("/api/meetings/google/status"), 1, "ADMIN_SINDICO")
                .andExpect(status().isOk()).andExpect(jsonPath("$.configured").value(false));
        PLAN_STATUS.set(403);
        as(post("/api/meetings/google/authorize"), 1, "ADMIN_SINDICO").andExpect(status().isForbidden());
        mvc.perform(get("/api/meetings/google/callback").param("state", "forged").param("code", "forged"))
                .andExpect(status().isSeeOther()).andExpect(header().string("Location", "http://localhost:5173/adm/reunioes?googleMeet=error"));
    }

    @Test void missingGoogleCredentialsDoNotPersistPartialMeetings() throws Exception {
        long before = meetings.count();
        String body = "{\"titulo\":\"Assembleia\",\"dataHoraInicio\":\"" + LocalDateTime.now().plusDays(1)
                + "\",\"dataHoraFim\":\"" + LocalDateTime.now().plusDays(1).plusHours(1) + "\",\"idConvidados\":[2]}";
        as(post("/api/meetings").contentType("application/json").content(body), 1, "ADMIN_SINDICO")
                .andExpect(status().isServiceUnavailable());
        assertThat(meetings.count()).isEqualTo(before);
    }

    @Test void managersCreatePollInTheirTenantAndCanPublishAndEditOnlyTheirMeetingsAta() throws Exception {
        var created = as(post("/api/polls").contentType("application/json")
                .content("{\"titulo\":\"Nova votação\",\"condominioId\":\"tenant-b\",\"opcoes\":[\"Sim\",\"Não\"]}"), 1, "ADMIN_SINDICO")
                .andExpect(status().isCreated()).andReturn();
        long id = JSON.readTree(created.getResponse().getContentAsString()).path("id").asLong();
        assertThat(polls.findById(id).orElseThrow().getCondominioId()).isEqualTo("tenant-a");
        as(patch("/api/polls/" + id + "/close"), 1, "ADMIN_SINDICO").andExpect(status().isNoContent());
        String ata = "{\"topicosDiscutidos\":\"Pauta\",\"decisoesTomadas\":\"Aprovada\",\"idPresentes\":[2]}";
        as(post("/api/meetings/" + meetingA.getId() + "/ata").contentType("application/json").content(ata), 1, "ADMIN_SINDICO")
                .andExpect(status().isCreated());
        assertThat(atas.findByMeetingId(meetingA.getId()).orElseThrow().getCondominioId()).isEqualTo("tenant-a");
        as(put("/api/meetings/" + meetingA.getId() + "/ata").contentType("application/json").content(ata), 1, "ADMIN_SINDICO")
                .andExpect(status().isOk());
        as(post("/api/meetings/" + meetingB.getId() + "/ata").contentType("application/json").content(ata), 1, "ADMIN_SINDICO")
                .andExpect(status().isNotFound());
        as(put("/api/meetings/" + meetingB.getId() + "/ata").contentType("application/json").content(ata), 1, "ADMIN_SINDICO")
                .andExpect(status().isNotFound());
    }
}
