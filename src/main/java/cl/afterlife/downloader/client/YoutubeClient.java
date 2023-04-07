package cl.afterlife.downloader.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

/**
 * Y2Client
 */
@FeignClient( value = "ytClient", url = "https://youtube.googleapis.com/youtube/v3")
public interface YoutubeClient {

   @RequestLine("GET /playlistItems?key={key}&playlistId={playlistId}&pageToken={pageToken}&part=snippet")
   @Headers( "Accept: application/json")
   ResponseEntity<JsonNode> playlistItems (@Param String key, @Param String playlistId, @Param String pageToken);

}