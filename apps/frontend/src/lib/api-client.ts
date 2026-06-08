const NEXT_PUBLIC_API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export interface SignUpRequest {
  email: string;
  name: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  success: boolean;
  accessToken: string;
}

export interface CongestionResponse {
  areaName: string;
  congestionLevel: "GREEN" | "YELLOW" | "ORANGE" | "RED";
  congestionMessage: string;
  populationMin: number;
  populationMax: number;
  updateTime: string;
}

export interface QuestionRequest {
  title: string;
  content: string;
}

export interface QuestionBoardResponse {
  id: number;
  title: string;
  content: string;
  writerEmail: string;
  writerName: string;
  createdAt: string;
  updatedAt: string;
}

// 답변(댓글) 데이터 응답 인터페이스 명세
export interface AnswerResponse {
  id: number;
  content: string;
  writerEmail: string;
  writerName: string;
  createdAt: string;
  updatedAt: string;
}

export const apiClient = {
  /** 이메일 중복 체크 API */
  async checkEmail(email: string): Promise<{ isDuplicated: boolean }> {
    const res = await fetch(`${NEXT_PUBLIC_API_URL}/api/v1/users/check-email?email=${encodeURIComponent(email)}`, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
    });
    if (!res.ok) throw new Error('이메일 중복 체크에 실패했습니다.');
    return res.json();
  },

  /** 회원가입 API */
  async signUp(data: SignUpRequest): Promise<{ id: number; success: boolean }> {
    const res = await fetch(`${NEXT_PUBLIC_API_URL}/api/v1/users/signup`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error('회원가입 요청에 실패했습니다.');
    return res.json();
  },

  /** 로그인 API */
  async login(data: LoginRequest): Promise<LoginResponse> {
    const response = await fetch(`${NEXT_PUBLIC_API_URL}/api/v1/users/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });
    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || "로그인 요청 처리 중 에러가 발생했습니다.");
    }
    return response.json();
  },

  /** 실시간 혼잡도 조회 API */
  async getCongestion(areaName: string, token: string): Promise<CongestionResponse> {
    const response = await fetch(`${NEXT_PUBLIC_API_URL}/api/v1/congestion?areaName=${encodeURIComponent(areaName)}`, {
      method: "GET",
      headers: {
        "Authorization": `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });
    if (!response.ok) {
      if (response.status === 403) throw new Error("인증 토큰이 만료되었거나 접근 권한이 없습니다.");
      throw new Error("혼잡도 데이터를 가져오는 중 에러가 발생했습니다.");
    }
    return response.json();
  },

  /** Q&A 질문글 등록 API */
  async createQuestion(data: QuestionRequest, token: string): Promise<{ success: boolean; questionId: number }> {
    const response = await fetch(`${NEXT_PUBLIC_API_URL}/api/v1/questions`, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(data),
    });
    if (!response.ok) throw new Error("질문 등록에 실패했습니다.");
    return response.json();
  },

  /** Q&A 질문 목록 최신순 조회 API */
  async getAllQuestions(token: string): Promise<QuestionBoardResponse[]> {
    const response = await fetch(`${NEXT_PUBLIC_API_URL}/api/v1/questions`, {
      method: "GET",
      headers: {
        "Authorization": `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });
    if (!response.ok) throw new Error("질문 목록을 불러오지 못했습니다.");
    return response.json();
  },

  /** 질문글 단건 상세 조회 API */
  async getQuestion(id: number, token: string): Promise<QuestionBoardResponse> {
    const response = await fetch(`${NEXT_PUBLIC_API_URL}/api/v1/questions/${id}`, {
      method: "GET",
      headers: {
        "Authorization": `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });
    if (!response.ok) throw new Error("질문 상세 내용을 불러오지 못했습니다.");
    return response.json();
  },

  /** 특정 질문글의 답변 목록 조회 API */
  async getAnswers(questionId: number, token: string): Promise<AnswerResponse[]> {
    const response = await fetch(`${NEXT_PUBLIC_API_URL}/api/v1/questions/${questionId}/answers`, {
      method: "GET",
      headers: {
        "Authorization": `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });
    if (!response.ok) throw new Error("답변 목록을 불러오지 못했습니다.");
    return response.json();
  },

  /** 신규 답변(댓글) 등록 API */
  async createAnswer(questionId: number, data: { content: string }, token: string): Promise<{ success: boolean; answerId: number }> {
    const response = await fetch(`${NEXT_PUBLIC_API_URL}/api/v1/questions/${questionId}/answers`, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(data),
    });
    if (!response.ok) throw new Error("답변 등록에 실패했습니다.");
    return response.json();
  },
};