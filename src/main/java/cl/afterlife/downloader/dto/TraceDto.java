/**
 * © 2023 Afterlife. Todos los derechos reservados
 */
package cl.afterlife.downloader.dto;

import lombok.Builder;
import lombok.Data;

/**
 * TraceDto
 *
 * @author Gabriel Rojas
 * @version 1.0
 * @since 2023-04-07
 */
@Data
@Builder
public class TraceDto {

    private String message;
    private String value;
    private Boolean processedCorrectly;

}