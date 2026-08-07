"use client";

import { useEffect, useState, useRef, useCallback } from "react";
import { useRouter } from "next/navigation";
import Script from "next/script";
import { ResponsiveContainer, LineChart, Line, XAxis, YAxis, Tooltip } from "recharts";

interface CongestionResponse {
  areaName: string;
  congestionLevel: "GREEN" | "YELLOW" | "ORANGE" | "RED" | "UNKNOWN";
  congestionMessage: string;
  populationMin: number;
  populationMax: number;
  updateTime: string;
}

interface KakaoLatLng {
  getLat(): number;
  getLng(): number;
}

interface KakaoMapInstance {
  setCenter(latlng: KakaoLatLng): void;
  setLevel(level: number): void;
}

interface KakaoCustomOverlay {
  setMap(map: KakaoMapInstance | null): void;
}

declare global {
  interface Window {
    kakao: {
      maps: {
        LatLng: new (lat: number, lng: number) => KakaoLatLng;
        Map: new (container: HTMLDivElement, options: { center: KakaoLatLng; level: number }) => KakaoMapInstance;
        CustomOverlay: new (options: { position: KakaoLatLng; content: string; yAnchor: number }) => KakaoCustomOverlay;
        load: (callback: () => void) => void;
      };
    };
  }
}

const AREA_COORDINATES: Record<string, { lat: number; lng: number }> = {
  "강남역": { lat: 37.497942, lng: 127.027621 },
  "홍대입구역": { lat: 37.557192, lng: 126.924403 },
  "신림역": { lat: 37.484269, lng: 126.929676 },
  "명동": { lat: 37.563692, lng: 126.982211 },
  "이태원": { lat: 37.534533, lng: 126.994584 },
  "잠실역": { lat: 37.513261, lng: 127.100159 },
  "서울역": { lat: 37.554722, lng: 126.970833 },
  "여의도": { lat: 37.521569, lng: 126.924311 }
};

type FilterType = "ALL" | "GREEN" | "YELLOW" | "ORANGE" | "RED" | "UNKNOWN";

