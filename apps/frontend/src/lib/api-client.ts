const NEXT_PUBLIC_API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

// 회원가입 요청 DTO 타입 정의
export interface SignUpRequest {
  email: string;
  name: string;
  password: string;
}

// 로그인 요청/응답 규격 인터페이스
export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  success: boolean;
  accessToken: string;
}

// 혼잡도 API 데이터
export interface CongestionResponse {
  areaName: string;
  congestionLevel: "GREEN" | "YELLOW" | "ORANGE" | "RED";
  congestionMessage: string;
  populationMin: number;
  populationMax: number;
  updateTime: string;
}

export const apiClient = {
  /**
   * 이메일 중복 체크 API
   * GET /api/v1/users/check-email?email=...
   */
  async checkEmail(email: string): Promise<{ isDuplicated: boolean }> {
    const res = await fetch(`${NEXT_PUBLIC_API_URL}/api/v1/users/check-email?email=${encodeURIComponent(email)}`, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
    });
    if (!res.ok) throw new Error('이메일 중복 체크에 실패했습니다.');
    return res.json();
  },

  /**
   * 회원가입 API
   * POST /api/v1/users/signup
   */
  async signUp(data: SignUpRequest): Promise<{ id: number; success: boolean }> {
    const res = await fetch(`${NEXT_PUBLIC_API_URL}/api/v1/users/signup`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error('회원가입 요청에 실패했습니다.');
    return res.json();
  },

  /**
   * 로그인 API
   * POST /api/v1/users/login
   */
  async login(data: LoginRequest): Promise<LoginResponse> {
    const response = await fetch(`${NEXT_PUBLIC_API_URL}/api/v1/users/login`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(data),
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || "로그인 요청 처리 중 에러가 발생했습니다.");
    }

    return response.json();
  },

  /**
   * 인증 토큰을 헤더에 실어서 혼잡도를 조회
   * GET /api/v1/congestion?areaName=...
   */
  async getCongestion(areaName: string, token: string): Promise<CongestionResponse> {
    const response = await fetch(`${NEXT_PUBLIC_API_URL}/api/v1/congestion?areaName=${encodeURIComponent(areaName)}`, {
      method: "GET",
      headers: {
        "Authorization": `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });

    if (!response.ok) {
      if (response.status === 403) {
        throw new Error("인증 토큰이 만료되었거나 접근 권한이 없습니다. 다시 로그인해 주세요.");
      }
      throw new Error("혼잡도 데이터를 가져오는 중 에러가 발생했습니다.");
    }

    return response.json();
  },
};