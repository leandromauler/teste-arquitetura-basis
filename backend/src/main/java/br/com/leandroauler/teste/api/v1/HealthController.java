package br.com.leandroauler.teste.api.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
  @GetMapping
  public ResponseEntity<Map<String,Object>> get() {
    return ResponseEntity.ok(Map.of("status","UP","service","teste-backend"));
  }
}
