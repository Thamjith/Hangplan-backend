CREATE TABLE IF NOT EXISTS subscription_plans (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    duration_days INT,
    description TEXT
);

INSERT INTO subscription_plans (name, duration_days, description) VALUES
    ('FREE', NULL, 'Default free plan'),
    ('PAID_1Y', 365, 'Paid plan for 1 year')
ON CONFLICT (name) DO NOTHING;

DO
$$
    BEGIN
        IF EXISTS (SELECT 1
                   FROM information_schema.tables
                   WHERE table_schema = 'public'
                     AND table_name = 'users')
            AND EXISTS (SELECT 1
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = 'users'
                          AND column_name = 'is_premium') THEN
            IF NOT EXISTS (SELECT 1
                           FROM information_schema.columns
                           WHERE table_schema = 'public'
                             AND table_name = 'users'
                             AND column_name = 'subscription_plan_id') THEN
                ALTER TABLE users ADD COLUMN subscription_plan_id INT;
            END IF;
            IF NOT EXISTS (SELECT 1
                           FROM information_schema.columns
                           WHERE table_schema = 'public'
                             AND table_name = 'users'
                             AND column_name = 'subscription_start') THEN
                ALTER TABLE users ADD COLUMN subscription_start TIMESTAMP;
            END IF;
            IF NOT EXISTS (SELECT 1
                           FROM information_schema.columns
                           WHERE table_schema = 'public'
                             AND table_name = 'users'
                             AND column_name = 'subscription_end') THEN
                ALTER TABLE users ADD COLUMN subscription_end TIMESTAMP;
            END IF;

            UPDATE users
            SET subscription_plan_id = (SELECT id FROM subscription_plans WHERE name = 'FREE')
            WHERE subscription_plan_id IS NULL;

            UPDATE users
            SET subscription_plan_id   = (SELECT id FROM subscription_plans WHERE name = 'PAID_1Y'),
                subscription_start = NOW(),
                subscription_end   = NOW() + INTERVAL '1 year'
            WHERE is_premium = true;

            ALTER TABLE users DROP COLUMN is_premium;

            IF NOT EXISTS (SELECT 1
                           FROM pg_constraint
                           WHERE conname = 'fk_subscription_plan') THEN
                ALTER TABLE users
                    ADD CONSTRAINT fk_subscription_plan
                        FOREIGN KEY (subscription_plan_id) REFERENCES subscription_plans (id);
            END IF;
        END IF;
    END
$$;
