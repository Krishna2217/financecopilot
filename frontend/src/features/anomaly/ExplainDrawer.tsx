import { formatCurrency, formatMonth } from '@/shared/lib/formatters';
import type { AnomalyResponse } from '@/shared/lib/types';

export function ExplainDrawer({
  anomaly,
  onClose,
}: {
  anomaly: AnomalyResponse;
  onClose: () => void;
}) {
  return (
    <div className="fixed inset-0 z-10 flex justify-end bg-black/30" onClick={onClose}>
      <div
        className="h-full w-full max-w-md overflow-y-auto bg-white p-6 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-slate-900">{anomaly.categoryName}</h2>
          <button onClick={onClose} className="text-sm text-slate-500 hover:text-slate-800">
            Close
          </button>
        </div>

        <dl className="mb-6 grid grid-cols-2 gap-4 text-sm">
          <div>
            <dt className="text-slate-500">Month</dt>
            <dd className="font-medium text-slate-900">{formatMonth(anomaly.month)}</dd>
          </div>
          <div>
            <dt className="text-slate-500">Spend</dt>
            <dd className="font-medium text-slate-900">{formatCurrency(anomaly.spend)}</dd>
          </div>
          <div>
            <dt className="text-slate-500">Mean Spend</dt>
            <dd className="font-medium text-slate-900">{formatCurrency(anomaly.meanSpend)}</dd>
          </div>
          <div>
            <dt className="text-slate-500">Z-Score</dt>
            <dd className="font-medium text-slate-900">{anomaly.zScore.toFixed(2)}</dd>
          </div>
        </dl>

        <h3 className="mb-2 text-sm font-medium text-slate-500">Explanation</h3>
        <p className="text-slate-800">
          {anomaly.explanation ?? 'No explanation generated yet.'}
        </p>
      </div>
    </div>
  );
}
