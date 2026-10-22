package comunicacao.service;

import comunicacao.exception.DadosInvalidosException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImagemAvisoServiceTest {

    @TempDir
    Path raiz;

    private static byte[] imagem(int largura, int altura, int tipo, String formato) throws Exception {
        BufferedImage img = new BufferedImage(largura, altura, tipo);
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(img, formato, saida);
        return saida.toByteArray();
    }

    @Test
    void gravaOReEncodeComNomeSorteado() throws Exception {
        ImagemAvisoService servico = new ImagemAvisoService(raiz.toString());

        String url = servico.guardar(imagem(20, 10, BufferedImage.TYPE_INT_RGB, "png"));

        assertTrue(url.startsWith("/uploads/avisos/"));
        assertTrue(url.endsWith(".jpg"), "sem transparência vira JPEG");
        Path gravado = servico.pasta().resolve(url.substring("/uploads/avisos/".length()));
        assertTrue(Files.exists(gravado));
        assertNotNull(ImageIO.read(gravado.toFile()), "o que foi gravado é imagem legível");
    }

    @Test
    void mantemPngQuandoHaTransparencia() throws Exception {
        String url = new ImagemAvisoService(raiz.toString())
                .guardar(imagem(8, 8, BufferedImage.TYPE_INT_ARGB, "png"));
        assertTrue(url.endsWith(".png"));
    }

    @Test
    void recusaTextoComExtensaoDeImagem() {
        // O nome e o Content-Type não importam: o que decide é conseguir decodificar.
        byte[] falso = "isto é um .txt renomeado para .png".getBytes(StandardCharsets.UTF_8);
        ImagemAvisoService servico = new ImagemAvisoService(raiz.toString());
        assertThrows(DadosInvalidosException.class, () -> servico.guardar(falso));
    }

    @Test
    void recusaArquivoVazio() {
        ImagemAvisoService servico = new ImagemAvisoService(raiz.toString());
        assertThrows(DadosInvalidosException.class, () -> servico.guardar(new byte[0]));
    }

    @Test
    void recusaImagemComDimensaoExagerada() throws Exception {
        byte[] enorme = imagem(ImagemAvisoService.LADO_MAXIMO + 1, 1, BufferedImage.TYPE_INT_RGB, "png");
        ImagemAvisoService servico = new ImagemAvisoService(raiz.toString());
        assertThrows(DadosInvalidosException.class, () -> servico.guardar(enorme));
    }
}
