package cl.afterlife.downloader.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.SneakyThrows;

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
