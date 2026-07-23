import { Card } from '@/shared/components/Card';
import { formatCurrency } from '@/shared/lib/formatters';
import type { KpiSummary } from '@/shared/lib/types';

export function KpiCards({ kpis }: { kpis: KpiSummary }) {
  const items = [
    { label: 'Total Income', value: kpis.totalIncome },
    { label: 'Total Expenses', value: kpis.totalExpenses },
    { label: 'Net Cashflow', value: kpis.netCashflow },
    { label: 'Savings Rate', value: `${kpis.savingsRatePercent}%`, raw: true },
  ];

  return (
    <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
      {items.map((item) => (
        <Card key={item.label} title={item.label}>
          <p className="text-2xl font-semibold text-slate-900">
            {item.raw ? item.value : formatCurrency(item.value as number)}
          </p>
        </Card>
      ))}
    </div>
  );
}
