"use client";

import { useEffect, useState, useRef, useCallback } from "react";
import Script from "next/script";
import { ResponsiveContainer, LineChart, Line, XAxis, YAxis, Tooltip } from "recharts";

import Link from "next/link";
import { apiClient, errorMessage, type CongestionResponse, type CollectionStatus } from "@/lib/api-client";

interface KakaoLatLng {
  getLat(): number;
  getLng(): number;
}

interface KakaoMapInstance {
  getBounds(): { contain(point: KakaoLatLng): boolean };
  getCenter(): KakaoLatLng;
  getLevel(): number;
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
        CustomOverlay: new (options: { position: KakaoLatLng; content: HTMLElement; yAnchor: number }) => KakaoCustomOverlay;
        event: { addListener(map: KakaoMapInstance, event: string, callback: () => void): void; removeListener(map: KakaoMapInstance, event: string, callback: () => void): void };
        load: (callback: () => void) => void;
      };
    };
  }
}

const levelLabels = { GREEN: "여유", YELLOW: "보통", ORANGE: "약간 혼잡", RED: "혼잡", UNKNOWN: "등급 미제공" };
function ageLabel(item: CongestionResponse) {
  if (item.observationAgeMinutes === null) return "미수집";
  const age=item.observationAgeMinutes;
  return `${age >= 60 ? `${Math.floor(age / 60)}시간 ${age % 60}분` : `${age}분`} 전 관측${item.freshness === "EXPIRED" ? " · 오래됨" : item.freshness === "DELAYED" ? " · 지연" : ""}`;
}

type FilterType = "ALL" | "GREEN" | "YELLOW" | "ORANGE" | "RED" | "UNKNOWN";

