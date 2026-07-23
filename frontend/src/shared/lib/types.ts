export interface KpiSummary {
  month: string;
  totalIncome: number;
  totalExpenses: number;
  netCashflow: number;
  savingsRatePercent: number;
}

export interface CategorySpend {
  category: string;
  totalAmount: number;
  transactionCount: number;
}

export interface CashflowPoint {
  month: string;
  income: number;
  expenses: number;
  netCashflow: number;
}

export interface TrendPoint {
  month: string;
  totalAmount: number;
}

export interface AnomalyResponse {
  id: number;
  categoryId: number;
  categoryName: string;
  month: string;
  spend: number;
  meanSpend: number;
  zScore: number;
  iqrFlag: boolean;
  explanation: string | null;
}

export interface QueryResponse {
  generatedSql: string;
  rationale: string;
  columns: string[];
  rows: Record<string, unknown>[];
  summary: string;
  historyId: number;
}

export interface QueryHistoryItem {
  id: number;
  nlQuery: string;
  generatedSql: string;
  rationale: string;
  rowsReturned: number;
  summary: string;
  promptTokens: number;
  completionTokens: number;
  sqlExecMs: number;
  totalMs: number;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
