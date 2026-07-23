import { useState } from 'react';
import { useAnomalies, useExplainAnomaly } from '@/shared/hooks/useAnomalies';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { PageError } from '@/shared/components/PageError';
import { AnomalyList } from '@/features/anomaly/AnomalyList';
import { ExplainDrawer } from '@/features/anomaly/ExplainDrawer';
import type { AnomalyResponse } from '@/shared/lib/types';

export function AnomaliesPage() {
  const anomalies = useAnomalies();
  const explainAnomaly = useExplainAnomaly();
  const [selected, setSelected] = useState<AnomalyResponse | null>(null);

  if (anomalies.isLoading) {
    return <LoadingSpinner label="Scanning for anomalies…" />;
  }

  if (anomalies.error) {
    return <PageError error={anomalies.error} />;
  }

  function handleExplain(id: number) {
    explainAnomaly.mutate(id, {
      onSuccess: (result) => setSelected(result),
    });
  }

  return (
    <div>
      {anomalies.data && (
        <AnomalyList
          anomalies={anomalies.data}
          onExplain={handleExplain}
          explainingId={explainAnomaly.isPending ? (explainAnomaly.variables ?? null) : null}
          onView={setSelected}
        />
      )}
      {selected && <ExplainDrawer anomaly={selected} onClose={() => setSelected(null)} />}
    </div>
  );
}
