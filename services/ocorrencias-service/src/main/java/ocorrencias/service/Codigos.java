package ocorrencias.service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * Protocolo da ocorrência e número da OS.
 *
 * Mesmo formato que o auth-api usava (`REC-<tempo em base 36>-<4 aleatórios>`),
 * para os protocolos migrados e os novos não parecerem de sistemas diferentes.
 */
final class Codigos {

    private static final String ALFABETO = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int TENTATIVAS = 5;

    private Codigos() {}

    static String gerar(String prefixo) {
        String tempo = Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        StringBuilder sufixo = new StringBuilder(4);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 4; i++) sufixo.append(ALFABETO.charAt(r.nextInt(ALFABETO.length())));
        return prefixo + "-" + tempo + "-" + sufixo;
    }

    /**
     * Um código que ainda não existe. A coluna é única: a checagem aqui evita o
     * erro no caso comum, e o índice segura o caso raro de duas ao mesmo tempo.
     */
    static String gerarUnico(String prefixo, Predicate<String> existe) {
        String codigo = gerar(prefixo);
        for (int i = 0; i < TENTATIVAS && existe.test(codigo); i++) {
            codigo = gerar(prefixo);
        }
        return codigo;
    }
}
