import { formatDateTime } from '@/shared/lib/formatters';
import type { QueryHistoryItem } from '@/shared/lib/types';

export function HistoryTable({
  items,
  onRerun,
}: {
  items: QueryHistoryItem[];
  onRerun: (nlQuery: string) => void;
}) {
  if (items.length === 0) {
    return <p className="text-sm text-slate-500">No queries yet.</p>;
  }

  return (
    <div className="space-y-3">
      {items.map((item) => (
        <div key={item.id} className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="font-medium text-slate-900">{item.nlQuery}</p>
              <p className="mt-1 text-sm text-slate-500">{item.summary}</p>
              <p className="mt-2 text-xs text-slate-400">
                {formatDateTime(item.createdAt)} · {item.rowsReturned} rows · {item.totalMs}ms
              </p>
            </div>
            <button
              onClick={() => onRerun(item.nlQuery)}
              className="shrink-0 rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              Re-run
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}
