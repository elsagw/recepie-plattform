CREATE TABLE external_recipes (
    id BIGSERIAL PRIMARY KEY,
    source_url VARCHAR(2048) NOT NULL UNIQUE,
    title VARCHAR(512),
    image_url VARCHAR(2048),
    domain VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
