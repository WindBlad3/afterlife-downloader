package cl.afterlife.downloader.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Contract;

/**
 * FeignConfiguration
 */
@Configuration
public class FeignConfiguration {

    @Bean
    public Contract contract() {
        return new Contract.Default();
    }
    
}