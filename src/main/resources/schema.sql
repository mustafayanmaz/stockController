CREATE TABLE IF NOT EXISTS products (
    product_id   BIGSERIAL     PRIMARY KEY,
    product_code VARCHAR(50)   NOT NULL UNIQUE,
    product_name VARCHAR(150)  NOT NULL,
    category     VARCHAR(100)  NOT NULL,
    unit_cost    NUMERIC(12,2) NOT NULL,
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    quantity     INTEGER       NOT NULL DEFAULT 0,
    unit         VARCHAR(30)   NOT NULL,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   VARCHAR(50),
    updated_at   TIMESTAMP,
    updated_by   VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS stock_transactions (
    transaction_id     BIGSERIAL     PRIMARY KEY,
    product_id         BIGINT        NOT NULL,
    quantity           INTEGER       NOT NULL,
    unit_cost          NUMERIC(12,2) NOT NULL,
    remaining_quantity INTEGER       NOT NULL DEFAULT 0,
    transaction_type   VARCHAR(10)   NOT NULL DEFAULT 'IN',
    transaction_date   TIMESTAMP     NOT NULL,
    created_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(50),
    updated_at         TIMESTAMP,
    updated_by         VARCHAR(50),
    CONSTRAINT fk_transaction_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS sale_orders (
    order_id     BIGSERIAL     PRIMARY KEY,
    order_code   VARCHAR(40)   NOT NULL UNIQUE,
    product_id   BIGINT        NOT NULL,
    quantity     INTEGER       NOT NULL,
    unit_price   NUMERIC(12,2) NOT NULL,
    total_cost   NUMERIC(12,2) NOT NULL,
    order_date   TIMESTAMP     NOT NULL,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   VARCHAR(50),
    updated_at   TIMESTAMP,
    updated_by   VARCHAR(50),
    CONSTRAINT fk_sale_order_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE RESTRICT
);
