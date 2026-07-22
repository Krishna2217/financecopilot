INSERT INTO analytics.categories (name, is_income) VALUES
    ('Salary', true),
    ('Bonus', true),
    ('Interest Income', true),
    ('Rent', false),
    ('Mortgage', false),
    ('Groceries', false),
    ('Dining Out', false),
    ('Coffee Shops', false),
    ('Utilities', false),
    ('Internet & Phone', false),
    ('Transportation', false),
    ('Ride Share', false),
    ('Fuel', false),
    ('Insurance', false),
    ('Healthcare', false),
    ('Entertainment', false),
    ('Streaming Services', false),
    ('Shopping', false),
    ('Clothing', false),
    ('Electronics', false),
    ('Travel', false),
    ('Education', false),
    ('Childcare', false),
    ('Fitness', false),
    ('Subscriptions', false),
    ('Charitable Giving', false),
    ('Transfer', false),
    ('Fees & Charges', false);

INSERT INTO analytics.accounts (name, account_type, institution, currency) VALUES
    ('Primary Checking', 'CHECKING', 'First National Bank', 'USD'),
    ('High-Yield Savings', 'SAVINGS', 'Ally Bank', 'USD'),
    ('Rewards Credit Card', 'CREDIT_CARD', 'Chase', 'USD'),
    ('Travel Credit Card', 'CREDIT_CARD', 'American Express', 'USD'),
    ('Joint Checking', 'CHECKING', 'First National Bank', 'USD'),
    ('Emergency Fund', 'SAVINGS', 'Marcus by Goldman Sachs', 'USD'),
    ('Brokerage Cash', 'INVESTMENT', 'Fidelity', 'USD'),
    ('Business Checking', 'CHECKING', 'Chase', 'USD');

-- One budget per expense category for each of the last 12 months.
INSERT INTO analytics.budgets (category_id, budget_month, amount_limit)
SELECT
    c.id,
    date_trunc('month', gs)::date AS budget_month,
    round((50 + random() * 950)::numeric, 2) AS amount_limit
FROM analytics.categories c
CROSS JOIN generate_series(
    date_trunc('month', now()) - interval '11 months',
    date_trunc('month', now()),
    interval '1 month'
) AS gs
WHERE c.is_income = false;
