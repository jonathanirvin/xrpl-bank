CREATE TABLE IF NOT EXISTS bank_accounts
(
	id                BIGSERIAL PRIMARY KEY,
	user_id           BIGINT       NOT NULL REFERENCES users (id),
	account_type      VARCHAR(50)  NOT NULL,
	wallet_identifier VARCHAR(255) NOT NULL UNIQUE,
	classic_address   VARCHAR(255) NOT NULL UNIQUE,
	x_address         VARCHAR(255) NOT NULL UNIQUE,
	created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
	updated_at        TIMESTAMP    NOT NULL DEFAULT NOW()
)