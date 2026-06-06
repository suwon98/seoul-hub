"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { apiClient, CongestionResponse } from "@/lib/api-client";

export default function DashboardPage() {
  const router = useRouter();
  const [token, setToken] = useState<string | null>(null);
  const [selectedArea, setSelectedArea] = useState("강남역");
  const [data, setData] = useState<CongestionResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // [안정성 조치] 데이터 조회 로직을 캡슐화하여 캐싱 및 재사용 보장
  const fetchCongestionData = useCallback(async (area: string, authToken: string) => {
    try {
      setLoading(true);
      setError(null);
      const response = await apiClient.getCongestion(area, authToken);
      setData(response);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  // [보안성 조치] 화면이 켜지자마자 브라우저 금고에서 토큰 검증 수행 (라우팅 가드)
  useEffect(() => {
    const storedToken = localStorage.getItem("accessToken");
    if (!storedToken) {
      alert("로그인을 먼저 진행해 주세요.");
      router.push("/login");
      return;
    }
    setToken(storedToken);
    fetchCongestionData(selectedArea, storedToken);
  }, [router, selectedArea, fetchCongestionData]);

  // 로그아웃 처리
  const handleLogout = () => {
    localStorage.removeItem("accessToken");
    alert("안전하게 로그아웃 되었습니다.");
    router.push("/login");
  };

  // 혼잡도 레벨별 태일윈드 동적 컬러 바인딩 매트릭스
  const getLevelStyles = (level: string) => {
    switch (level) {
      case "GREEN": return { bg: "bg-emerald-50 border-emerald-200", text: "text-emerald-700", badge: "bg-emerald-500" };
      case "YELLOW": return { bg: "bg-amber-50 border-amber-200", text: "text-amber-700", badge: "bg-amber-500" };
      case "ORANGE": return { bg: "bg-orange-50 border-orange-200", text: "text-orange-700", badge: "bg-orange-500" };
      case "RED": return { bg: "bg-rose-50 border-rose-200", text: "text-rose-700", badge: "bg-rose-500" };
      default: return { bg: "bg-slate-50 border-slate-200", text: "text-slate-700", badge: "bg-slate-500" };
    }
  };

  const styles = data ? getLevelStyles(data.congestionLevel) : getLevelStyles("NONE");

  return (
    <main className="min-h-screen bg-slate-50 p-6 sm:p-10 text-slate-900">
      <div className="max-w-4xl mx-auto space-y-8">
        
        {/* 네비게이션 헤더 바 */}
        <header className="flex flex-col sm:flex-row justify-between items-start sm:items-center p-6 bg-white border border-slate-100 rounded-2xl shadow-sm gap-4">
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">Seoul-Hub Realtime Dashboard</h1>
            <p className="text-xs text-slate-500">통합 연동 3단계: JWT 검증 기반 실시간 혼잡도 대시보드</p>
          </div>
          <button
            onClick={handleLogout}
            className="px-4 py-2 text-xs font-bold text-slate-600 border border-slate-200 hover:bg-slate-50 rounded-lg transition-all cursor-pointer"
          >
            로그아웃
          </button>
        </header>

        {/* 제어 컨트롤러 섹션 */}
        <section className="p-6 bg-white border border-slate-100 rounded-2xl shadow-sm flex flex-col sm:flex-row items-center gap-4 justify-between">
          <div className="flex items-center gap-3 w-full sm:w-auto">
            <label className="text-sm font-bold text-slate-700 shrink-0">관측 거점 선택</label>
            <select
              value={selectedArea}
              onChange={(e) => setSelectedArea(e.target.value)}
              className="px-4 py-2 bg-slate-50 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm font-medium w-full sm:w-48 text-slate-800"
            >
              <option value="강남역">강남역 🚉</option>
              <option value="홍대입구역">홍대입구역 🎨</option>
              <option value="강남대로">강남대로 🛣️</option>
              <option value="테헤란로">테헤란로 🏢</option>
              <option value="신림역">신림역 🏘️</option>
            </select>
          </div>

          <button
            onClick={() => token && fetchCongestionData(selectedArea, token)}
            disabled={loading}
            className="w-full sm:w-auto px-5 py-2 bg-slate-900 hover:bg-slate-800 text-white font-bold text-sm rounded-lg shadow-sm transition-all cursor-pointer disabled:opacity-50"
          >
            {loading ? "데이터 갱신 중..." : "실시간 수동 동기화 🔄"}
          </button>
        </section>

        {/* 실시간 모니터링 카드 판넬 */}
        {error ? (
          <div className="p-6 bg-rose-50 border border-rose-200 rounded-2xl text-rose-700 text-sm font-semibold shadow-sm">
            🔴 에러 발생: {error}
          </div>
        ) : data ? (
          <article className={`p-8 border rounded-3xl shadow-md transition-all space-y-6 ${styles.bg}`}>
            <div className="flex justify-between items-start">
              <div>
                <span className="text-xs font-bold uppercase tracking-widest text-slate-400">Current Location</span>
                <h2 className="text-4xl font-black text-slate-900 tracking-tight mt-1">{data.areaName}</h2>
              </div>
              <div className={`flex items-center gap-2 px-4 py-1.5 rounded-full text-white font-black text-xs tracking-wider shadow-sm ${styles.badge}`}>
                {data.congestionLevel}
              </div>
            </div>

            <div className="p-5 bg-white/80 backdrop-blur-xs rounded-xl border border-white/50 shadow-xs">
              <p className={`text-base font-bold leading-relaxed ${styles.text}`}>
                {data.congestionMessage}
              </p>
            </div>

            {/* 가상 유동 인구 정보 영역 */}
            <div className="grid grid-cols-2 gap-4">
              <div className="p-4 bg-white/40 rounded-xl border border-white/20">
                <span className="text-xs font-semibold text-slate-500">최소 예상 인파</span>
                <p className="text-2xl font-extrabold text-slate-900 mt-0.5">{data.populationMin.toLocaleString()} 명</p>
              </div>
              <div className="p-4 bg-white/40 rounded-xl border border-white/20">
                <span className="text-xs font-semibold text-slate-500">최대 예상 인파</span>
                <p className="text-2xl font-extrabold text-slate-900 mt-0.5">{data.populationMax.toLocaleString()} 명</p>
              </div>
            </div>

            <div className="text-right">
              <span className="text-xs font-medium text-slate-400">
                마지막 자동 적재 시점: {new Date(data.updateTime).toLocaleString()}
              </span>
            </div>
          </article>
        ) : (
          <div className="text-center py-12 text-slate-400 text-sm font-medium">
            관측 데이터를 로드하고 있습니다...
          </div>
        )}
      </div>
    </main>
  );
}