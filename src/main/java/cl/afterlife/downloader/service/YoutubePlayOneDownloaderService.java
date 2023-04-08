/**
 * © 2023 Afterlife. Todos los derechos reservados
 */
package cl.afterlife.downloader.service;

import org.springframework.http.ResponseEntity;

import java.util.Map;

/**
 * YoutubePlayOneDownloaderService
 *
 * @author Gabriel Rojas
 * @version 1.0
 * @since 2023-04-08
 */
public interface YoutubePlayOneDownloaderService {

    ResponseEntity<Map<String, Object>> downloadVideoToMp3(String videoUrl, String videoName);

}