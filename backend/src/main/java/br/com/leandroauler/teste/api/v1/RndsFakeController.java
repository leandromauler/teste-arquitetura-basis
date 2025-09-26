package br.com.leandroauler.teste.api.v1;

import br.com.leandroauler.teste.service.RndsFakeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/rnds-fake")
public class RndsFakeController {
  private final RndsFakeService fake;
  public RndsFakeController(RndsFakeService fake) { this.fake = fake; }

  @PostMapping("/Immunization")
  public ResponseEntity<?> post(@RequestBody Map<String,Object> fhir) {
    return ResponseEntity.ok(fake.processImmunization(fhir));
  }
}
