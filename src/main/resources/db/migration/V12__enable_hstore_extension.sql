-- Spring AI's PgVectorStore schema initialization runs, under the app's own (finance_app)
-- datasource: `CREATE EXTENSION IF NOT EXISTS {vector,hstore}`, `CREATE SCHEMA IF NOT EXISTS
-- public`, then the vector_store table/index (already created by V5). Postgres skips the
-- privilege check for CREATE EXTENSION once the extension already exists (so pre-installing
-- hstore here, as superuser, makes that step a no-op) but CREATE SCHEMA always checks
-- CREATE-on-database privilege and CREATE INDEX always checks table ownership, existing or
-- not — finance_app needs the grant and the ownership transfer regardless.
CREATE EXTENSION IF NOT EXISTS hstore;

DO $$
BEGIN
    EXECUTE format('GRANT CREATE ON DATABASE %I TO finance_app', current_database());
END
$$;

GRANT CREATE ON SCHEMA public TO finance_app;
ALTER TABLE public.vector_store OWNER TO finance_app;
