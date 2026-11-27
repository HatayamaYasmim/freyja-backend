
ALTER TABLE pagamento
    ADD COLUMN estornado BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN estornado_em TIMESTAMP WITH TIME ZONE,
    ADD COLUMN motivo_estorno VARCHAR(500);

ALTER TABLE pagamento
    ADD CONSTRAINT ck_pagamento_estorno_consistente
    CHECK (
        (
            estornado = FALSE
            AND estornado_em IS NULL
            AND motivo_estorno IS NULL
        )
        OR
        (
            estornado = TRUE
            AND estornado_em IS NOT NULL
            AND motivo_estorno IS NOT NULL
            AND LENGTH(TRIM(motivo_estorno)) > 0
        )
    );

CREATE INDEX idx_pagamento_ativo_lancamento
    ON pagamento (lancamento_id, data_pagamento)
    WHERE estornado = FALSE;
