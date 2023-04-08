/**
 * © 2023 Afterlife. Todos los derechos reservados
 */
package cl.afterlife.downloader.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder
 *
 * @author Gabriel Rojas
 * @version 1.0
 * @since 2023-04-07
 */
@Component
public class Builder {

    public ResponseEntity<Map<String, Object>> createResponse(String response) {
        return ResponseEntity.ok(Map.of("Result", response));
    }

    public ResponseEntity<Map<String, Object>> createError(String response, HttpStatus httpStatus) {
        return ResponseEntity.status(httpStatus).body(Map.of("Error", response));
    }

    public ResponseEntity<Map<String, Object>> createError(String response, List<String> errors, HttpStatus httpStatus) {
        LinkedHashMap error = new LinkedHashMap() {{
            put("Error", response);
            put("Details", errors);
        }};
        return ResponseEntity.status(httpStatus).body(error);
    }

}
