package br.com.leandroauler.teste.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class OpenApiConfig {
  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("Saúde API — POC RNDS")
            .description("Documentação OpenAPI gerada via Springdoc.")
            .version("v1")
            .contact(new Contact().name("Arquitetura").email("arquitetura@example.com")))
        .servers(List.of(new Server().url("http://localhost:8080").description("Dev")));
  }
}
