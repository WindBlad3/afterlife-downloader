/**
 * © 2023 Afterlife. Todos los derechos reservados
 */
package cl.afterlife.downloader.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Formatter
 *
 * @author Gabriel Rojas
 * @version 1.0
 * @since 2023-04-07
 */
@Component
@Getter
public class Formatter {

    @Autowired
    private ObjectMapper objectMapper;

    @SneakyThrows
    public String writeValueAsJsonString(Object value){
        return this.objectMapper.writeValueAsString(value);
    }

}
