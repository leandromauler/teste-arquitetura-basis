# Sistema de Saúde — Arquitetura e POC (Java 21 + PostgreSQL + Angular)

## Documento explicativo (teste técnico) — Resumo executivo

> Esta documentação atende aos requisitos solicitados no teste com: diagrama com **backend, frontend, mobile, banco e integrações externas utilizando o RNDS conforme sugestão**, respondendo as justificativas das escolhas, os **padrões aplicados**, **fluxos de dados** e **considerações de escalabilidade, segurança e manutenção**, além das respostas das **10 perguntas** solicitadas no final do teste.

### Justificativa das escolhas arquiteturais (Utilizei a maioria das Stacks que trabalho hoje)
- **Java 21 + Spring Boot 3**: plataforma madura, com um ecosistema completo, suporte a virtual threads (quando necessário) e fácil observabilidade e integração.
- **Arquitetura Hexagonal (Ports & Adapters)**: utilizaria pois desacopla o domínio da infraestrutura (RNDS, banco, filas) caso seja necessaria a mudança de stacks no andamento do projeto, reduz custo de mudança e facilita testes.
- **BFF (Backend for Frontend) opcional**: seria uma possibilidade de utilizar pois, faz payloads sob medida para Web e Mobile sem poluir a API de domínio; simplifica evolução de UX e versionamento.
- **Angular 17** - Framework moderno com Standalone Components, *Angular Material* UI/UX consistente, *PWA* Progressive Web App e *NgRx* para State management reativo.
- **PostgreSQL 16**: base relacional robusta que utiliza o **JSONB** para armazenar recursos **FHIR** completos (trilha/auditoria sem perda), bons índices e extensões.
- **Integração RNDS**: exige **Two-Way SSL**; isolei em um adaptador específico com *timeouts/retries/circuit breaker* (Resilience) e **idempotência**.
- **Observabilidade**: Utilizaria o Actuator + Micrometer/Prometheus, Loki/ELK, trilhas OpenTelemetry e logs estruturados.
- **Docker/Kubernetes**: Containerização e orquestração, utilizaria *stateless* ou invês de *statefull* para não ficar guardando o estado da reqisição, horizontalmente escalável, com rate-limit, *autoscaling* e práticas 12‑fatores.

### Padrões aplicados
- **Hexagonal (Ports & Adapters)** + **Anti-corruption Layer (ACL)** para integração com o RNDS.
- **CQRS** com **Outbox** para publicação de eventos (notificações/ETL).
- **BFF** para composição e transformação de respostas orientadas a UX.
- **Resiliência**: *timeouts*, *retry com backoff*, **circuit breaker**, *bulkhead* nos clientes externos.
- **Idempotência** (chave de deduplicação/correlationId) em operações de envio.
- **Cache-aside** com Redis (consultas de leitura frequentes e listas).
- **API Gateway** (*SpingCloud/NGINX/ingress*) com **rate limiting**, roteamento dinamico e Load Balancer.
- **Security by design**: OIDC/OAuth2 no front, mTLS para integrações, *secure headers*, CSP, política de chaves/segredos em Vault.
- **Infrastructure as Code**: docker-compose para dev e manifestos K8s para prod (pasta `infra/k8s` pode ser adicionada).

### Arquitetura Completa

