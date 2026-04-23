CREATE TABLE cartao (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    dia_vencimento INTEGER NOT NULL,
    limite NUMERIC(12,2) NOT NULL,
    status CHAR(1) NOT NULL,
    proprietario VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
