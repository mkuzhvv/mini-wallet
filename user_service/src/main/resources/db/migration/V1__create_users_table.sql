CREATE TABLE users (
                                   id                     UUID          PRIMARY KEY,
                                   email                  VARCHAR(254)  NOT NULL,
                                   password_hash          VARCHAR(255)  NOT NULL,
                                   role                   VARCHAR(20)   NOT NULL DEFAULT 'USER',
                                   status                 VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
                                   created_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
                                   updated_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),

                                   CHECK (email = lower(email)),
                                   CHECK (role IN ('USER', 'ADMIN')),
                                   CHECK (status IN ('ACTIVE', 'BLOCKED')),
                                   CONSTRAINT uq_users_email UNIQUE (email)
);