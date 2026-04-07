CREATE TABLE IF NOT EXISTS products (
    product_id   BIGSERIAL    PRIMARY KEY,
    product_code VARCHAR(50)  NOT NULL UNIQUE,
    product_name VARCHAR(150) NOT NULL,
    category     VARCHAR(100) NOT NULL,
    description  VARCHAR(500),
    unit_cost    NUMERIC(12,2) NOT NULL,
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   VARCHAR(50),
    updated_at   TIMESTAMP,
    updated_by   VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS stocks (
    stock_id            BIGSERIAL   PRIMARY KEY,
    product_id          BIGINT      NOT NULL UNIQUE,
    quantity            INTEGER     NOT NULL DEFAULT 0,
    unit                VARCHAR(30) NOT NULL,
    minimum_stock_level INTEGER     NOT NULL DEFAULT 0,
    created_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(50),
    updated_at          TIMESTAMP,
    updated_by          VARCHAR(50),
    CONSTRAINT fk_stock_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS stock_transactions (
    transaction_id      BIGSERIAL   PRIMARY KEY,
    stock_id            BIGINT      NOT NULL,
    quantity            INTEGER     NOT NULL,
    unit_price          NUMERIC(12,2) NOT NULL,
    unit_cost           NUMERIC(12,2) NOT NULL,
    transaction_date    TIMESTAMP   NOT NULL,
    created_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(50),
    updated_at          TIMESTAMP,
    updated_by          VARCHAR(50),
    CONSTRAINT fk_transaction_stock FOREIGN KEY (stock_id) REFERENCES stocks(stock_id) ON DELETE CASCADE
);