```
                            ┌─────────────────────────────────┐
                            │      USUÁRIOS EXTERNOS          │
                            │  (Profissionais, Pacientes)     │
                            └─────────────┬───────────────────┘
                                          │
                            ┌─────────────▼───────────────────┐
                            │   KUBERNETES INGRESS NGINX      │
                            │  • SSL/TLS Termination          │
                            │  • Load Balancing               │
                            │  • Roteamento                   │
                            │  • Rate Limit                   │
                            └─────────────┬───────────────────┘
                                          │
┌──────────────────────────┬─────────────▼──────────────┬──────────────────────────┐
│    Angular Web           │    Angular Mobile PWA      │   Admin Dashboard        │
│  • Material Design       │  • Offline-First           │  • Monitoramento         │
│  • Responsive Layout     │  • Service Worker          │  • Analytics             │
└──────────┬───────────────┴────────────┬───────────────┴───────────┬──────────────┘
           │                            │                           │
           └────────────────────────────┼───────────────────────────┘
                                        │
                    ┌───────────────────▼────────────────────┐
                    │   SPRING CLOUD GATEWAY (API Gateway)   │
                    │  • Roteamento dinâmico                 │
                    │  • Circuit Breaker (Resilience4j)      │
                    │  • Filtro Request/Response             │
                    │  • Authentication & Authorization      │
                    └───────────────────┬────────────────────┘
                                        │
                    ┌───────────────────▼────────────────────┐
                    │   EUREKA SERVICE DISCOVERY             │
                    │  • Service Registration                │
                    │  • Health Monitoring                   │
                    │  • Load Balancing                      │
                    └───────────────────┬────────────────────┘
                                        │
        ┌───────────────────────────────┼───────────────────────────────┐
        │                               │                               │
┌───────▼──────────┐       ┌───────────▼────────────┐       ┌─────────▼─────────┐
│  Patient Service │       │  Exames Service        │       │ Integração Service│
│  (Spring Boot)   │       │  (Spring Boot)         │       │ (Spring Boot)     │
│  • CRUD Pacientes│       │  • CRUD Exames         │       │ • RNDS/FHIR       │
│  • FHIR Mapping  │       │  • Processar Resultados│       │ • APIs Externas   │
└───────┬──────────┘       └───────────┬────────────┘       └─────────┬─────────┘
        │                              │                               │
        └──────────────────────────────┼───────────────────────────────┘
                                       │
                    ┌──────────────────▼──────────────────┐
                    │   SPRING CLOUD CONFIG SERVER        │
                    │  • Configuração Centralizada        │
                    │  • Config. específicas do ambiente  │
                    │  • Hot Reload                       │
                    └──────────────────┬──────────────────┘
                                       │
        ┌──────────────────────────────┼──────────────────────────────┐
        │                              │                              │
┌───────▼──────────┐       ┌───────────▼────────────┐    ┌──────────▼──────────┐
│  APACHE KAFKA    │       │  RABBITMQ              │    │  RNDS (Externa)     │
│ ┌──────────────┐ │       │ ┌────────────────────┐ │    │ • HL7 FHIR R4       │
│ │Event Streams │ │       │ │  Queues & Topics   │ │    │ • OAuth 2.0 + mTLS  │
│ │• Exames Event│ │       │ │• Notifications     │ │    │ • Clinical Data     │
│ │• Audit Logs  │ │       │ │• Email/SMS Queue   │ │    └─────────────────────┘
│ │• CDC Events  │ │       │ │• Dead Letter Queue │ │
│ └──────────────┘ │       │ └────────────────────┘ │
└───────┬──────────┘       └───────────┬────────────┘
        │                              │
        │    ┌─────────────────────────┼─────────────────────────────┐
        │    │                         │                             │
        │    │                         │                             │
┌───────▼────▼──────┐    ┌─────────────▼──────────┐    ┌────────────▼─────────┐
│  PostgreSQL 16    │    │  Redis 7               │    │  Elasticsearch 8     │
│ • DB primário     │    │ • Camada de Cache      │    │ • Full-text Search   │
│ • Ler de Réplicas │    │ • Session Store        │    │ • Log Aggregation    │
│ • Particionamento │    │ • Pub/Sub              │    │ • Analytics          │
└───────────────────┘    └────────────────────────┘    └──────────────────────┘
        │                              │                             │
        └──────────────────────────────┼─────────────────────────────┘
                                       │
                    ┌──────────────────▼──────────────────────┐
                    │      OBSERVABILIDADE STACK              │
                    │ ┌────────────────────────────────────┐  │
                    │ │ Prometheus → Grafana (Metricas)    │  │
                    │ │ ELK Stack (Logs)                   │  │
                    │ │ Jaeger (Trilhas Distribuidas)      │  │
                    │ │ Spring Boot Actuator (Health)      │  │
                    │ └────────────────────────────────────┘  │
                    └─────────────────────────────────────────┘
```

