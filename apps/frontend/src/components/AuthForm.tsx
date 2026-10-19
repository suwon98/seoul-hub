"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { apiClient, errorMessage } from "@/lib/api-client";

export default function AuthForm({ signup = false, registered = false }: { signup?: boolean; registered?: boolean }) {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  async function submit(event: React.FormEvent) {
    event.preventDefault(); if (busy) return;
    setError(""); setNotice(""); setBusy(true);
    try {
      const normalized = email.trim().toLowerCase();
      if (signup) {
        const response = await apiClient.signUp({ email: normalized });
        setNotice(response.message);
      } else {
        await apiClient.login({ email: normalized, password });
        localStorage.removeItem("accessToken"); router.replace("/dashboard"); router.refresh();
      }
    } catch (err) { setError(errorMessage(err)); }
    finally { setBusy(false); }
  }
  const inputClass = "w-full rounded-lg border border-slate-300 bg-slate-50 px-4 py-3 text-sm";
  return <main className="flex min-h-screen items-center justify-center bg-slate-50 p-5">
    <section className="w-full max-w-md space-y-6 rounded-2xl border border-slate-100 bg-white p-8 shadow-lg">
      <header><Link href="/dashboard" className="text-sm font-bold text-blue-700">Seoul Hub</Link><h1 className="mt-3 text-2xl font-bold">{signup ? "회원가입" : "로그인"}</h1><p className="mt-2 text-sm text-slate-500">{signup ? "이메일을 인증한 뒤 닉네임과 비밀번호를 설정합니다." : "서울의 방문 경험을 함께 나누세요."}</p></header>
      {registered && !signup && <p role="status" className="rounded-lg bg-emerald-50 p-3 text-sm text-emerald-800">이메일 인증과 계정 설정이 완료되었습니다. 로그인해 주세요.</p>}
      {!signup && <p className="rounded-lg bg-slate-50 p-3 text-sm text-slate-600">로그인하면 다른 기기의 기존 로그인은 종료됩니다.</p>}
      <form onSubmit={submit} className="space-y-4">
        <div><label htmlFor="email" className="mb-1 block text-sm font-semibold">이메일</label><input className={inputClass} id="email" type="email" autoComplete="email" maxLength={254} required disabled={busy} value={email} onChange={e => setEmail(e.target.value)} /></div>
        {!signup && <div><label htmlFor="password" className="mb-1 block text-sm font-semibold">비밀번호</label><input className={inputClass} id="password" type="password" autoComplete="current-password" maxLength={64} required disabled={busy} value={password} onChange={e => setPassword(e.target.value)} /></div>}
        {error && <p role="alert" className="rounded-lg bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
        {notice && <p role="status" className="rounded-lg bg-blue-50 p-3 text-sm text-blue-800">{notice}</p>}
        <button className="w-full rounded-lg bg-slate-900 py-3 font-bold text-white disabled:opacity-50" disabled={busy}>{busy ? "처리 중…" : signup ? "인증 메일 받기" : "로그인"}</button>
      </form>
      <div className="flex flex-wrap gap-3 text-sm text-blue-700"><Link href={signup ? "/login" : "/signup"}>{signup ? "이미 계정이 있나요? 로그인" : "계정 만들기"}</Link><Link href="/verify-email">인증 메일 다시 받기</Link>{!signup && <Link href="/forgot-password">비밀번호 찾기</Link>}<Link href="/dashboard">혼잡도 둘러보기</Link></div>
    </section>
  </main>;
}
