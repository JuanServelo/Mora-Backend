package comunicacao.service;

import comunicacao.exception.DadosInvalidosException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Guarda a imagem de um aviso.
 *
 * Três cuidados, cada um contra um ataque conhecido:
 *
 * - **O arquivo gravado é o re-encode, nunca os bytes recebidos.** Um `.txt`
 *   renomeado para `.png`, com `Content-Type` forjado, é recusado porque o
 *   decodificador não consegue ler — não porque o cabeçalho foi conferido, o que
 *   qualquer um falsifica. E um arquivo "imagem" com script embutido sai limpo,
 *   porque só os pixels sobrevivem ao re-encode.
 * - **O nome é sorteado aqui**, com extensão fixa. Nome vindo do cliente é o
 *   caminho clássico para escrever fora da pasta (`../../`).
 * - **Dimensão limitada.** Uma imagem pequena em bytes pode declarar 50.000 ×
 *   50.000 pixels e esgotar a memória ao ser decodificada.
 */
@Service
public class ImagemAvisoService {

    static final int LADO_MAXIMO = 4000;

    private final Path pasta;

    public ImagemAvisoService(@Value("${uploads.dir:/app/uploads}") String raiz) {
        this.pasta = Path.of(raiz, "avisos");
    }

    /** Valida, re-codifica e grava. Devolve o caminho público (`/uploads/avisos/...`). */
    public String guardar(byte[] conteudo) {
        if (conteudo == null || conteudo.length == 0) {
            throw new DadosInvalidosException("Envie um arquivo de imagem.");
        }

        BufferedImage imagem;
        try {
            imagem = ImageIO.read(new ByteArrayInputStream(conteudo));
        } catch (IOException e) {
            imagem = null;
        }
        if (imagem == null) {
            throw new DadosInvalidosException("O arquivo não é uma imagem válida (use PNG ou JPEG).");
        }
        if (imagem.getWidth() > LADO_MAXIMO || imagem.getHeight() > LADO_MAXIMO) {
            throw new DadosInvalidosException(
                    "Imagem grande demais: o maior lado pode ter até " + LADO_MAXIMO + " pixels.");
        }

        // PNG quando há transparência (o JPEG a perderia); JPEG no resto, que
        // para foto ocupa bem menos.
        boolean transparente = imagem.getColorModel().hasAlpha();
        String formato = transparente ? "png" : "jpg";
        BufferedImage saida = transparente ? imagem : semAlfa(imagem);

        String nome = UUID.randomUUID() + "." + formato;
        try {
            Files.createDirectories(pasta);
            if (!ImageIO.write(saida, formato, pasta.resolve(nome).toFile())) {
                throw new DadosInvalidosException("Não foi possível processar a imagem.");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar a imagem do aviso", e);
        }
        return "/uploads/avisos/" + nome;
    }

    /** O JPEG não aceita canal alfa; copia para uma imagem RGB. */
    private static BufferedImage semAlfa(BufferedImage origem) {
        if (origem.getType() == BufferedImage.TYPE_INT_RGB) return origem;
        BufferedImage rgb = new BufferedImage(origem.getWidth(), origem.getHeight(), BufferedImage.TYPE_INT_RGB);
        rgb.createGraphics().drawImage(origem, 0, 0, null);
        return rgb;
    }

    Path pasta() {
        return pasta;
    }
}
