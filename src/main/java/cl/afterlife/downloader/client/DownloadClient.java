/**
 * © 2023 Afterlife. Todos los derechos reservados
 */
package cl.afterlife.downloader.client;

import feign.FeignException;
import feign.RequestLine;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * DownloadClient
 *
 * @author Gabriel Rojas
 * @version 1.0
 * @since 2023-04-07
 */
@FeignClient("downloadClient")
public interface DownloadClient {

    @RequestLine("GET")
    byte[] downloadDlink(String url) throws FeignException;

}