/**
 * © 2023 Afterlife. Todos los derechos reservados
 */
package cl.afterlife.downloader.service;

import org.springframework.http.ResponseEntity;

import java.util.Map;

/**
 * YoutubeDownloaderService
 *
 * @author Gabriel Rojas
 * @version 1.0
 * @since 2023-04-07
 */
public interface YoutubeDownloaderService {

    ResponseEntity<Map<String,Object>> downloadVideosOfPlayListToMp3(String playlistUrl, String apikey);

}