export default function DashboardPage() {
  const mapRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<KakaoMapInstance | null>(null);
  const activeOverlaysRef = useRef<KakaoCustomOverlay[]>([]);

  const dataRef = useRef<CongestionResponse[]>([]);
  const clientId = useRef("");
  const interestSequence = useRef(0);
  const [refreshError,setRefreshError] = useState<string | null>(null);
  const [collectionStatus, setCollectionStatus] = useState<CollectionStatus | null>(null);
  const [signedIn, setSignedIn] = useState(false);
  const [kakaoLoaded, setKakaoLoaded] = useState(false);
  const [congestionList, setCongestionList] = useState<CongestionResponse[]>([]);
  const [historyList, setHistoryList] = useState<CongestionResponse[]>([]);
  const [selectedFilter, setSelectedFilter] = useState<FilterType>("ALL");
  const [searchInput, setSearchInput] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [mapFailed, setMapFailed] = useState(false);
  const mapKey = process.env.NEXT_PUBLIC_KAKAO_MAP_CLIENT_KEY;
  const [selectedName, setSelectedName] = useState("강남역");
  const selectedArea = congestionList.find(item => item.areaName === selectedName) ?? null;

  useEffect(() => {
    let active = true;
    const checkSession = () => { if (document.visibilityState === "visible") void apiClient.me().then(() => { if (active) setSignedIn(true); }).catch(() => { if (active) setSignedIn(false); }); };
    checkSession();
    window.addEventListener("focus", checkSession);
    document.addEventListener("visibilitychange", checkSession);
    return () => { active = false; window.removeEventListener("focus", checkSession); document.removeEventListener("visibilitychange", checkSession); };
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    let pending = false;
    const refresh = async () => {
      if (pending || document.visibilityState !== "visible") return;
      pending = true;
      try {
        const [data,status] = await Promise.all([apiClient.getAllCongestion(controller.signal),apiClient.getCollectionStatus(controller.signal)]);
        if (!controller.signal.aborted) setCollectionStatus(status);
        if (!controller.signal.aborted) { dataRef.current=data; setCongestionList(data); setError(null); }
      } catch (err) {
        if (!controller.signal.aborted) setError(errorMessage(err));
      } finally { pending = false; if (!controller.signal.aborted) setLoading(false); }
    };
    void refresh();
    const timer = setInterval(refresh, 5000);
    document.addEventListener("visibilitychange",refresh);
    return () => { controller.abort(); clearInterval(timer); document.removeEventListener("visibilitychange",refresh); };
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    apiClient.getHistory(selectedName, controller.signal).then(data => {
      if (!controller.signal.aborted) setHistoryList(data);
    }).catch(() => { if (!controller.signal.aborted) setHistoryList([]); });
    return () => controller.abort();
  }, [selectedName, congestionList]);

  const selectArea = (name: string) => {
    setSelectedName(name);
    setHistoryList([]);
    const coords = congestionList.find(item => item.areaName === name);
    if (coords && mapInstanceRef.current && window.kakao) {
      mapInstanceRef.current.setCenter(new window.kakao.maps.LatLng(coords.latitude, coords.longitude));
      mapInstanceRef.current.setLevel(5);
    }
  };

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!searchInput.trim() || loading) return;
    try {
      setLoading(true); setError(null);
      const match=congestionList.find(item => item.areaName.includes(searchInput.trim()) || item.areaCode === searchInput.trim());
      if (!match) throw new Error("공식 거점 목록에서 검색 결과를 찾을 수 없습니다.");
      const data = await apiClient.getCongestion(match.areaCode);
      setCongestionList(prev => [...prev.filter(item => item.areaName !== data.areaName), data]);
      selectArea(data.areaName);
      setSelectedFilter("ALL");
    } catch (err) { setError(errorMessage(err)); }
    finally { setLoading(false); }
  };

  const renderMapAndMarkers = useCallback(() => {
    if (!kakaoLoaded || !mapRef.current || !window.kakao) return;

    const initialLatLng = new window.kakao.maps.LatLng(37.5665,126.978);

    if (!mapInstanceRef.current) {
      mapInstanceRef.current = new window.kakao.maps.Map(mapRef.current, {
        center: initialLatLng,
        level: 9
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
      const coords = item;
      if (!coords) return;

      const latLng = new window.kakao.maps.LatLng(coords.latitude, coords.longitude);

      let color = "#10b981";
      if (item.congestionLevel === "YELLOW") color = "#f59e0b";
      if (item.congestionLevel === "ORANGE") color = "#ea580c";
      if (item.congestionLevel === "RED") color = "#ef4444";
      if (item.congestionLevel === "UNKNOWN") color = "#94a3b8";

      const displayLevel = item.updateTime ? levelLabels[item.congestionLevel] : "미수집";

      const content = document.createElement("button");
      content.type = "button";
      content.textContent = "●";
      content.title = `${item.areaName}: ${displayLevel} · ${ageLabel(item)}`;
      content.setAttribute("aria-label", content.title);
      content.style.cssText = `padding:2px 6px;background:white;border:2px solid ${color};border-radius:20px;font-weight:bold;color:${color};cursor:pointer`;
      if(item.stale) content.style.borderStyle="dashed";
      if(item.freshness === "EXPIRED") content.style.opacity="0.5";
      content.onclick = () => { setSelectedName(item.areaName); setHistoryList([]); };

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
    renderMapAndMarkers();
    return () => { activeOverlaysRef.current.forEach(overlay => overlay.setMap(null)); };
  }, [renderMapAndMarkers]);

  useEffect(() => {
    const map=mapInstanceRef.current;
    if (!congestionList.length) return;
    if (!clientId.current) clientId.current=crypto.randomUUID();
    const controller=new AbortController();
    let debounce: ReturnType<typeof setTimeout> | undefined;
    const submit=() => {
      const hidden=document.visibilityState !== "visible";
      const bounds=map?.getBounds();
      const center=map?.getCenter();
      const visible=(!hidden && map && map.getLevel()<=7 && bounds && center)
        ? dataRef.current.filter(item => (selectedFilter==="ALL" || item.congestionLevel===selectedFilter) && bounds.contain(new window.kakao.maps.LatLng(item.latitude,item.longitude)))
          .sort((a,b) => ((a.latitude-center.getLat())**2+(a.longitude-center.getLng())**2)-((b.latitude-center.getLat())**2+(b.longitude-center.getLng())**2))
          .slice(0,12).map(item => item.areaCode) : [];
      void apiClient.refreshVisible({clientId:clientId.current, sequence:++interestSequence.current, visible, selected:hidden ? null : selectedName},controller.signal)
        .then(() => { if(!controller.signal.aborted) setRefreshError(null); })
        .catch(err => { if(!controller.signal.aborted) setRefreshError(errorMessage(err)); });
    };
    const idle=() => { clearTimeout(debounce); debounce=setTimeout(submit,500); };
    if(map) window.kakao.maps.event.addListener(map,"idle",idle);
    document.addEventListener("visibilitychange",submit);
    submit();
    const timer=setInterval(submit,15000);
    return () => { controller.abort(); clearTimeout(debounce); clearInterval(timer); if(map) window.kakao.maps.event.removeListener(map,"idle",idle); document.removeEventListener("visibilitychange",submit); };
  },[kakaoLoaded,congestionList.length,selectedName,selectedFilter]);

  const chartData = historyList.map((item) => ({
    time: new Date(item.updateTime || 0).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
    popMax: item.populationMax
  }));

  const filters: { key: FilterType; label: string; color: string }[] = [
    { key: "ALL", label: "전체", color: "bg-slate-800 text-white" },
    { key: "GREEN", label: "여유", color: "bg-emerald-500 text-white" },
    { key: "YELLOW", label: "보통", color: "bg-amber-500 text-white" },
    { key: "ORANGE", label: "약간 혼잡", color: "bg-orange-500 text-white" },
    { key: "RED", label: "혼잡", color: "bg-rose-500 text-white" },
    { key: "UNKNOWN", label: "미수집·등급 없음", color: "bg-slate-400 text-white" }
  ];

  return (
    <main className="min-h-screen bg-slate-50 text-slate-900 p-4 sm:p-8">
      {mapKey && <Script
        src={`https://dapi.kakao.com/v2/maps/sdk.js?appkey=${process.env.NEXT_PUBLIC_KAKAO_MAP_CLIENT_KEY}&autoload=false`}
        strategy="afterInteractive"
        onError={() => setMapFailed(true)}
        onReady={() => {
          if (window.kakao && window.kakao.maps) {
            window.kakao.maps.load(() => setKakaoLoaded(true));
          }
        }}
      />}

      <div className="max-w-7xl mx-auto space-y-6">
        <header className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <Link href="/dashboard" className="text-xl font-black tracking-tight text-blue-700">Seoul Hub<span className="mt-1 block text-xs font-medium text-slate-500">서울 실시간 인구 지도</span></Link>
          <nav aria-label="주요 메뉴" className="flex flex-wrap items-center gap-2 text-sm font-semibold">
            <Link href="/questions" className="rounded-lg px-3 py-2 hover:bg-slate-100">커뮤니티</Link>
            {signedIn ? <>
              <Link href="/account" className="rounded-lg px-3 py-2 hover:bg-slate-100">계정 관리</Link>
              <button className="rounded-lg border border-slate-200 px-3 py-2 hover:bg-slate-100" onClick={async () => { try { await apiClient.logout(); setSignedIn(false); } catch (err) { setError(errorMessage(err)); } }}>로그아웃</button>
            </> : <>
              <Link href="/login" className="rounded-lg px-3 py-2 hover:bg-slate-100">로그인</Link>
              <Link href="/signup" className="rounded-lg bg-blue-700 px-4 py-2 text-white hover:bg-blue-800">회원가입</Link>
            </>}
          </nav>
        </header>
        <section className="space-y-3" aria-labelledby="dashboard-title">
          <h1 id="dashboard-title" className="text-2xl font-bold sm:text-3xl">서울, 지금 어디로 갈까요?</h1>
          <p className="text-sm leading-relaxed text-slate-600">지도를 확대하거나 거점을 검색해 인구와 혼잡도를 확인하세요. 로그인 없이도 이용할 수 있습니다.</p>
          <div className="grid grid-cols-3 gap-3 text-sm">
            <div className="rounded-xl border border-slate-200 bg-white p-4"><span className="text-slate-500">공식 거점</span><strong className="mt-1 block text-xl">{congestionList.length || "—"}<span className="ml-1 text-sm font-normal">곳</span></strong></div>
            <div className="rounded-xl border border-slate-200 bg-white p-4"><span className="text-slate-500">관측 보유</span><strong className="mt-1 block text-xl">{congestionList.filter(item => item.updateTime).length}<span className="ml-1 text-sm font-normal">곳</span></strong></div>
            <div className="rounded-xl border border-slate-200 bg-white p-4"><span className="text-slate-500">선택 거점</span><strong className="mt-1 block break-words text-base">{selectedArea?.areaName || selectedName}</strong></div>
          </div>
        </section>

        {/* 검색 및 등급 필터 컨트롤러 영역 */}
        <div className="flex flex-col xl:flex-row justify-between items-start xl:items-center gap-4 bg-white p-4 border border-slate-100 rounded-2xl shadow-xs">
          <form onSubmit={handleSearch} className="flex gap-2 w-full sm:w-auto">
            <input
              type="text"
              aria-label="거점명 검색"
              placeholder="거점명 검색 (예: 강남역, 신림역)"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              className="px-3 py-3 text-sm bg-slate-50 border border-slate-200 rounded-lg w-full sm:w-64"
            />
            <button
              type="submit" disabled={loading}
              className="px-4 py-3 text-sm font-bold text-white bg-slate-900 hover:bg-slate-800 rounded-lg transition-all cursor-pointer whitespace-nowrap"
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
                  aria-pressed={isSelected}
                  onClick={() => setSelectedFilter(f.key)}
                  className={`px-3 py-2 rounded-full text-sm font-bold transition-all cursor-pointer border ${
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

        <p className="text-xs text-slate-500">출처: 서울 열린데이터광장 · 지정 거점의 추정 인구이며 서울 전역을 연속 측정한 데이터가 아닙니다. 색상은 관측 당시 혼잡도입니다. 점선은 45분 이상, 흐린 마커는 2시간 이상 지난 관측이며 회색은 미수집·등급 미제공입니다.</p>
        {collectionStatus && <p className="text-xs text-slate-600" role="status">검색·선택한 거점 우선 갱신 · 확대된 지도 안 최대 12곳 갱신 · 서버 확인 결과는 5분간 공유합니다. 오늘 API 사용 {collectionStatus.usedToday}/{collectionStatus.dailyBudget}회. {collectionStatus.usedToday >= collectionStatus.dailyBudget ? "오늘 수집 한도에 도달해 저장된 관측을 표시합니다." : "서울 전체 보기에서는 선택한 거점만 갱신합니다."}</p>}
        {refreshError && <p role="alert" className="text-xs text-amber-800">최신 확인 요청 실패: {refreshError} 저장된 관측을 표시합니다.</p>}
        <button className="text-sm text-blue-700" onClick={() => { mapInstanceRef.current?.setCenter(new window.kakao.maps.LatLng(37.5665,126.978)); mapInstanceRef.current?.setLevel(9); }}>서울 전체 보기</button>
        <details className="rounded-xl border border-slate-200 bg-white p-4"><summary className="cursor-pointer text-sm font-semibold">거점 목록에서 선택하기</summary>
        <section aria-label="지역 목록" className="flex max-h-48 overflow-y-auto flex-wrap gap-2">
          {congestionList.filter(item => selectedFilter === "ALL" || item.congestionLevel === selectedFilter).map(item => (
            <button key={item.areaName} onClick={() => selectArea(item.areaName)} aria-pressed={selectedName === item.areaName}
              className={`rounded-xl border px-3 py-2 text-xs ${selectedName === item.areaName ? "border-blue-600 bg-blue-50" : "border-slate-200 bg-white"}`}>
              <strong>{item.areaName}</strong> · {item.updateTime ? levelLabels[item.congestionLevel] : "미수집"}<span className="block text-[10px] text-slate-500">{ageLabel(item)}</span>
            </button>
          ))}
          {!loading && !congestionList.length && <p className="text-sm text-slate-500">아직 수집된 데이터가 없습니다. 지역명을 검색해 주세요.</p>}
          {!!congestionList.length && !congestionList.some(item => selectedFilter === "ALL" || item.congestionLevel === selectedFilter) && <p>해당 혼잡도의 지역이 없습니다.</p>}
        </section></details>
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 bg-white border border-slate-100 p-4 rounded-3xl shadow-sm h-[420px] sm:h-[600px] flex flex-col">
            {(!mapKey || mapFailed) && <p role="status" className="mb-3 rounded-xl bg-blue-50 p-4 text-sm text-blue-900">지도를 사용할 수 없습니다. 위 거점 목록에서 혼잡도를 확인해 주세요.</p>}
            <div ref={mapRef} className="w-full flex-1 rounded-2xl bg-slate-100 border border-slate-200 overflow-hidden" />
          </div>

          <div className="bg-white border border-slate-100 p-6 rounded-3xl shadow-sm flex flex-col justify-between min-h-[520px]">
            <div className="space-y-4">
              <h2 className="text-sm font-bold text-slate-800 tracking-tight">선택 거점 상세 정보</h2>

              {loading && <p className="text-xs text-slate-400 animate-pulse font-medium">데이터를 불러오는 중...</p>}
              {error && <p className="text-xs text-rose-500 font-bold bg-rose-50 p-3 rounded-lg">{error}</p>}

              {selectedArea && !loading && (
                <div className="space-y-3">
                  {selectedArea.stale && <p role="status" className="rounded-lg bg-amber-50 p-3 text-xs text-amber-900">{selectedArea.freshness === "EXPIRED" ? "오래된 관측입니다. 현재 상황으로 해석하지 마세요." : "관측 시각이 오래되었습니다. 원본 갱신 지연 또는 제한된 수집 주기의 영향일 수 있습니다."} 아래 값은 마지막 관측 기준입니다.</p>}
                  <div className="p-3.5 bg-slate-50 border border-slate-100 rounded-xl">
                    <span className="text-xs text-slate-500 font-black block mb-1">거점명 / 관측 당시 혼잡도</span>
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-black text-slate-800">{selectedArea.areaName}</span>
                      <span className="text-xs font-bold text-slate-500">
                        ({filters.find(f => f.key === selectedArea.congestionLevel)?.label})
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
                    <span className="text-xs text-slate-500 font-black block mb-1">마지막 관측 인구</span>
                    <p className="text-lg font-bold text-slate-800">
                      {selectedArea.populationMin === 0 && selectedArea.populationMax === 0
                        ? "집계 불가 (데이터 미수집)"
                        : `${selectedArea.populationMin.toLocaleString()}명 ~ ${selectedArea.populationMax.toLocaleString()}명`}
                    </p>
                  </div>

                  <div className="p-3.5 bg-slate-50 border border-slate-100 rounded-xl space-y-2">
                    <span className="text-xs text-slate-500 font-black block">📈 수집된 인구 변화 추이</span>
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
                              formatter={(value) => [`${Number(value).toLocaleString()}명`, "관측 인구 상한"]}
                            />
                            <Line type="monotone" dataKey="popMax" stroke="#2563eb" strokeWidth={2} dot={false} />
                          </LineChart>
                        </ResponsiveContainer>
                      </div>
                    ) : (
                      <p className="text-[11px] text-slate-400 font-medium py-2 text-center">추이를 표시할 데이터가 아직 부족합니다.</p>
                    )}
                  </div>

                  <p className="text-xs font-semibold text-slate-600">{ageLabel(selectedArea)}</p>
                  <p className="text-xs text-slate-500">{!selectedArea.updateTime ? "관측 시각: 정보 없음" : `관측 시각: ${new Date(selectedArea.updateTime || 0).toLocaleString("ko-KR")}`}</p>
                  <p className="text-xs text-slate-500">마지막 API 확인: {selectedArea.lastCheckedAt ? new Date(selectedArea.lastCheckedAt).toLocaleString("ko-KR") : "아직 확인 기록 없음"}</p>
                  {selectedArea.collectedAt && <p className="text-xs text-slate-500">수집 시각: {new Date(selectedArea.collectedAt).toLocaleString("ko-KR")}</p>}
                  <div className="p-3.5 bg-slate-50 border border-slate-100 rounded-xl">
                    <span className="text-xs text-slate-500 font-black block mb-1">서울시 관측 안내</span>
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