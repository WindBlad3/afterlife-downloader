/**
 * © 2023 Afterlife. Todos los derechos reservados
 */
package cl.afterlife.downloader.controller;

import cl.afterlife.downloader.service.YoutubePlayOneDownloaderService;
import cl.afterlife.downloader.service.YoutubePlaylistDownloaderService;
import cl.afterlife.downloader.util.Formatter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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
@RequestMapping("/youtube/v1")
public class YoutubeDownloaderController {

    @Autowired
    private YoutubePlaylistDownloaderService youtubePlaylistDownloaderService;

    @Autowired
    private YoutubePlayOneDownloaderService youtubePlayOneDownloaderService;

    @Autowired
    private Formatter formatter;

    @GetMapping(value = "/videos-to-mp3", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getVideosToMp3(@RequestParam String playlistUrl, @RequestParam("apiKey") String apikey) {
        log.info("[REST GET] url: /youtube/v1/videos-to-mp3 with playlistUrl: {}", playlistUrl);
        ResponseEntity<Map<String, Object>> resultOfDownload = this.youtubePlaylistDownloaderService.downloadVideosOfPlayListToMp3(playlistUrl, apikey);
        log.info("[END REST GET] url: /youtube/videos-to-mp3 with response: {}", this.formatter.writeValueAsJsonString(resultOfDownload.getBody()));
        return resultOfDownload;
    }

    @GetMapping(value = "/video-to-mp3", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getVideoToMp3(@RequestParam String videoUrl, @RequestParam String videoName) {
        log.info("[REST GET] url: /youtube/v1/video-to-mp3 with videoUrl {} and videoName: {}", videoUrl, videoName);
        ResponseEntity<Map<String, Object>> resultOfDownload = this.youtubePlayOneDownloaderService.downloadVideoToMp3(videoUrl, videoName);
        log.info("[END REST GET] url: /youtube/v1/video-to-mp3 with response: {}", this.formatter.writeValueAsJsonString(resultOfDownload.getBody()));
        return resultOfDownload;
    }

}