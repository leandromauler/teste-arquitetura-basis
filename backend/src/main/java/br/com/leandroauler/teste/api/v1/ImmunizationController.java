package br.com.leandroauler.teste.api.v1;

import br.com.leandroauler.teste.service.RndsFakeService;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/immunizations")
public class ImmunizationController {
  private final JdbcTemplate jdbc;
  private final RndsFakeService fake;

  public ImmunizationController(JdbcTemplate jdbc, RndsFakeService fake) {
    this.jdbc = jdbc;
    this.fake = fake;
  }

  @PostMapping
  @Transactional
  public ResponseEntity<?> submit(@RequestBody Map<String,Object> fhirJson) {
    fhirJson.putIfAbsent("resourceType", "Immunization");
    UUID corr = UUID.randomUUID();
    String payload = Jsons.toJson(fhirJson);

    jdbc.update("""
      insert into fhir_message(resource_type, direction, status, payload, correlation_id)
      values (?, 'OUTBOUND','SENT_PENDING', ?::jsonb, ?)
    """, fhirJson.get("resourceType"), payload, corr);

    Map<String,Object> ack = fake.processImmunization(fhirJson);
    String status = String.valueOf(ack.getOrDefault("status","ERROR"));

    jdbc.update("""
      update fhir_message
         set status = ?
       where correlation_id = ?
    """, status, corr);

    return ResponseEntity.accepted().body(Map.of(
      "correlationId", corr.toString(),
      "ack", ack,
      "finalStatus", status
    ));
  }

  static class Jsons {
    static String toJson(Map<String,Object> map) {
      StringBuilder sb = new StringBuilder();
      appendValue(sb, map);
      return sb.toString();
    }
    @SuppressWarnings("unchecked")
    private static void appendValue(StringBuilder sb, Object v) {
      if (v == null) { sb.append("null"); return; }
      if (v instanceof String s) {
        sb.append('"').append(escape(s)).append('"');
      } else if (v instanceof Number || v instanceof Boolean) {
        sb.append(v.toString());
      } else if (v instanceof Map<?,?> m) {
        sb.append('{');
        boolean first = true;
        for (var e : m.entrySet()) {
          if (!(e.getKey() instanceof String)) continue;
          if (!first) sb.append(',');
          first = false;
          sb.append('"').append(escape((String)e.getKey())).append('"').append(':');
          appendValue(sb, e.getValue());
        }
        sb.append('}');
      } else if (v instanceof Iterable<?> it) {
        sb.append('[');
        boolean first = true;
        for (var e : it) {
          if (!first) sb.append(',');
          first = false;
          appendValue(sb, e);
        }
        sb.append(']');
      } else {
        sb.append('"').append(escape(v.toString())).append('"');
      }
    }
    private static String escape(String s) {
      return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r");
    }
  }
}
