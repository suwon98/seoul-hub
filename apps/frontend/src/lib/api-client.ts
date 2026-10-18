export class ApiError extends Error { constructor(message: string, public status: number) { super(message); } }
export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : "요청 처리 중 문제가 발생했습니다.";
}
async function request<T>(path: string, body?: unknown, signal?: AbortSignal): Promise<T> {
  const response = await fetch(`/api/v1${path}`, {
    method: body === undefined ? "GET" : "POST", credentials: "same-origin", cache: "no-store",
    headers: body === undefined ? {} : { "Content-Type": "application/json", "X-SeoulHub-Request": "1" },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    signal: signal ? AbortSignal.any([signal, AbortSignal.timeout(55000)]) : AbortSignal.timeout(55000),
  });
  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new ApiError(error?.message || `요청에 실패했습니다 (${response.status}).`, response.status);
  }
  return response.json();
}
export interface SignUpRequest {
  email: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  success: boolean;
}

export interface CollectionStatus { dailyBudget: number; usedToday: number; requestDelayMs: number; cacheTtlSeconds: number; }

export interface CongestionResponse {
  freshness: "RECENT" | "DELAYED" | "EXPIRED" | "UNAVAILABLE" | "HISTORICAL";
  observationAgeMinutes: number | null;
  lastCheckedAt: string | null;
  collectedAt: string | null;
  areaCode: string;
  category: string;
  latitude: number;
  longitude: number;
  areaName: string;
  congestionLevel: "GREEN" | "YELLOW" | "ORANGE" | "RED" | "UNKNOWN";
  congestionMessage: string;
  populationMin: number;
  populationMax: number;
  updateTime: string | null;
  stale: boolean;
  sourceCongestionLevel: "GREEN" | "YELLOW" | "ORANGE" | "RED" | "UNKNOWN";
}

export interface QuestionRequest {
  title: string;
  content: string;
}

export interface QuestionBoardResponse {
  id: number;
  title: string;
  content: string;
  writerName: string;
  createdAt: string;
  updatedAt: string;
}

// 답변(댓글) 데이터 응답 인터페이스 명세
export interface AnswerResponse {
  id: number;
  content: string;
  writerName: string;
  createdAt: string;
  updatedAt: string;
}

export const apiClient = {
  refreshVisible: (body: {clientId: string; sequence: number; visible: string[]; selected: string | null}, signal?: AbortSignal) => request<{accepted: boolean}>("/congestion/refresh", body, signal),
  getCollectionStatus: (signal?: AbortSignal) => request<CollectionStatus>("/congestion/status", undefined, signal),
  signUp: (data: SignUpRequest) => request<{ success: boolean; message: string }>("/users/signup", data),
  requestVerification: (email: string) => request<{ message: string }>("/users/request-verification", { email }),
  verifyEmail: (token: string, name: string, password: string) => request<{ success: boolean }>("/users/verify-email", { token, name, password }),
  forgotPassword: (email: string) => request<{ message: string }>("/users/forgot-password", { email }),
  resetPassword: (token: string, password: string) => request<{ success: boolean }>("/users/reset-password", { token, password }),
  deleteAccount: (password: string, confirmation: string) => request<{ success: boolean }>("/users/delete-account", { password, confirmation }),
  login: (data: LoginRequest) => request<LoginResponse>("/users/login", data),
  me: () => request<{ email: string }>("/users/me"),
  logout: () => request<{ success: boolean }>("/users/logout", {}),
  getCongestion: (areaName: string) => request<CongestionResponse>(`/congestion/${encodeURIComponent(areaName)}`),
  getAllCongestion: (signal?: AbortSignal) => request<CongestionResponse[]>("/congestion/all", undefined, signal),
  getHistory: (area: string, signal?: AbortSignal) => request<CongestionResponse[]>(`/congestion/${encodeURIComponent(area)}/history`, undefined, signal),
  createQuestion: (data: QuestionRequest) => request<{ success: boolean; questionId: number }>("/questions", data),
  getAllQuestions: () => request<QuestionBoardResponse[]>("/questions"),
  getQuestion: (id: number) => request<QuestionBoardResponse>(`/questions/${id}`),
  getAnswers: (id: number) => request<AnswerResponse[]>(`/questions/${id}/answers`),
  createAnswer: (id: number, data: { content: string }) => request<{ success: boolean; answerId: number }>(`/questions/${id}/answers`, data),
};
