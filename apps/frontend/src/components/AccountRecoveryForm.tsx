"use client";
import Link from "next/link";
import { useState, useSyncExternalStore } from "react";
import { apiClient, errorMessage } from "@/lib/api-client";

function subscribe(listener: () => void) {
  window.addEventListener("hashchange", listener);
  return () => window.removeEventListener("hashchange", listener);
}
const readToken = () => new URLSearchParams(window.location.hash.slice(1)).get("token") || "";
const serverToken = () => "";

export default function AccountRecoveryForm({ mode }: { mode: "verify" | "forgot" | "reset" }) {
  const token = useSyncExternalStore(subscribe, readToken, serverToken);
  const [email, setEmail] = useState(""); const [name, setName] = useState("");
  const [password, setPassword] = useState(""); const [confirmation, setConfirmation] = useState("");
  const [busy, setBusy] = useState(false); const [done, setDone] = useState(false);
  const [notice, setNotice] = useState(""); const [error, setError] = useState("");
  const isLink = mode !== "forgot" && token.length > 0;
  const title = mode === "verify" ? "이메일 인증" : "비밀번호 재설정";
  const input = "w-full rounded-lg border border-slate-300 bg-slate-50 px-4 py-3 text-sm";
  async function submit(event: React.FormEvent) {
    event.preventDefault(); if (busy) return; setError(""); setNotice("");
    if (isLink && password !== confirmation) { setError("비밀번호 확인이 일치하지 않습니다."); return; }
    if (isLink && !/^[A-Za-z0-9_-]{43}$/.test(token)) { setError("올바르지 않은 링크입니다. 새 메일을 요청해 주세요."); return; }
    setBusy(true);
    try {
      if (isLink) {
        if (mode === "verify") await apiClient.verifyEmail(token, name.trim(), password);
        else await apiClient.resetPassword(token, password);
        window.history.replaceState(null, "", window.location.pathname);
        setPassword(""); setConfirmation(""); setDone(true);
      } else {
        const normalized = email.trim().toLowerCase();
        const result = mode === "verify" ? await apiClient.requestVerification(normalized) : await apiClient.forgotPassword(normalized);
        setNotice(result.message);
      }
    } catch (err) { setError(errorMessage(err)); }
    finally { setBusy(false); }
  }
  return <main className="flex min-h-screen items-center justify-center bg-slate-50 p-5"><section className="w-full max-w-md space-y-5 rounded-2xl border border-slate-100 bg-white p-8 shadow-lg">
    <Link href="/dashboard" className="text-sm font-bold text-blue-700">Seoul Hub</Link><h1 className="text-2xl font-bold">{title}</h1>
    {done ? <><p role="status" className="rounded-lg bg-emerald-50 p-3 text-sm text-emerald-800">{mode === "verify" ? "이메일 인증과 계정 설정이 완료되었습니다." : "비밀번호가 변경되었고 모든 기기에서 로그아웃되었습니다."} 새 비밀번호로 로그인해 주세요.</p><Link className="block rounded-lg bg-slate-900 p-3 text-center font-bold text-white" href="/login">로그인하기</Link></> : <>
      <p className="text-sm text-slate-600">{isLink ? mode === "verify" ? "메일 주소의 소유자만 사용할 닉네임과 비밀번호를 설정해 주세요. 완료 버튼을 눌러야 인증됩니다." : "새 비밀번호를 설정하면 모든 기기의 기존 로그인 세션이 종료됩니다." : mode === "verify" ? "가입 시 사용한 이메일로 새 인증 링크를 요청하세요. 아직 가입하지 않았다면 회원가입부터 진행해 주세요." : "가입한 이메일을 입력하세요. 인증된 계정에 재설정 링크를 보냅니다."}</p>
      <form onSubmit={submit} className="space-y-4">
        {!isLink && <div><label htmlFor="email" className="mb-1 block text-sm font-semibold">이메일</label><input id="email" className={input} type="email" autoComplete="email" maxLength={254} required disabled={busy} value={email} onChange={e => setEmail(e.target.value)} /></div>}
        {isLink && mode === "verify" && <div><label htmlFor="name" className="mb-1 block text-sm font-semibold">닉네임</label><input id="name" className={input} minLength={2} maxLength={40} autoComplete="nickname" required disabled={busy} value={name} onChange={e => setName(e.target.value)} /><p className="mt-1 text-xs text-slate-500">게시판에 공개되는 이름입니다. 2~40자로 입력해 주세요.</p></div>}
        {isLink && <><div><label htmlFor="password" className="mb-1 block text-sm font-semibold">새 비밀번호</label><input id="password" className={input} type="password" autoComplete="new-password" minLength={12} maxLength={64} required disabled={busy} value={password} onChange={e => setPassword(e.target.value)} /><p className="mt-1 text-xs text-slate-500">영문, 숫자, 공백, 기호를 사용한 12~64자.</p></div><div><label htmlFor="confirm" className="mb-1 block text-sm font-semibold">비밀번호 확인</label><input id="confirm" className={input} type="password" autoComplete="new-password" maxLength={64} required disabled={busy} value={confirmation} onChange={e => setConfirmation(e.target.value)} /></div></>}
        {error && <p role="alert" className="rounded-lg bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
        {notice && <p role="status" className="rounded-lg bg-blue-50 p-3 text-sm text-blue-800">{notice}</p>}
        <button disabled={busy} className="w-full rounded-lg bg-slate-900 py-3 font-bold text-white disabled:opacity-50">{busy ? "처리 중…" : isLink ? mode === "verify" ? "인증하고 가입 완료" : "비밀번호 변경" : "메일 요청하기"}</button>
      </form>
      <div className="flex flex-wrap gap-3 text-sm text-blue-700"><Link href="/login">로그인</Link><Link href="/signup">회원가입</Link>{isLink && <Link href={mode === "verify" ? "/verify-email" : "/forgot-password"}>새 링크 요청</Link>}</div>
    </>}
  </section></main>;
}
