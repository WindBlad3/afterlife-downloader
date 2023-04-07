/**
 * © 2023 Afterlife. Todos los derechos reservados
 */
package cl.afterlife.downloader.client;

import com.fasterxml.jackson.databind.JsonNode;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;

/**
 * Y2Client
 *
 * @author Gabriel Rojas
 * @version 1.0
 * @since 2023-04-07
 */
@FeignClient("y2Client")
public interface Y2Client {

    @RequestLine("POST /analyzeV2/ajax")
    @Headers({"content-Type: application/x-www-form-urlencoded", "origin: https://www.y2mate.com"})
    ResponseEntity<JsonNode> analyzeV2Ajax(@Param("k_query") String kQuery, @Param("k_page") String kPage, @Param String h1, @Param("q_auto") Integer qAuto);

    @RequestLine("POST /convertV2/index")
    @Headers({"content-Type: application/x-www-form-urlencoded", "origin: https://www.y2mate.com"})
    ResponseEntity<JsonNode> convertV2Index(@Param String vid, @Param String k);

}