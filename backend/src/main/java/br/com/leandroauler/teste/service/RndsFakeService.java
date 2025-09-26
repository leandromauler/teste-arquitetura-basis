package br.com.leandroauler.teste.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.UUID;

@Service
public class RndsFakeService {
  public Map<String,Object> processImmunization(Map<String,Object> fhir) {
    String resourceType = String.valueOf(fhir.getOrDefault("resourceType", "Immunization"));
    if (!"Immunization".equals(resourceType)) {
      return Map.of("status","REJECTED","reason","resourceType must be Immunization");
    }
    return Map.of(
      "status","ACCEPTED",
      "ackId", UUID.randomUUID().toString(),
      "receivedAt", System.currentTimeMillis()
    );
  }
}
