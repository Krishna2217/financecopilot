-- Generates 50,000 transactions spread over the last 24 months across the seeded
-- accounts/categories/merchants. Row-numbered dimension CTEs + arithmetic random offsets
-- avoid an ORDER BY random() per generated row, which would be far slower at this volume.
INSERT INTO analytics.transactions (account_id, category_id, amount, currency, description, merchant, transaction_date)
WITH acc_ids AS (
    SELECT id, row_number() OVER (ORDER BY id) AS rn FROM analytics.accounts
), cat_ids AS (
    SELECT id, name, is_income, row_number() OVER (ORDER BY id) AS rn FROM analytics.categories
), acc_count AS (SELECT count(*) AS n FROM analytics.accounts),
   cat_count AS (SELECT count(*) AS n FROM analytics.categories),
   merchants AS (
       SELECT unnest(ARRAY[
           'Amazon', 'Walmart', 'Starbucks', 'Shell', 'Target', 'Uber', 'Netflix',
           'Whole Foods', 'Costco', 'Delta Airlines', 'Spotify', 'CVS Pharmacy',
           'Local Market', 'Trader Joes'
       ]) AS name
   ),
   merchant_ids AS (SELECT name, row_number() OVER () AS rn FROM merchants),
   merchant_count AS (SELECT count(*) AS n FROM merchant_ids),
   gen AS (
       SELECT
           g AS seq,
           (floor(random() * (SELECT n FROM acc_count)) + 1)::int AS acc_rn,
           (floor(random() * (SELECT n FROM cat_count)) + 1)::int AS cat_rn,
           (floor(random() * (SELECT n FROM merchant_count)) + 1)::int AS merch_rn,
           (current_date - (floor(random() * 730))::int) AS txn_date
       FROM generate_series(1, 50000) AS g
   )
SELECT
    acc_ids.id,
    cat_ids.id,
    CASE
        WHEN cat_ids.is_income THEN round((500 + random() * 4500)::numeric, 2)
        ELSE round((-1) * (3 + random() * 497)::numeric, 2)
    END AS amount,
    'USD',
    cat_ids.name || ' - ' || merchant_ids.name,
    merchant_ids.name,
    gen.txn_date
FROM gen
JOIN acc_ids ON acc_ids.rn = gen.acc_rn
JOIN cat_ids ON cat_ids.rn = gen.cat_rn
JOIN merchant_ids ON merchant_ids.rn = gen.merch_rn;
