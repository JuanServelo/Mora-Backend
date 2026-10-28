package ocorrencias.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * O job diário que marca as OS com prazo vencido (RF-24).
 *
 * Roda também na subida do serviço: com o container parado durante a
 * madrugada, a execução das 6h se perderia e as OS vencidas ficariam em dia
 * até o dia seguinte. Rodar de novo não repete nada — só pega o que ainda
 * não está atrasado, e a notificação tem chave única.
 */
@Component
@RequiredArgsConstructor
public class OrdemServicoAtrasoJob {

    private static final Logger log = LoggerFactory.getLogger(OrdemServicoAtrasoJob.class);

    private final OrdemServicoService ordemServicoService;

    @Scheduled(cron = "${ordens-servico.atraso.cron}", zone = "America/Sao_Paulo")
    public void diario() {
        executar();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void naSubida() {
        executar();
    }

    private void executar() {
        try {
            ordemServicoService.marcarAtrasadas();
        } catch (Exception e) {
            log.error("falha ao marcar ordens de serviço atrasadas", e);
        }
    }
}
