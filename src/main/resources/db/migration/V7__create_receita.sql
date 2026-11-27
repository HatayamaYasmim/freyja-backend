
CREATE TABLE receita (
    id BIGSERIAL PRIMARY KEY,

    grupo_id BIGINT NOT NULL,

    descricao VARCHAR(255) NOT NULL,

    valor_previsto NUMERIC(15, 2) NOT NULL,
    valor_recebido NUMERIC(15, 2) NOT NULL DEFAULT 0,

    competencia DATE NOT NULL,
    data_prevista DATE NOT NULL,
    data_recebimento DATE,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_receita_grupo
        FOREIGN KEY (grupo_id)
        REFERENCES grupo(id),

    CONSTRAINT ck_receita_valor_previsto
        CHECK (valor_previsto > 0),

    CONSTRAINT ck_receita_valor_recebido
        CHECK (valor_recebido >= 0),

    CONSTRAINT ck_receita_status
        CHECK (
            status IN (
                'PENDENTE',
                'PARCIAL',
                'RECEBIDA',
                'CANCELADA'
            )
        ),

    CONSTRAINT ck_receita_descricao
        CHECK (LENGTH(TRIM(descricao)) > 0)
);

CREATE INDEX idx_receita_grupo_competencia
    ON receita (grupo_id, competencia);

CREATE INDEX idx_receita_grupo_status
    ON receita (grupo_id, status);
