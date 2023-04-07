/**
 * © 2023 Afterlife. Todos los derechos reservados
 */
package cl.afterlife.downloader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * DownloaderApplication
 *
 * @author Gabriel Rojas
 * @version 1.0
 * @since 2023-04-07
 */
@EnableFeignClients
@SpringBootApplication
public class DownloaderApplication {

    public static void main(String[] args) {
        SpringApplication.run(DownloaderApplication.class, args);
    }

}
