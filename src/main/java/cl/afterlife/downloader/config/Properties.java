/**
 * © 2023 Afterlife. Todos los derechos reservados
 */
package cl.afterlife.downloader.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Properties
 *
 * @author Gabriel Rojas
 * @version 1.0
 * @since 2023-04-07
 */

@Data
@Configuration
public class Properties {

    @Value("${downloader.youtube.videos-in-mp3.files-location}")
    private String mp3FilesLocation;
}
