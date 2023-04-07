package cl.afterlife.downloader.service;

import java.util.Map;

import org.springframework.http.ResponseEntity;

/**
 * YoutubeDownloaderService
 */
public interface YoutubeDownloaderService {

    ResponseEntity<Map<String,Object>> downloadVideosOfPlayListToMp3(String playlistUrl, String apikey);
    
}