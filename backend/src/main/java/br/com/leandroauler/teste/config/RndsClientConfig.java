package br.com.leandroauler.teste.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.*;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

@Configuration
public class RndsClientConfig {
  @Bean
  RestClient rndsRestClient(
      @Value("{rnds.base-url}") String baseUrl,
      @Value("{rnds.ssl.client-keystore}") String clientKeystore,
      @Value("{rnds.ssl.client-password}") String clientPassword,
      @Value("{rnds.ssl.truststore}") String truststore,
      @Value("{rnds.ssl.truststore-password}") String truststorePassword
  ) throws Exception {
    KeyStore ks = KeyStore.getInstance("PKCS12");
    try (var in = Files.newInputStream(Path.of(clientKeystore.replace("file:", "")))) {
      ks.load(in, clientPassword.toCharArray());
    }
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(ks, clientPassword.toCharArray());
    KeyStore ts = KeyStore.getInstance("JKS");
    try (var in = Files.newInputStream(Path.of(truststore.replace("file:", "")))) {
      ts.load(in, truststorePassword.toCharArray());
    }
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(ts);
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
    var httpClient = HttpClient.newBuilder().sslContext(sslContext).version(HttpClient.Version.HTTP_2).build();
    var requestFactory = new JdkClientHttpRequestFactory(httpClient);
    return RestClient.builder()
        .baseUrl(baseUrl)
        .requestFactory(requestFactory)
        .defaultHeader(HttpHeaders.ACCEPT, "application/fhir+json")
        .build();
  }
}
