package cl.afterlife.downloader.dto;

import java.util.List;

import org.springframework.http.HttpStatus;

import lombok.Builder;
import lombok.Data;

/**
 * TraceDto
 */
@Data
@Builder
public class TraceDto {

    private String message;
    private String value;
    private Boolean processedCorrectly;
    
}