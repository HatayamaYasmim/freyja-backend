CREATE TABLE gastos_cartao (
    id BIGSERIAL PRIMARY KEY,
    id_cartao BIGINT NOT NULL,
    descricao VARCHAR(255),
    data_compra DATE NOT NULL,
    parcela_atual INTEGER,
    quantidade_parcelas INTEGER,
    valor NUMERIC(12,2) NOT NULL,
    status CHAR(1) NOT NULL,
    usuario VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cartao
        FOREIGN KEY (id_cartao)
        REFERENCES cartao(id)
);
