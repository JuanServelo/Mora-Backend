package portaria.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import portaria.client.AuthApiClient;
import portaria.client.ComunicacaoClient;
import portaria.model.Entrega;
import portaria.model.PreAutorizacao;
import portaria.model.Reserva;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Quem é avisado de cada evento da portaria.
 *
 * Juntar aqui, em vez de espalhar chamadas pelos serviços, é o que mantém o
 * texto e a escolha do destinatário num lugar só — e é a escolha do
 * destinatário que erra com mais facilidade: avisar o porteiro de algo que é
 * do morador, ou o morador de algo que é do síndico, transforma a caixa de
 * notificações em ruído e as pessoas param de ler.
 *
 * Toda publicação falha aberta: o {@link ComunicacaoClient} engole o erro e
 * registra no log. Notificação é aviso, não registro — a reserva, a entrada e
 * a entrega já foram gravadas quando estes métodos rodam.
 */
@Service
@RequiredArgsConstructor
public class NotificacaoPortariaService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoPortariaService.class);
    private static final DateTimeFormatter DT_BR =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final ComunicacaoClient comunicacao;
    private final AuthApiClient authApiClient;

    // ─── Reservas ────────────────────────────────────────────────────────────

    /**
     * Reserva nasceu pendente: o síndico precisa saber que há algo esperando
     * decisão dele.
     *
     * Sem este aviso, a solicitação só aparece para quem abre a aba de
     * aprovações por conta própria — e expira em 24h sem ninguém ter visto.
     */
    public void reservaAguardandoAprovacao(Reserva reserva) {
        List<AuthApiClient.PessoaUnidade> sindicos =
                authApiClient.sindicos(reserva.getCondominioId());
        if (sindicos.isEmpty()) {
            log.warn("[notificacao] nenhum síndico encontrado para avisar da reserva {}",
                    reserva.getId());
            return;
        }

        String quem = reserva.getResponsavelNome() != null
                ? reserva.getResponsavelNome() : "Um morador";
        String mensagem = quem + " solicitou " + nomeDoEspaco(reserva) + " para "
                + reserva.getInicio().format(DT_BR) + ". Decida até "
                + ReservaService.prazoDecisao(reserva).format(DT_BR)
                + ", ou a solicitação expira.";

        for (AuthApiClient.PessoaUnidade sindico : sindicos) {
            comunicacao.notificar(
                    sindico.id(), reserva.getCondominioId(), "RESERVA",
                    "Reserva aguardando sua aprovação", mensagem,
                    // Uma chave por síndico: dois síndicos recebem o mesmo
                    // evento, e uma chave só faria o segundo ser descartado
                    // como repetição.
                    "portaria:RESERVA_PENDENTE:" + reserva.getId() + ":" + sindico.id(),
                    Map.of("reservaId", reserva.getId(), "acao", "aprovar"));
        }
    }

    /** Decisão tomada: quem pediu precisa saber, e no caso da recusa, por quê. */
    public void reservaDecidida(Reserva reserva, boolean aprovada, String motivo) {
        String destinatario = reserva.getSolicitanteId();
        if (destinatario == null || destinatario.isBlank()) return;

        String titulo = aprovada ? "Reserva confirmada" : "Reserva recusada";
        String quando = reserva.getInicio().format(DT_BR);

        String mensagem = aprovada
                ? nomeDoEspaco(reserva) + " está confirmado para " + quando + "."
                // A justificativa vai no corpo, não num campo que a tela possa
                // deixar de mostrar: recusar sem dizer o motivo obriga a pessoa
                // a procurar alguém para entender.
                : nomeDoEspaco(reserva) + " não foi liberado para " + quando + ". Motivo: "
                  + (motivo != null && !motivo.isBlank() ? motivo : "não informado") + ".";

        comunicacao.notificar(
                destinatario, reserva.getCondominioId(), "RESERVA",
                titulo, mensagem,
                "portaria:RESERVA_" + (aprovada ? "APROVADA" : "RECUSADA") + ":" + reserva.getId(),
                Map.of("reservaId", reserva.getId()));
    }

    // ─── Pré-autorização ─────────────────────────────────────────────────────

    /**
     * O visitante pré-liberado chegou.
     *
     * Vai para quem fez a pré-liberação, não para a unidade inteira: foi essa
     * pessoa que assumiu a autorização, e é ela que espera a visita.
     */
    public void visitantePreAprovadoEntrou(PreAutorizacao pa) {
        String morador = pa.getMoradorId();
        if (morador == null || morador.isBlank()) return;

        String visitante = pa.getNomeVisitante() != null ? pa.getNomeVisitante() : "Seu visitante";
        String comVeiculo = pa.getPlacaVeiculo() != null && !pa.getPlacaVeiculo().isBlank()
                ? " (veículo " + pa.getPlacaVeiculo() + ")" : "";

        comunicacao.notificar(
                morador, pa.getCondominioId(), "PRE_AUTORIZACAO",
                "Seu visitante chegou",
                visitante + comVeiculo + " entrou no condomínio pela portaria.",
                "portaria:PRE_AUTORIZACAO_ENTRADA:" + pa.getId(),
                Map.of("preAutorizacaoId", pa.getId()));
    }

    // ─── Entregas ────────────────────────────────────────────────────────────

    /** A encomenda chegou e está na portaria (RF-7). */
    public void entregaRecebida(Entrega entrega) {
        if (entrega.getDestinatarioId() == null) return;

        String oQue = entrega.getDescricao() != null && !entrega.getDescricao().isBlank()
                ? entrega.getDescricao() : "Uma encomenda";
        String deQuem = entrega.getRemetente() != null && !entrega.getRemetente().isBlank()
                ? " enviada por " + entrega.getRemetente() : "";

        comunicacao.notificar(
                String.valueOf(entrega.getDestinatarioId()), entrega.getCondominioId(), "ENTREGA",
                "Encomenda na portaria",
                oQue + deQuem + " chegou e está na portaria" + ondeFica(entrega)
                        + ". Retire quando puder.",
                "portaria:ENTREGA:" + entrega.getId(),
                Map.of("entregaId", entrega.getId()));
    }

    /**
     * A encomenda foi retirada.
     *
     * Dois avisos com propósitos diferentes: o destinatário fica sabendo que a
     * encomenda dele saiu da portaria e por quem — é o caso em que alguém da
     * casa retira por ele; e quem retirou recebe o comprovante do que levou.
     * Quando são a mesma pessoa, sai um aviso só.
     */
    public void entregaRetirada(Entrega entrega, String recebedorId) {
        String oQue = entrega.getDescricao() != null && !entrega.getDescricao().isBlank()
                ? entrega.getDescricao() : "Sua encomenda";
        String porQuem = entrega.getRecebedorNome() != null ? entrega.getRecebedorNome() : "alguém";
        String destinatarioId = entrega.getDestinatarioId() != null
                ? String.valueOf(entrega.getDestinatarioId()) : null;

        if (destinatarioId != null) {
            comunicacao.notificar(
                    destinatarioId, entrega.getCondominioId(), "ENTREGA",
                    "Encomenda retirada",
                    oQue + " foi retirada da portaria por " + porQuem + ".",
                    "portaria:ENTREGA_RETIRADA:" + entrega.getId(),
                    Map.of("entregaId", entrega.getId()));
        }

        boolean outraPessoa = recebedorId != null && !recebedorId.isBlank()
                && !recebedorId.equals(destinatarioId);
        if (outraPessoa) {
            String deQuem = entrega.getDestinatarioNome() != null
                    ? " de " + entrega.getDestinatarioNome() : "";
            comunicacao.notificar(
                    recebedorId, entrega.getCondominioId(), "ENTREGA",
                    "Você retirou uma encomenda",
                    "Registramos a retirada" + deQuem + ": " + oQue + ".",
                    "portaria:ENTREGA_RETIRADA_RECEBEDOR:" + entrega.getId(),
                    Map.of("entregaId", entrega.getId()));
        }
    }

    // ─── apoio ───────────────────────────────────────────────────────────────

    private static String nomeDoEspaco(Reserva r) {
        return r.getAreaComum() != null && r.getAreaComum().getNome() != null
                ? "O espaço " + r.getAreaComum().getNome()
                : "O espaço reservado";
    }

    private static String ondeFica(Entrega e) {
        return e.getBloco() != null && e.getApartamento() != null
                ? " (" + e.getBloco() + " " + e.getApartamento() + ")" : "";
    }
}
