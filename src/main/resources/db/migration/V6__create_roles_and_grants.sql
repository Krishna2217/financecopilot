DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_roles WHERE rolname = 'finance_ro') THEN
        CREATE ROLE finance_ro LOGIN;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_roles WHERE rolname = 'finance_app') THEN
        CREATE ROLE finance_app LOGIN;
    END IF;
END
$$;

ALTER ROLE finance_ro PASSWORD '${financeRoPassword}';
ALTER ROLE finance_app PASSWORD '${financeAppPassword}';

-- finance_ro: read-only on the analytics schema, used exclusively by AI-generated SQL.
-- Defense in depth alongside the app-level SET LOCAL statement_timeout in SqlExecutor.
ALTER ROLE finance_ro SET statement_timeout = '5s';

GRANT CONNECT ON DATABASE financecopilot TO finance_ro;
GRANT USAGE ON SCHEMA analytics TO finance_ro;
GRANT SELECT ON ALL TABLES IN SCHEMA analytics TO finance_ro;
ALTER DEFAULT PRIVILEGES IN SCHEMA analytics GRANT SELECT ON TABLES TO finance_ro;

-- finance_app: the main application datasource. Full read/write on app.*, read-only on
-- analytics.* (used by the non-AI analytics dashboard endpoints).
GRANT CONNECT ON DATABASE financecopilot TO finance_app;
GRANT USAGE ON SCHEMA analytics TO finance_app;
GRANT SELECT ON ALL TABLES IN SCHEMA analytics TO finance_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA analytics GRANT SELECT ON TABLES TO finance_app;

GRANT USAGE ON SCHEMA app TO finance_app;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA app TO finance_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA app TO finance_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA app GRANT ALL ON TABLES TO finance_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA app GRANT USAGE, SELECT ON SEQUENCES TO finance_app;
