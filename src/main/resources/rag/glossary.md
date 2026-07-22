# FinanceCopilot Glossary

## MRR (Monthly Recurring Spend)
The sum of a category's recurring, subscription-like transactions (e.g. Streaming Services,
Subscriptions, Internet & Phone) in a given month. Computed from `analytics.transactions` by
filtering to categories known to be recurring and summing `amount` grouped by month.

## Burn Rate
The net rate at which an account's balance is being depleted: total expenses minus total income
over a period, typically expressed per month. A positive burn rate means spending exceeds income
for that account in that period.

## Cashflow
The net movement of money for a period: total income minus total expenses. Cashflow is computed
from `analytics.transactions.amount`, where income categories contribute positive amounts and
expense categories contribute negative amounts.

## Run Rate
An annualized projection of the current month's (or trailing 3-month average) spend or income,
computed by multiplying the monthly figure by 12.

## Discretionary Spending
Spending on non-essential categories — Dining Out, Entertainment, Travel, Shopping, Coffee Shops —
as opposed to essential spending (Rent, Utilities, Groceries, Insurance, Healthcare).

## Savings Rate
The percentage of income not spent in a period: `(income - expenses) / income`, expressed as a
percentage. Computed per month or per account from `analytics.transactions`.

## Budget Variance
The difference between actual spend in a category for a month (from
`analytics.monthly_summary` or aggregated `analytics.transactions`) and the budgeted limit for
that category and month in `analytics.budgets`. A positive variance means the category is over
budget.

## Net Worth
The sum of balances across all accounts (assets: checking, savings, investment minus liabilities:
credit card balances). FinanceCopilot approximates this from cumulative transaction totals per
account, since account balances are not stored directly.
