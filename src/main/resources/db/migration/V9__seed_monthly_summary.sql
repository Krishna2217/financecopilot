INSERT INTO analytics.monthly_summary (account_id, category_id, summary_month, total_amount, transaction_count)
SELECT
    account_id,
    category_id,
    date_trunc('month', transaction_date)::date AS summary_month,
    sum(amount) AS total_amount,
    count(*) AS transaction_count
FROM analytics.transactions
GROUP BY account_id, category_id, date_trunc('month', transaction_date)::date;