export default function DashboardPage() {
  const router = useRouter();
  const mapRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<KakaoMapInstance | null>(null);
  const activeOverlaysRef = useRef<KakaoCustomOverlay[]>([]);

  const [kakaoLoaded, setKakaoLoaded] = useState(false);
  const [congestionList, setCongestionList] = useState<CongestionResponse[]>([]);
  const [selectedArea, setSelectedArea] = useState<CongestionResponse | null>(null);
  const [historyList, setHistoryList] = useState<CongestionResponse[]>([]);
  const [selectedFilter, setSelectedFilter] = useState<FilterType>("ALL");
  const [searchInput, setSearchInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchAllCongestion = useCallback(async () => {
    const token = localStorage.getItem("accessToken");
    if (!token) {
      router.push("/login");
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const res = await fetch("http://localhost:8080/api/v1/congestion/all", {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (!res.ok) throw new Error("전체 데이터 동기화 실패");
      const data: CongestionResponse[] = await res.json();
      setCongestionList(data);
      if (data.length > 0 && !selectedArea) {
        setSelectedArea(data[0]);
      }
    } catch (err: any) {
      setError(err.message || "데이터를 불러오는 중 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  }, [router, selectedArea]);

  const fetchAreaHistory = useCallback(async (areaName: string) => {
    const token = localStorage.getItem("accessToken");
    if (!token) return;

    try {
      const res = await fetch(`http://localhost:8080/api/v1/congestion/${encodeURIComponent(areaName)}/history`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (!res.ok) return;
      const data: CongestionResponse[] = await res.json();
      setHistoryList(data);
    } catch {
      setHistoryList([]);
    }
  }, []);

  useEffect(() => {
    if (selectedArea) {
      fetchAreaHistory(selectedArea.areaName);
    }
  }, [selectedArea, fetchAreaHistory]);

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!searchInput.trim()) return;

    const token = localStorage.getItem("accessToken");
    if (!token) {
      router.push("/login");
      return;
    }

    const areaName = searchInput.trim();
    try {
      setLoading(true);
      setError(null);
      const res = await fetch(`http://localhost:8080/api/v1/congestion/${encodeURIComponent(areaName)}`, {
        headers: { Authorization: `Bearer ${token}` }
      });

      if (!res.ok) throw new Error(`'${areaName}' 거점 수집 실패`);

      const data: CongestionResponse = await res.json();
      
      setCongestionList((prev) => {
        const filtered = prev.filter((item) => item.areaName !== data.areaName);
        return [...filtered, data];
      });
      setSelectedArea(data);
      setSearchInput("");

      const coords = AREA_COORDINATES[data.areaName] || { lat: 37.5665, lng: 126.9780 };
      if (mapInstanceRef.current && window.kakao) {
        mapInstanceRef.current.setCenter(new window.kakao.maps.LatLng(coords.lat, coords.lng));
      }
    } catch (err: any) {
      setError(err.message || "검색 중 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const renderMapAndMarkers = useCallback(() => {
    if (!kakaoLoaded || !mapRef.current || !window.kakao) return;

    const defaultCoords = AREA_COORDINATES["강남역"];
    const initialLatLng = new window.kakao.maps.LatLng(defaultCoords.lat, defaultCoords.lng);

    if (!mapInstanceRef.current) {
      mapInstanceRef.current = new window.kakao.maps.Map(mapRef.current, {
        center: initialLatLng,
        level: 6
      });
    }

    activeOverlaysRef.current.forEach((overlay) => overlay.setMap(null));
    activeOverlaysRef.current = [];

    // 혼잡도 등급 필터링 적용
    const filteredList = congestionList.filter((item) => {
      if (selectedFilter === "ALL") return true;
      return item.congestionLevel === selectedFilter;
    });

    filteredList.forEach((item) => {
      const coords = AREA_COORDINATES[item.areaName];
      if (!coords) return;

      const latLng = new window.kakao.maps.LatLng(coords.lat, coords.lng);

      let color = "#10b981";
      if (item.congestionLevel === "YELLOW") color = "#f59e0b";
      if (item.congestionLevel === "ORANGE") color = "#ea580c";
      if (item.congestionLevel === "RED") color = "#ef4444";
      if (item.congestionLevel === "UNKNOWN") color = "#94a3b8";

      const displayLevel = item.congestionLevel === "UNKNOWN" ? "정보 없음" : item.congestionLevel;

      const content = `
        <div style="padding: 6px 12px; background-color: white; border: 2px solid #1e293b; border-radius: 20px; font-weight: bold; font-size: 11px; box-shadow: 0 4px 10px rgba(0,0,0,0.15); display: flex; align-items: center; gap: 6px; cursor: pointer;">
          <span style="width: 10px; height: 10px; background-color: ${color}; border-radius: 50%; display: inline-block;"></span>
          <span style="color: #0f172a;">${item.areaName}: ${displayLevel}</span>
        </div>
      `;

      const overlay = new window.kakao.maps.CustomOverlay({
        position: latLng,
        content: content,
        yAnchor: 1.3
      });

      overlay.setMap(mapInstanceRef.current);
      activeOverlaysRef.current.push(overlay);
    });
  }, [kakaoLoaded, congestionList, selectedFilter]);

  useEffect(() => {
    fetchAllCongestion();
  }, [fetchAllCongestion]);

  useEffect(() => {
    if (kakaoLoaded && congestionList.length > 0) {
      renderMapAndMarkers();
    }
  }, [kakaoLoaded, congestionList, renderMapAndMarkers]);

  const chartData = historyList.map((item) => ({
    time: new Date(item.updateTime).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
    popMax: item.populationMax
  }));

  const filters: { key: FilterType; label: string; color: string }[] = [
    { key: "ALL", label: "전체", color: "bg-slate-800 text-white" },
    { key: "GREEN", label: "여유", color: "bg-emerald-500 text-white" },
    { key: "YELLOW", label: "보통", color: "bg-amber-500 text-white" },
    { key: "ORANGE", label: "약간 혼잡", color: "bg-orange-500 text-white" },
    { key: "RED", label: "혼잡", color: "bg-rose-500 text-white" },
    { key: "UNKNOWN", label: "정보 없음", color: "bg-slate-400 text-white" }
  ];

  return (
    <main className="min-h-screen bg-slate-50 text-slate-900 p-6 sm:p-10">
      <Script
        src={`//dapi.kakao.com/v2/maps/sdk.js?appkey=${process.env.NEXT_PUBLIC_KAKAO_MAP_CLIENT_KEY}&autoload=false`}
        strategy="afterInteractive"
        onLoad={() => {
          if (window.kakao && window.kakao.maps) {
            window.kakao.maps.load(() => setKakaoLoaded(true));
          }
        }}
      />

      <div className="max-w-5xl mx-auto space-y-6">
        <header className="flex justify-between items-center bg-white p-6 border border-slate-100 rounded-2xl shadow-sm">
          <div>
            <h1 className="text-xl font-black tracking-tight">Seoul-Hub Realtime 관제 대시보드</h1>
            <p className="text-xs text-slate-500">전체 거점 다중 마커 실시간 표출 및 동적 검색 모니터링</p>
          </div>
          <button
            onClick={() => router.push("/questions")}
            className="px-4 py-2 text-xs font-bold text-blue-600 bg-blue-50 hover:bg-blue-100 rounded-lg transition-all cursor-pointer"
          >
            커뮤니티 가기
          </button>
        </header>

        {/* 검색 및 등급 필터 컨트롤러 영역 */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-4 border border-slate-100 rounded-2xl shadow-xs">
          <form onSubmit={handleSearch} className="flex gap-2 w-full sm:w-auto">
            <input
              type="text"
              placeholder="거점명 검색 (예: 강남역, 신림역)"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              className="px-3 py-1.5 text-xs outline-none bg-slate-50 border border-slate-200 rounded-lg w-full sm:w-64"
            />
            <button
              type="submit"
              className="px-4 py-1.5 text-xs font-bold text-white bg-slate-900 hover:bg-slate-800 rounded-lg transition-all cursor-pointer whitespace-nowrap"
            >
              검색
            </button>
          </form>

          {/* 혼잡도 등급 필터 버튼 그룹 */}
          <div className="flex flex-wrap gap-1.5">
            {filters.map((f) => {
              const isSelected = selectedFilter === f.key;
              return (
                <button
                  key={f.key}
                  onClick={() => setSelectedFilter(f.key)}
                  className={`px-3 py-1 rounded-full text-xs font-bold transition-all cursor-pointer border ${
                    isSelected
                      ? `${f.color} border-transparent shadow-xs`
                      : "bg-slate-50 text-slate-600 border-slate-200 hover:bg-slate-100"
                  }`}
                >
                  {f.label}
                </button>
              );
            })}
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 bg-white border border-slate-100 p-4 rounded-3xl shadow-sm h-[520px] flex flex-col">
            <div ref={mapRef} className="w-full flex-1 rounded-2xl bg-slate-100 border border-slate-200 overflow-hidden" />
          </div>

          <div className="bg-white border border-slate-100 p-6 rounded-3xl shadow-sm flex flex-col justify-between min-h-[520px]">
            <div className="space-y-4">
              <h2 className="text-sm font-bold text-slate-800 tracking-tight">🔴 선택 거점 상세 정보</h2>

              {loading && <p className="text-xs text-slate-400 animate-pulse font-medium">데이터 수송 중...</p>}
              {error && <p className="text-xs text-rose-500 font-bold bg-rose-50 p-3 rounded-lg">{error}</p>}

              {selectedArea && !loading && (
                <div className="space-y-3">
                  <div className="p-3.5 bg-slate-50 border border-slate-100 rounded-xl">
                    <span className="text-[10px] text-slate-400 font-black block mb-1">거점명 / 혼잡도</span>
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-black text-slate-800">{selectedArea.areaName}</span>
                      <span className="text-xs font-bold text-slate-500">
                        ({selectedArea.congestionLevel === "UNKNOWN" ? "정보 없음" : selectedArea.congestionLevel})
                      </span>
                      <span className={`w-2.5 h-2.5 rounded-full ${
                        selectedArea.congestionLevel === "GREEN" ? "bg-emerald-500" :
                        selectedArea.congestionLevel === "YELLOW" ? "bg-amber-400" :
                        selectedArea.congestionLevel === "ORANGE" ? "bg-orange-500" :
                        selectedArea.congestionLevel === "RED" ? "bg-rose-500" : "bg-slate-400"
                      }`} />
                    </div>
                  </div>

                  <div className="p-3.5 bg-slate-50 border border-slate-100 rounded-xl">
                    <span className="text-[10px] text-slate-400 font-black block mb-1">실시간 체류 인구</span>
                    <p className="text-xs font-bold text-slate-700">
                      {selectedArea.populationMin === 0 && selectedArea.populationMax === 0
                        ? "집계 불가 (데이터 미수집)"
                        : `${selectedArea.populationMin.toLocaleString()}명 ~ ${selectedArea.populationMax.toLocaleString()}명`}
                    </p>
                  </div>

                  <div className="p-3.5 bg-slate-50 border border-slate-100 rounded-xl space-y-2">
                    <span className="text-[10px] text-slate-400 font-black block">📈 인구 수 변화 추이 (1시간 평균 통계)</span>
                    {chartData.length >= 2 ? (
                      <div className="w-full h-28 pt-1">
                        <ResponsiveContainer width="100%" height="100%">
                          <LineChart data={chartData}>
                            <XAxis dataKey="time" tick={{ fontSize: 9 }} interval="preserveStartEnd" />
                            <YAxis hide domain={["auto", "auto"]} />
                            <Tooltip
                              contentStyle={{ backgroundColor: "#1e293b", borderRadius: "8px", border: "none" }}
                              labelStyle={{ color: "#94a3b8", fontSize: "10px" }}
                              itemStyle={{ color: "#38bdf8", fontSize: "11px", fontWeight: "bold" }}
                              formatter={(value: any) => [`${Number(value).toLocaleString()}명`, "평균 인구"]}
                            />
                            <Line type="monotone" dataKey="popMax" stroke="#2563eb" strokeWidth={2} dot={false} />
                          </LineChart>
                        </ResponsiveContainer>
                      </div>
                    ) : (
                      <p className="text-[11px] text-slate-400 font-medium py-2 text-center">시계열 트래픽 축적 중...</p>
                    )}
                  </div>

                  <div className="p-3.5 bg-slate-50 border border-slate-100 rounded-xl">
                    <span className="text-[10px] text-slate-400 font-black block mb-1">서울시 라이브 메시지</span>
                    <p className="text-xs text-slate-600 leading-relaxed font-semibold">
                      {selectedArea.congestionMessage}
                    </p>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}