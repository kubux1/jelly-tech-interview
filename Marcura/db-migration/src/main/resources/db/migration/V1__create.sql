CREATE TABLE IF NOT EXISTS currency_exchange_rate
(
    id             INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    currency_from  VARCHAR(255)             NOT NULL,
    currency_to    VARCHAR(255)             NOT NULL,
    rate           NUMERIC(19, 6)           NOT NULL,
    exchange_date  DATE                     NOT NULL,
    access_counter INTEGER                  NOT NULL DEFAULT 0,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX currency_exchange_rate_currency_from_to_date_desc_unique_index
    ON currency_exchange_rate (currency_from, currency_to, exchange_date DESC);

CREATE TABLE IF NOT EXISTS currency_spread
(
    id         INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    currency   VARCHAR(255)             NOT NULL,
    spread     NUMERIC(4, 2)           NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

INSERT INTO currency_spread (currency, spread)
VALUES ('JPY', 3.25),
       ('HKD', 3.25),
       ('KRW', 3.25),
       ('MYR', 4.50),
       ('INR', 4.50),
       ('MXN', 4.50),
       ('RUB', 6),
       ('CNY', 6),
       ('ZAR', 6);
CREATE UNIQUE INDEX currency_spread_currency_created_at_desc_unique_index
    ON currency_spread (currency, created_at DESC);

CREATE FUNCTION update_updated_at_currency_exchange_rate_task()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_currency_exchange_rate_task_updated_at
    BEFORE UPDATE
    ON
        currency_exchange_rate
    FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_currency_exchange_rate_task();
