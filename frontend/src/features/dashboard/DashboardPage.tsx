import { useCashflow, useKpis, useSpendByCategory } from '@/shared/hooks/useAnalytics';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { PageError } from '@/shared/components/PageError';
import { KpiCards } from '@/features/dashboard/KpiCards';
import { SpendByCategoryChart } from '@/features/dashboard/SpendByCategoryChart';
import { CashflowChart } from '@/features/dashboard/CashflowChart';

export function DashboardPage() {
  const kpis = useKpis();
  const spendByCategory = useSpendByCategory();
  const cashflow = useCashflow();

  if (kpis.isLoading || spendByCategory.isLoading || cashflow.isLoading) {
    return <LoadingSpinner label="Loading dashboard…" />;
  }

  const error = kpis.error ?? spendByCategory.error ?? cashflow.error;
  if (error) {
    return <PageError error={error} />;
  }

  return (
    <div className="space-y-6">
      {kpis.data && <KpiCards kpis={kpis.data} />}
      <div className="grid gap-4 lg:grid-cols-2">
        {spendByCategory.data && <SpendByCategoryChart data={spendByCategory.data} />}
        {cashflow.data && <CashflowChart data={cashflow.data} />}
      </div>
    </div>
  );
}
