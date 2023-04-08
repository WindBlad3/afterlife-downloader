/**
 * © 2023 Afterlife. Todos los derechos reservados
 */
package cl.afterlife.downloader.client;

import com.fasterxml.jackson.databind.JsonNode;
import feign.FeignException;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;

/**
 * YoutubeClient
 *
 * @author Gabriel Rojas
 * @version 1.0
 * @since 2023-04-07
 */
@FeignClient("ytClient")
public interface YoutubeClient {

    @RequestLine("GET /playlistItems?key={key}&playlistId={playlistId}&pageToken={pageToken}&part=snippet")
    @Headers("Accept: application/json")
    ResponseEntity<JsonNode> playlistItems(@Param String key, @Param String playlistId, @Param String pageToken) throws FeignException;

}