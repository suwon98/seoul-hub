"use client";
import Link from "next/link";
import { useEffect, useState } from "react";
import { apiClient, ApiError, errorMessage } from "@/lib/api-client";
export default function AccountPage() {
  const [email,setEmail]=useState(""); const [password,setPassword]=useState("");
  const [confirmation,setConfirmation]=useState(""); const [error,setError]=useState("");
  const [busy,setBusy]=useState(false); const [loaded,setLoaded]=useState(false); const [done,setDone]=useState(false);
  useEffect(() => { let active=true; apiClient.me().then(user => { if(active) setEmail(user.email); }).catch(err => {
    if(active) setError(err instanceof ApiError && err.status === 401 ? "로그인 후 계정을 관리할 수 있습니다." : errorMessage(err));
  }).finally(() => { if(active) setLoaded(true); }); return () => {active=false;}; },[]);
  async function remove(event: React.FormEvent) {
    event.preventDefault(); if(busy) return; setBusy(true); setError("");
    try { await apiClient.deleteAccount(password,confirmation); localStorage.removeItem("accessToken"); setPassword("");setEmail("");setDone(true); }
    catch(err) {setError(errorMessage(err));} finally {setBusy(false);}
  }
  const input="w-full rounded-lg border border-slate-300 bg-slate-50 px-4 py-3 text-sm";
  return <main className="flex min-h-screen items-center justify-center bg-slate-50 p-5"><section className="w-full max-w-lg space-y-5 rounded-2xl border border-slate-100 bg-white p-8 shadow-lg">
    <Link href="/dashboard" className="text-sm font-bold text-blue-700">Seoul Hub</Link><h1 className="text-2xl font-bold">계정 관리</h1>
    {!loaded && <p role="status">계정을 확인하고 있습니다.</p>}
    {error && <p role="alert" className="rounded-lg bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
    {done ? <><p role="status" className="rounded-lg bg-emerald-50 p-3 text-sm text-emerald-800">회원 탈퇴가 완료되었습니다. 계정과 작성한 콘텐츠를 삭제하고 모든 로그인 세션을 종료했습니다.</p><Link className="text-blue-700" href="/dashboard">혼잡도 둘러보기</Link></> : email ? <>
      <p className="break-all text-sm text-slate-600">로그인 계정: {email}</p><Link href="/forgot-password" className="block text-sm text-blue-700">비밀번호 재설정 메일 받기</Link>
      <h2 className="border-t pt-5 text-lg font-bold text-rose-700">회원 탈퇴</h2><p id="deletion-details" className="text-sm leading-6 text-slate-600">탈퇴하면 계정, 작성한 질문과 답변, 내 질문에 달린 다른 사람의 답변이 삭제됩니다. 다른 사람의 질문은 유지됩니다. 모든 기기에서 로그아웃되며 이 작업은 되돌릴 수 없습니다.</p>
      <form onSubmit={remove} aria-describedby="deletion-details" className="space-y-4"><div><label htmlFor="password" className="mb-1 block text-sm font-semibold">현재 비밀번호</label><input id="password" className={input} type="password" autoComplete="current-password" maxLength={64} required disabled={busy} value={password} onChange={e=>setPassword(e.target.value)} /></div><div><label htmlFor="confirmation" className="mb-1 block text-sm font-semibold">확인을 위해 ‘회원 탈퇴’를 입력하세요</label><input id="confirmation" className={input} autoComplete="off" maxLength={20} required disabled={busy} value={confirmation} onChange={e=>setConfirmation(e.target.value)} /></div><button disabled={busy || confirmation!=="회원 탈퇴"} className="w-full rounded-lg bg-rose-700 py-3 font-bold text-white disabled:opacity-50">{busy ? "처리 중…" : "계정과 콘텐츠 영구 삭제"}</button></form>
    </> : loaded && <Link href="/login" className="text-blue-700">로그인하기</Link>}
  </section></main>;
}