### Integrações
- 🔗 **RNDS** - Rede Nacional de Dados em Saúde
- 🔗 **DATASUS** - Sistemas do Ministério da Saúde
- 🔗 **Laboratórios** - APIs de sistemas laboratoriais
- 🔗 **Certificação Digital A3** - Autenticação segura

### Fluxos de dados
**Fluxo A — Registro de imunização (OUTBOUND):**
1. **Web/Mobile** → **BFF** → **Core API** (DTO → recurso **FHIR** `Immunization`).
2. Validação local (HAPI FHIR — opcional) e persistência de **payload JSONB** em `fhir_message` (`SENT_PENDING`).
3. **Adaptador RNDS (mTLS)** envia o recurso; recebe ACK/erro.
4. Atualiza `status` (`ACCEPTED`/`REJECTED`) e publica evento em **Outbox** (fila/stream).
5. **Front** recebe retorno imediato (202) e pode acompanhar por **SSE/WebSocket** (quando habilitado).

**Fluxo B — Consulta de histórico (INBOUND/READ):**
1. Front pede lista filtrada (paginada) ao **BFF**.
2. **Core API** lê visões de leitura (SQL + índices + cache Redis).
3. Quando necessário, reconcilia com RNDS em *background* (fila) e atualiza visões.

### Considerações: escalabilidade, segurança e manutenção
- **Escalabilidade**: serviços *stateless*, múltiplas réplicas; **HikariCP** tunado; **Redis** para *cache/rate-limit*; trabalhos pesados via **mensageria** (Kafka/RabbitMQ); **particionamento**/índices no Postgres; *read replicas* para relatórios; *autoscaling* com métricas.
- **Segurança**: OIDC/OAuth2 (fronts), **mTLS** com RNDS; *secrets* no **Vault**; *rotate* de certificados; **LGPD** (minimização, *masking*, *audit log* separado); **Criptografia** em repouso (disco/backup) e em trânsito (TLS 1.2+); **CSP/Headers**; *WAF/Ingress*.
- **Manutenção**: **ADRs** para decisões, **OpenAPI** versionada, **testes** (unit/integration/contract), **migrations** (Flyway/Liquibase), **tracing** + **dashboards**, *feature flags*, *blue/green/canary* e **ciência de incidentes** (runbooks).

---

## Respostas às 10 perguntas

**1) Padrões arquiteturais aplicados e por quê?**  
Hexagonal: isola o domínio da infraestrutura, facilitando testes e trocas; BFF: entrega payloads sob medida para Web/Mobile, melhorando UX e desacoplando o domínio; CQRS leve + Outbox: separa escrita de leitura e garante publicação confiável de eventos na mesma transação; ACL (RNDS): mapeia/normaliza FHIR na borda, evitando “vazamento” de detalhes e reduzindo impacto de mudanças; Resilience (timeouts/retry/backoff/circuit breaker/bulkhead): mantém estabilidade diante de falhas externas; Cache-aside (Redis): reduz latência e carga no banco/integrações; Idempotência: evita duplicidades em reenvios e torna retries seguros; API Gateway: centraliza TLS/OIDC/CORS, rate-limit, roteamento e observabilidade, protegendo o backend.

**2) Comunicação entre API, frontend e mobile?**  
- **REST JSON** para operações principais, contratado por **OpenAPI**.  
- **SSE/WebSocket** para *push* (status/notifications).  
- **BFF** compondo e simplificando respostas.

**3) Estratégias para escalabilidade horizontal do backend?**  
Stateless + containers, múltiplas instâncias atrás do gateway, **cache Redis**, **fila** para tarefas pesadas, **HikariCP** ajustado, *bulkhead/circuit breaker*, **observabilidade** (para autoscaling), Postgres com **índices, particionamento e réplicas de leitura**.

