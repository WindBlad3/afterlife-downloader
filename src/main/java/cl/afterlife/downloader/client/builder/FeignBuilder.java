package cl.afterlife.downloader.client.builder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import feign.Client;
import feign.Contract;
import feign.Feign;
import feign.Feign.Builder;
import feign.codec.Encoder;
import feign.slf4j.Slf4jLogger;
import feign.codec.Decoder;

/**
 * FeignBuilder
 */
@Component
public class FeignBuilder {

    @Autowired
    private Client client;
    
    @Autowired
    private Encoder Encoder;
    
    @Autowired
    private Decoder Decoder;

    @Autowired
    private Contract Contract;

    public Feign.Builder builder (String url, Class<?> type){
        return Feign.builder()
        .client(client)
        .encoder(Encoder)
        .decoder(Decoder)
        .contract(Contract)
        .logger(new Slf4jLogger(type))
        .target(type, url);
    }
}