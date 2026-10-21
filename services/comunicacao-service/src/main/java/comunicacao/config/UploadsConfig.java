package comunicacao.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Serve as imagens dos avisos em `/uploads/**`.
 *
 * É o próprio serviço que serve, e não o frontend: o arquivo está no volume
 * `comunicacao_uploads`, montado só neste container. A rota fica fora do
 * `AuthFilter` porque a tag `<img>` não manda o cabeçalho `Authorization` — e o
 * nome sorteado de cada arquivo não é adivinhável.
 */
@Configuration
public class UploadsConfig implements WebMvcConfigurer {

    private final String raiz;

    public UploadsConfig(@Value("${uploads.dir:/app/uploads}") String raiz) {
        this.raiz = raiz;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // A barra no fim é obrigatória: sem ela o Spring trata o local como
        // arquivo, não pasta. `toUri()` só a coloca se a pasta já existir, e no
        // primeiro boot ela ainda não existe.
        String local = Path.of(raiz).toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(local.endsWith("/") ? local : local + "/");
    }
}
