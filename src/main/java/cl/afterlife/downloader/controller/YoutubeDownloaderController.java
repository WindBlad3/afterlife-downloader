/**
 * © 2023 Afterlife. Todos los derechos reservados
 */
package cl.afterlife.downloader.controller;

import cl.afterlife.downloader.service.YoutubeDownloaderService;
import cl.afterlife.downloader.util.Formatter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


/**
 * YoutubeDownloaderController
 *
 * @author Gabriel Rojas
 * @version 1.0
 * @since 2023-04-07
 */

@Log4j2
@RestController
@RequestMapping("/youtube-downloader/v1")
public class YoutubeDownloaderController {

    @Autowired
    private YoutubeDownloaderService youtubeDownloaderService;

    @Autowired
    private Formatter formatter;

    @GetMapping(value = "/videos-to-mp3")
    public ResponseEntity<Map<String, Object>> getVideosToMp3(@RequestParam String playlistUrl, @RequestParam("apiKey") String apikey) {
        log.info("[GET] url: /youtube-downloader/videos-to-mp3 with playlistUrl: {}", playlistUrl);
        ResponseEntity<Map<String, Object>> resultOfDownload = this.youtubeDownloaderService.downloadVideosOfPlayListToMp3(playlistUrl, apikey);
        log.info("[GET] url: /youtube-downloader/videos-to-mp3 with response: {}", this.formatter.writeValueAsJsonString(resultOfDownload.getBody()));
        return resultOfDownload;
    }

}