**4) Segurança na integração com serviços externos?**  
**mTLS** obrigatório (keystore/truststore), *pinning* da CA quando aplicável, *timeouts* + *retry* controlado, **auditoria completa** (request/response FHIR com hash/correlationId), **segregação de rede** (egress control), **Vault** para segredos/certificados, e *least privilege*.

**5) Projeto do banco considerando integrações e performance?**  
Modelo **híbrido**: relacional para entidades do domínio e **JSONB** para **payload FHIR** *ímpar* (sem perda). Índices em colunas de busca e **GIN** sobre `payload`. **Outbox** para entrega garantida. **Particionamento** por data/status em `fhir_message` quando volume crescer.

**6) Monitoramento e logging**  
**Actuator** + **Prometheus/Grafana** (métricas, SLO p95), **OpenTelemetry** (traços distribuídos), **Loki/ELK** (logs estruturados). **Dashboards** por domínio e integrações (erros RNDS, latência, fila, throughput). **Alertas** com *burn rate* e *error budget*.

**7) Trade-offs microsserviços vs monolito?**  
**Decisão: Monolito Modular → Microsserviços Evolutivo**
**Fase 1: Monolito Modular**
Começar com **Monólito Modular**: Desenvolvimento mais rápido inicialmente, deploy simplificado (um artefato), debugging mais fácil, transações ACID nativas.
```
saude-core/
├── paciente-module/
├── exame-module/
├── integraticao-module/
└── auditoria-module/
```
**Fase 2: Microsserviços (quando necessário)**
Evoluir para microserviços *por módulo* quando houver escala/equipe/necessidade de independência de tecnologia. *Contras* dos microserviços prematuros: complexidade operacional, latência de rede, debugging distribuído complexo, *overhead* operacional aumentado e consistência distribuída.
```
services:
  - paciente-service      # Gestão de pacientes
  - integracao-service  # RNDS e integrações
  - exame-service        # Exames
  - notificacao-service # Notificações
  - auditoria-service       # Auditoria
```

**8) Versionamento de API sem quebrar consumidores**  
**URI versionada** (`/v1`, `/v2`), **depreciação comunicada** por header e changelog, **compatibilidade retroativa** enquanto possível, BFF adaptando transições, *feature flags* para ativação gradual. Contratos **OpenAPI** publicados e validados em CI.

**9) Decisões de arquitetura que influenciam a UX**  
**Performance-First Architecture** Lazy Loading + Code Splitting, **BFF** (payload “redondo”), **SSE/WebSocket** (feedback imediato), **cache** e paginação (respostas rápidas), **idempotência** (sem erros duplicados em redes móveis), **resiliência** (app não “trava” com falha externa), **CDN** para estáticos do webapp.

**10) Como documentar para equipe e stakeholders**  
- **README** (visão geral, como subir).  
- **`/docs/architecture.md`** + **diagramas** (`.mmd` e **`.drawio`**).  
- **OpenAPI/Swagger** Living Documentation (Docs as Code), gerado e publicado.  
- **ADRs** (decisões), **Runbooks/Playbooks** (observabilidade/incidentes).  
- **Roadmap** com critérios “quando fatiar para microserviços”.

---

### Referências rápidas no repositório
- **Diagrama draw.io**: `docs/diagrams/arquitetura.drawio`  
- **Documento de arquitetura**: `docs/architecture.md`  
- **POC Backend**: `backend/` (Java 21 + Spring Boot + Postgres/Redis)  
- **POC Frontend**: `frontend/saude-web` (Angular 17)

## Swagger / OpenAPI
- UI: http://localhost:8080/swagger-ui/index.html
- JSON: http://localhost:8080/v3/api-docs
- YAML: http://localhost:8080/v3/api-docs.yaml

_Atualizado em 2025-09-26._

