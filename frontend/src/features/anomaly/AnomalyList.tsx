import { Card } from '@/shared/components/Card';
import { formatCurrency, formatMonth } from '@/shared/lib/formatters';
import type { AnomalyResponse } from '@/shared/lib/types';

export function AnomalyList({
  anomalies,
  onExplain,
  explainingId,
  onView,
}: {
  anomalies: AnomalyResponse[];
  onExplain: (id: number) => void;
  explainingId: number | null;
  onView: (anomaly: AnomalyResponse) => void;
}) {
  if (anomalies.length === 0) {
    return <Card>No anomalies detected for this month.</Card>;
  }

  return (
    <div className="space-y-3">
      {anomalies.map((anomaly) => (
        <Card key={anomaly.id}>
          <div className="flex items-center justify-between gap-4">
            <div>
              <p className="font-medium text-slate-900">{anomaly.categoryName}</p>
              <p className="text-sm text-slate-500">
                {formatMonth(anomaly.month)} · {formatCurrency(anomaly.spend)} spent vs{' '}
                {formatCurrency(anomaly.meanSpend)} average (z={anomaly.zScore.toFixed(2)})
              </p>
            </div>
            <div className="flex shrink-0 gap-2">
              {anomaly.explanation ? (
                <button
                  onClick={() => onView(anomaly)}
                  className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
                >
                  View
                </button>
              ) : (
                <button
                  onClick={() => onExplain(anomaly.id)}
                  disabled={explainingId === anomaly.id}
                  className="rounded-lg bg-slate-900 px-3 py-1.5 text-sm font-medium text-white disabled:opacity-50"
                >
                  {explainingId === anomaly.id ? 'Explaining…' : 'Explain'}
                </button>
              )}
            </div>
          </div>
        </Card>
      ))}
    </div>
  );
}
