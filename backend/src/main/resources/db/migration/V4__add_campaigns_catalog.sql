CREATE TABLE campaigns (
    id         UUID PRIMARY KEY,
    name       VARCHAR(160) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT campaigns_name_unique UNIQUE (name)
);

INSERT INTO campaigns (id, name) VALUES
    (gen_random_uuid(), 'fall_intake_2026'),
    (gen_random_uuid(), 'spring_intake_2026'),
    (gen_random_uuid(), 'black_friday'),
    (gen_random_uuid(), 'summer_camp_2026');
