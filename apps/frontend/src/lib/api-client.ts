// 백엔드 스프링 부트 서버 주소 (로컬 개발 환경 기준 기본값 8080)
const NEXT_PUBLIC_API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

// 회원가입 요청 DTO 타입 정의
export interface SignUpRequest {
  email: string;
  name: string;
  password: string;
}

// 혼잡도 기록 응답 데이터 타입 정의
export interface CongestionResponse {
  id?: number;
  areaName?: string;
  congestionLevel?: string;
  observedAt?: string;
}

export const apiClient = {
  /**
   * 1. 이메일 중복 체크 API
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
   * 2. 회원가입 API
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
   * 3. 혼잡도 데이터 조회 API
   * GET /api/v1/congestion
   */
  async getCongestion(): Promise<CongestionResponse[]> {
    const res = await fetch(`${NEXT_PUBLIC_API_URL}/api/v1/congestion`, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
      next: { revalidate: 0 }, // 실시간 데이터를 위해 Next.js 캐싱 비활성화
    });
    if (!res.ok) throw new Error('혼잡도 데이터를 가져오지 못했습니다.');
    return res.json();
  },
};