"use client";

import { useState } from "react";
import { apiClient } from "@/lib/api-client";

export default function Home() {
  const [email, setEmail] = useState("");
  const [name, setName] = useState("");
  const [password, setPassword] = useState("");

  const [emailChecked, setEmailChecked] = useState(false);
  const [isDuplicated, setIsDuplicated] = useState<boolean | null>(null);
  const [statusMessage, setStatusMessage] = useState("");
  
  // 💡 [무결성 보강] 비동기 요청 중 버튼 연타를 원천 차단하기 위한 락(Lock) 상태
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleCheckEmail = async () => {
    if (!email) {
      alert("이메일을 입력해 주세요.");
      return;
    }
    try {
      const result = await apiClient.checkEmail(email);
      setIsDuplicated(result.isDuplicated);
      setEmailChecked(true);
      if (result.isDuplicated) {
        setStatusMessage("❌ 이미 사용 중인 이메일입니다.");
      } else {
        setStatusMessage("✅ 사용 가능한 이메일입니다.");
      }
    } catch (error: any) {
      setStatusMessage(`🔴 에러 발생: ${error.message}`);
    }
  };

  const handleSignUp = async (e: React.FormEvent) => {
    e.preventDefault();

    // 💡 [방어선 1] 이미 통신이 진행 중이라면 두 번째 요청은 즉시 무시하고 폐기합니다.
    if (isSubmitting) return;

    if (!emailChecked || isDuplicated) {
      alert("이메일 중복 체크를 먼저 완료해 주세요.");
      return;
    }
    if (!name || !password) {
      alert("모든 필드를 정확히 입력해 주세요.");
      return;
    }

    try {
      // 💡 [방어선 2] 통신 진입과 동시에 성문을 걸어 잠급니다.
      setIsSubmitting(true);

      const response = await apiClient.signUp({ email, name, password });
      if (response.success) {
        alert(`🎉 회원가입 성공! (User ID: ${response.id})`);
        setEmail("");
        setName("");
        setPassword("");
        setEmailChecked(false);
        setIsDuplicated(null);
        setStatusMessage("");
      }
    } catch (error: any) {
      alert(`🔴 회원가입 실패: ${error.message}`);
    } finally {
      // 💡 [방어선 3] 가입이 성공하든 에러가 나든 통신이 완전히 종료되면 락을 해제합니다.
      setIsSubmitting(false);
    }
  };

  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-6 bg-slate-50">
      <div className="w-full max-w-md p-8 space-y-6 bg-white rounded-2xl shadow-xl border border-slate-100">
        <div className="space-y-2 text-center">
          <h1 className="text-3xl font-extrabold text-slate-900 tracking-tight">Seoul-Hub</h1>
          <p className="text-sm text-slate-500">통합 연동 1단계: 회원 정규 시스템 검증</p>
        </div>

        <form onSubmit={handleSignUp} className="space-y-4">
          {/* 이메일 입력 섹션 */}
          <div className="space-y-1">
            <label className="text-sm font-semibold text-slate-700">이메일 주소</label>
            <div className="flex gap-2">
              <input
                type="email"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  setEmailChecked(false);
                  setIsDuplicated(null);
                  setStatusMessage("");
                }}
                placeholder="seoul@hub.com"
                className="flex-1 px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50 text-slate-900 text-sm"
                required
                disabled={isSubmitting}
              />
              <button
                type="button"
                onClick={handleCheckEmail}
                disabled={isSubmitting}
                className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-white font-medium text-sm rounded-lg transition-colors cursor-pointer disabled:bg-slate-400 disabled:cursor-not-allowed"
              >
                중복 확인
              </button>
            </div>
            {statusMessage && (
              <p className={`text-xs font-medium mt-1 ${isDuplicated ? "text-red-500" : "text-green-600"}`}>
                {statusMessage}
              </p>
            )}
          </div>

          {/* 이름 입력 섹션 */}
          <div className="space-y-1">
            <label className="text-sm font-semibold text-slate-700">이름</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="홍길동"
              className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-slate-50 text-slate-900 text-sm"
              required
              disabled={isSubmitting}
            />
          </div>

          {/* 비밀번호 입력 섹션 */}
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

          {/* 회원가입 제출 버튼 */}
          <button
            type="submit"
            disabled={!emailChecked || isDuplicated === true || isSubmitting}
            className={`w-full py-3 font-bold text-sm rounded-lg shadow-md transition-all cursor-pointer ${
              emailChecked && !isDuplicated && !isSubmitting
                ? "bg-blue-600 hover:bg-blue-700 text-white"
                : "bg-slate-200 text-slate-400 cursor-not-allowed shadow-none"
            }`}
          >
            {/* 💡 사용자가 락의 상태를 인지할 수 있도록 실시간 텍스트 피드백 */}
            {isSubmitting ? "가입 요청 중..." : "가입하기"}
          </button>
        </form>
      </div>
    </main>
  );
}