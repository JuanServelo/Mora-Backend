package comunicacao.controller;

import comunicacao.model.Notificacao;
import comunicacao.service.NotificacaoService;
import comunicacao.service.NotificacaoService.Publicacao;
import comunicacao.service.NotificacaoService.Resultado;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * Por onde outros serviços publicam notificação para um usuário.
 *
 * O caso que motiva a rota é o fechamento do mês no financeiro: ele roda como
 * **job, sem usuário logado**, então não há JWT para repassar — e um JWT
 * fabricado não passaria, porque o auth-api confere o usuário no banco. A
 * credencial aqui é um segredo compartilhado entre serviços, `SERVICO_TOKEN`.
 *
 * Fica fora do `AuthFilter`; a autenticação é feita aqui.
 */
@RestController
@RequestMapping("/interno/notificacoes")
public class NotificacaoInternaController {

    private final NotificacaoService notificacaoService;
    private final byte[] tokenServico;

    public NotificacaoInternaController(NotificacaoService notificacaoService,
                                        @Value("${servico.token:}") String tokenServico) {
        this.notificacaoService = notificacaoService;
        this.tokenServico = tokenServico == null ? new byte[0] : tokenServico.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> publicar(
            @RequestHeader(value = "X-Servico-Token", required = false) String token,
            @RequestBody Publicacao publicacao) {

        // Sem token configurado a rota fica FECHADA, e não aberta. Uma variável
        // esquecida no deploy não pode virar uma porta para qualquer um escrever
        // na caixa de entrada de qualquer usuário.
        if (tokenServico.length == 0) {
            return resposta(HttpStatus.SERVICE_UNAVAILABLE,
                    Map.of("sucesso", false, "mensagem", "Publicação entre serviços não configurada."));
        }
        if (!tokenConfere(token)) {
            return resposta(HttpStatus.UNAUTHORIZED,
                    Map.of("sucesso", false, "mensagem", "Token de serviço inválido."));
        }

        Resultado r;
        try {
            r = notificacaoService.publicarDeServico(publicacao);
        } catch (DataIntegrityViolationException e) {
            // Duas publicações simultâneas com a mesma chave: a segunda esbarrou
            // no índice único. Para quem publicou, é uma repetição como outra.
            Notificacao existente = notificacaoService.buscarPorChave(publicacao.chaveUnica());
            if (existente == null) throw e;
            r = new Resultado(existente, true);
        }

        Map<String, Object> corpo = new HashMap<>();
        corpo.put("sucesso", true);
        corpo.put("repetida", r.repetida());
        corpo.put("notificacao", r.notificacao());
        return resposta(r.repetida() ? HttpStatus.OK : HttpStatus.CREATED, corpo);
    }

    /**
     * Comparação em tempo constante.
     *
     * `equals` para no primeiro caractere diferente, e o tempo da resposta
     * entrega quantos acertaram — dá para descobrir o token por tentativa,
     * caractere a caractere.
     */
    boolean tokenConfere(String recebido) {
        if (recebido == null) return false;
        return MessageDigest.isEqual(tokenServico, recebido.getBytes(StandardCharsets.UTF_8));
    }

    private static ResponseEntity<Map<String, Object>> resposta(HttpStatus status, Map<String, Object> corpo) {
        return ResponseEntity.status(status).body(corpo);
    }
}
