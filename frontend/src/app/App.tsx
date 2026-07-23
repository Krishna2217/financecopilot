import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { ErrorBoundary } from '@/shared/components/ErrorBoundary';
import { Layout } from '@/app/Layout';
import { DashboardPage } from '@/features/dashboard/DashboardPage';
import { ChatPage } from '@/features/chat/ChatPage';
import { AnomaliesPage } from '@/features/anomaly/AnomaliesPage';
import { HistoryPage } from '@/features/history/HistoryPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

export function App() {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <Routes>
            <Route element={<Layout />}>
              <Route index element={<DashboardPage />} />
              <Route path="chat" element={<ChatPage />} />
              <Route path="anomalies" element={<AnomaliesPage />} />
              <Route path="history" element={<HistoryPage />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </QueryClientProvider>
    </ErrorBoundary>
  );
}

export default App;
