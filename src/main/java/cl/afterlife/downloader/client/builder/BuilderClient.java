/**
 * © 2023 Afterlife. Todos los derechos reservados
 */
package cl.afterlife.downloader.client.builder;

import feign.Feign;
import feign.Logger;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.slf4j.Slf4jLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

/**
 * BuilderClient
 *
 * @author Gabriel Rojas
 * @version 1.0
 * @since 2023-04-07
 */
@Import(FeignClientsConfiguration.class)
@Component
public class BuilderClient {

    @Autowired
    private Decoder decoder;

    @Autowired
    private Encoder encoder;

    public Object createClient(String url, Class<?> type) {
        return Feign
                .builder()
                .encoder(encoder)
                .decoder(decoder)
                .contract(new feign.Contract.Default())
                .logger(new Slf4jLogger(BuilderClient.class))
                .logLevel(Logger.Level.FULL)
                .target(type, url);
    }
}