
-- Sprint 2.0: idempotencia de pagamentos

ALTER TABLE pagamento
    ADD COLUMN idempotency_key VARCHAR(36);

-- Cada chave pode identificar apenas um pagamento.
-- Permite NULL para preservar pagamentos antigos.
CREATE UNIQUE INDEX uk_pagamento_idempotency_key
    ON pagamento (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
