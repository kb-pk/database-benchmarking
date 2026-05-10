package bench.app.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthcheckController {
    @GetMapping("/healthcheck")
    public Map<String, String> getHealthcheck() {
        return Map.of("status", "ok");
    }
}
