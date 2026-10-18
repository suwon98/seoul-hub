import { NextRequest, NextResponse } from "next/server";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";
export const maxDuration = 60;
const cookieName = "seoul_session";
const upstream = process.env.BACKEND_URL || "http://127.0.0.1:8080";
const allowed = /^(users\/(login|signup|me|logout|request-verification|verify-email|forgot-password|reset-password|delete-account)|congestion\/(all|[^/]+(?:\/history)?)|questions(?:\/[1-9][0-9]*(?:\/answers)?)?)$/;

async function proxy(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  const { path } = await context.params;
  const route = path.join("/");
  if (!allowed.test(route) || path.some(segment => segment === "." || segment === ".." || segment.includes("/"))) return NextResponse.json({ message: "지원하지 않는 경로입니다." }, { status: 404 });
  const writing = request.method === "POST";
  const publicOrigin = process.env.APP_ORIGIN || request.nextUrl.origin;
  if (writing && (request.headers.get("origin") !== publicOrigin || request.headers.get("x-seoulhub-request") !== "1")) {
    return NextResponse.json({ message: "허용되지 않은 요청 출처입니다." }, { status: 403 });
  }
  const session = request.cookies.get(cookieName)?.value;
  if (route === "users/logout" && !session && writing) return NextResponse.json({ success: true });
  let body: string | undefined;
  if (writing) {
    if (!request.headers.get("content-type")?.startsWith("application/json")) return NextResponse.json({ message: "JSON 요청이 필요합니다." }, { status: 415 });
    // Read at most 32 KiB, including chunked requests without Content-Length.
    const reader = request.body?.getReader();
    const chunks: Uint8Array[] = []; let size = 0;
    if (reader) {
      while (true) {
        const chunk = await reader.read(); if (chunk.done) break;
        size += chunk.value.byteLength;
        if (size > 32768) { await reader.cancel(); return NextResponse.json({ message: "요청 내용이 너무 큽니다." }, { status: 413 }); }
        chunks.push(chunk.value);
      }
    }
    body = Buffer.concat(chunks).toString("utf8");
  }
  try {
    const url = new URL(`/api/v1/${path.map(encodeURIComponent).join("/")}`, upstream);
    const response = await fetch(url, {
      method: request.method, cache: "no-store", redirect: "error",
      headers: { "Content-Type": "application/json", ...(session ? { Authorization: `Bearer ${session}` } : {}),
        ...(process.env.API_PROXY_SECRET ? { "X-Internal-API-Key": process.env.API_PROXY_SECRET } : {}) },
      body, signal: AbortSignal.timeout(50000),
    });
    const data = await response.json().catch(() => ({ message: "서버가 시작 중이거나 응답을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요." }));
    const isLogin = route === "users/login" && writing && response.ok;
    const result = NextResponse.json(isLogin ? { success: true } : data, { status: response.status });
    result.headers.set("Cache-Control", "no-store, private");
    if (response.headers.has("Retry-After")) result.headers.set("Retry-After", response.headers.get("Retry-After")!);
    const secure = new URL(publicOrigin).protocol === "https:";
    const options = { httpOnly: true, secure, sameSite: "lax" as const, path: "/", maxAge: 3600 };
    if (isLogin) {
      if (typeof data.accessToken !== "string") return NextResponse.json({ message: "인증 응답이 올바르지 않습니다." }, { status: 502 });
      result.cookies.set(cookieName, data.accessToken, options);
    }
    if ((["users/logout", "users/delete-account", "users/reset-password", "users/verify-email"].includes(route) && response.ok) || response.status === 401) result.cookies.set(cookieName, "", { ...options, maxAge: 0 });
    return result;
  } catch {
    return NextResponse.json({ message: "서버 연결이 지연되고 있습니다. 무료 서버가 시작 중일 수 있으니 잠시 후 다시 시도해 주세요." }, { status: 503 });
  }
}
export const GET = proxy;
export const POST = proxy;
