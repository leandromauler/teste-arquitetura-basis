create table if not exists fhir_message (
  id bigserial primary key,
  resource_type text not null,
  direction text not null check (direction in ('OUTBOUND','INBOUND')),
  status text not null,
  payload jsonb not null,
  correlation_id uuid,
  created_at timestamptz default now()
);
create index if not exists idx_fhir_message_type on fhir_message(resource_type);
create index if not exists idx_fhir_message_status on fhir_message(status);
create index if not exists idx_fhir_payload_path on fhir_message using gin (payload jsonb_path_ops);
