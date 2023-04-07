package cl.afterlife.downloader.util;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class Builder {

    public ResponseEntity<Map<String,Object>> createResponse(String response){
        return ResponseEntity.ok(Map.of("Result", response));
    }

    public ResponseEntity<Map<String,Object>> createError(String response, HttpStatus httpStatus) {
        return ResponseEntity.status(httpStatus).body(Map.of("Error", response));
    }

    public ResponseEntity<Map<String,Object>> createError(String response, List<String> errors, HttpStatus httpStatus) {
        return ResponseEntity.status(httpStatus).body(Map.of("Error", response, "Details", errors));
    }
    
}
