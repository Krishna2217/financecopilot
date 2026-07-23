import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQueryHistory } from '@/shared/hooks/useQuery';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { PageError } from '@/shared/components/PageError';
import { HistoryTable } from '@/features/history/HistoryTable';

export function HistoryPage() {
  const [page, setPage] = useState(0);
  const history = useQueryHistory(page);
  const navigate = useNavigate();

  if (history.isLoading) {
    return <LoadingSpinner label="Loading history…" />;
  }

  if (history.error) {
    return <PageError error={history.error} />;
  }

  function handleRerun(nlQuery: string) {
    navigate(`/chat?q=${encodeURIComponent(nlQuery)}`);
  }

  return (
    <div className="space-y-4">
      {history.data && <HistoryTable items={history.data.content} onRerun={handleRerun} />}
      {history.data && history.data.totalPages > 1 && (
        <div className="flex items-center justify-center gap-3 text-sm">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="rounded-lg border border-slate-300 px-3 py-1.5 font-medium text-slate-700 disabled:opacity-40"
          >
            Previous
          </button>
          <span className="text-slate-500">
            Page {page + 1} of {history.data.totalPages}
          </span>
          <button
            onClick={() => setPage((p) => Math.min(history.data!.totalPages - 1, p + 1))}
            disabled={page + 1 >= history.data.totalPages}
            className="rounded-lg border border-slate-300 px-3 py-1.5 font-medium text-slate-700 disabled:opacity-40"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}
