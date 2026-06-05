"use client";

import { useState } from "react";
import { apiClient } from "@/lib/api-client";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();

    if (isSubmitting) return;
    if (!email || !password) {
      alert("모든 필드를 정확히 입력해 주세요.");
      return;
    }

    try {
      setIsSubmitting(true);

      const response = await apiClient.login({ email, password });
      
      if (response.success && response.accessToken) {
        localStorage.setItem("accessToken", response.accessToken);
        
        alert("🎉 로그인 성공! 인증 토큰이 안전하게 발급 및 저장되었습니다.");
        
        // 추후 메인 대시보드나 혼잡도 조회 화면으로 리다이렉트(router.push)할 브릿지 구간
        console.log("발급된 JWT 토큰 스냅샷:", response.accessToken);
      }
    } catch (error: any) {
      alert(`로그인 실패: ${error.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-6 bg-slate-50">
      <div className="w-full max-w-md p-8 space-y-6 bg-white rounded-2xl shadow-xl border border-slate-100">
        <div className="space-y-2 text-center">
          <h1 className="text-3xl font-extrabold text-slate-900 tracking-tight">Seoul-Hub</h1>
          <p className="text-sm text-slate-500">통합 연동 2단계: JWT 보안 인증 시스템 검증</p>
        </div>

        <form onSubmit={handleLogin} className="space-y-4">
          {/* 이메일 섹션 */}
          <div className="space-y-1">
            <label className="text-sm font-semibold text-slate-700">이메일 주소</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="seoul@hub.com"
              className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50 text-slate-900 text-sm"
              required
              disabled={isSubmitting}
            />
          </div>

          {/* 비밀번호 섹션 */}
          <div className="space-y-1">
            <label className="text-sm font-semibold text-slate-700">비밀번호</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50 text-slate-900 text-sm"
              required
              disabled={isSubmitting}
            />
          </div>

          {/* 로그인 제출 버튼 */}
          <button
            type="submit"
            disabled={isSubmitting}
            className={`w-full py-3 font-bold text-sm rounded-lg shadow-md transition-all cursor-pointer ${
              !isSubmitting
                ? "bg-slate-900 hover:bg-slate-800 text-white"
                : "bg-slate-200 text-slate-400 cursor-not-allowed shadow-none"
            }`}
          >
            {isSubmitting ? "인증 토큰 인쇄 중..." : "로그인"}
          </button>
        </form>

        <div className="text-center">
          <a href="/" className="text-xs text-blue-600 hover:underline">
            ← 회원가입 화면으로 돌아가기
          </a>
        </div>
      </div>
    </main>
  );
}