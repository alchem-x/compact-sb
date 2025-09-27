package app;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class CompactSBApp {

    @GetMapping("/")
    public ResponseEntity<?> hi() {
        return ResponseEntity.ok("compact-sb");
    }
}
