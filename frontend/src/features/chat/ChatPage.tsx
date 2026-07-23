import { useEffect, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useSubmitQuery } from '@/shared/hooks/useQuery';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { PageError } from '@/shared/components/PageError';
import { QueryForm } from '@/features/chat/QueryForm';
import { QueryResultView } from '@/features/chat/QueryResultView';

export function ChatPage() {
  const submitQuery = useSubmitQuery();
  const [searchParams] = useSearchParams();
  const prefilled = searchParams.get('q') ?? '';
  const hasAutoSubmitted = useRef(false);

  useEffect(() => {
    if (prefilled && !hasAutoSubmitted.current) {
      hasAutoSubmitted.current = true;
      submitQuery.mutate(prefilled);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [prefilled]);

  return (
    <div className="space-y-6">
      <QueryForm
        onSubmit={(q) => submitQuery.mutate(q)}
        isSubmitting={submitQuery.isPending}
        initialValue={prefilled}
      />
      {submitQuery.isPending && <LoadingSpinner label="Translating your question…" />}
      {submitQuery.isError && <PageError error={submitQuery.error} />}
      {submitQuery.data && <QueryResultView result={submitQuery.data} />}
    </div>
  );
}
