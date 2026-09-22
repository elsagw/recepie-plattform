CREATE TABLE saved_recipes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    recipe_id BIGINT NOT NULL REFERENCES external_recipes(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, recipe_id)
);

CREATE INDEX saved_recipes_user_id_idx ON saved_recipes (user_id);
