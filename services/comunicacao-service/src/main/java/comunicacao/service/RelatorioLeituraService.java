package comunicacao.service;

import comunicacao.clients.AuthClient;
import comunicacao.model.Aviso;
import comunicacao.security.Autorizacao;
import comunicacao.security.CondominioUtils;
import comunicacao.security.PublicoAlvoRegra;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Quem leu os comunicados — a visão da administração.
 *
 * Um número de confirmações sozinho não diz nada: 8 confirmaram é ótimo se o
 * aviso era para 9 pessoas, e é um problema se era para 80. Por isso o relatório
 * traz o denominador, calculado com a mesma regra que decide quem recebe o aviso
 * (`PublicoAlvoRegra`).
 */
@Service
@RequiredArgsConstructor
public class RelatorioLeituraService {

    private final AvisoService avisoService;
    private final AvisoLeituraService leituraService;
    private final AuthClient authClient;

    /** Avisos vigentes do condomínio, cada um com o total de confirmações. */
    public List<Map<String, Object>> panorama() {
        Autorizacao.exigirVisaoDeGestao("acompanhar a leitura dos comunicados");

        List<Aviso> avisos = avisoService.listarAtivos(CondominioUtils.condominioIdEfetivo());
        Map<UUID, Long> confirmacoes = leituraService.confirmacoesPorAviso(
                avisos.stream().map(Aviso::getId).toList());

        List<Map<String, Object>> saida = new ArrayList<>();
        for (Aviso a : avisos) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId());
            item.put("titulo", a.getTitulo());
            item.put("publicoAlvo", a.getPublicoAlvo());
            item.put("dataInicio", a.getDataInicio());
            item.put("dataFim", a.getDataFim());
            item.put("autor", a.getAutor());
            item.put("imagemUrl", a.getImagemUrl());
            item.put("confirmacoes", confirmacoes.getOrDefault(a.getId(), 0L));
            saida.add(item);
        }
        return saida;
    }

    /**
     * Quem deveria ler um aviso e quem já confirmou.
     *
     * O total vem do auth-api. **Sem resposta dele, o total sai nulo**, com uma
     * observação — nunca um número inventado. Um relatório dizendo "5 de 5"
     * porque só se contou quem leu levaria o síndico a achar que todos viram.
     */
    public Map<String, Object> relatorio(UUID avisoId, String authorization) {
        Autorizacao.exigirVisaoDeGestao("ver o relatório de leitura");
        Aviso aviso = avisoService.buscarPorId(avisoId);
        Map<String, LocalDateTime> leituras = leituraService.leiturasDoAviso(avisoId);

        List<AuthClient.Usuario> usuarios =
                authClient.listarUsuariosDoCondominio(aviso.getCondominioId(), authorization);

        Map<String, Object> saida = new HashMap<>();
        saida.put("avisoId", avisoId);

        if (usuarios == null) {
            List<Map<String, Object>> soQuemLeu = new ArrayList<>();
            leituras.forEach((usuarioId, lidoEm) -> soQuemLeu.add(destinatario(usuarioId, null, null, lidoEm)));
            saida.put("total", null);
            saida.put("confirmados", leituras.size());
            saida.put("observacao",
                    "Não foi possível consultar os moradores agora. O número de destinatários "
                            + "aparece quando o serviço de usuários voltar; abaixo, só quem já confirmou.");
            saida.put("destinatarios", soQuemLeu);
            return saida;
        }

        List<Map<String, Object>> destinatarios = usuarios.stream()
                .filter(AuthClient.Usuario::alcancavel)
                .filter(u -> PublicoAlvoRegra.ehDestinatario(aviso.getPublicoAlvo(), u.perfil()))
                .map(u -> destinatario(u.id(), u.nome(), u.perfil(), leituras.get(u.id())))
                // Quem ainda não leu primeiro: é a lista que o síndico precisa cobrar.
                .sorted(Comparator.comparing((Map<String, Object> d) -> (Boolean) d.get("lido"))
                        .thenComparing(d -> String.valueOf(d.get("nome"))))
                .toList();

        saida.put("total", destinatarios.size());
        saida.put("confirmados", destinatarios.stream().filter(d -> (Boolean) d.get("lido")).count());
        saida.put("observacao", null);
        saida.put("destinatarios", destinatarios);
        return saida;
    }

    private static Map<String, Object> destinatario(String usuarioId, String nome, String perfil, LocalDateTime lidoEm) {
        Map<String, Object> d = new HashMap<>();
        d.put("usuarioId", usuarioId);
        d.put("nome", nome);
        d.put("perfil", perfil);
        d.put("lido", lidoEm != null);
        d.put("lidoEm", lidoEm);
        return d;
    }
}
