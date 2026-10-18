import assert from "node:assert/strict";
import { readdir, readFile } from "node:fs/promises";
if (process.env.SMOKE_FILE_MAIL_CONFIRMED !== "true") throw new Error("Start an isolated backend with account.mail.mode=file, then set SMOKE_FILE_MAIL_CONFIRMED=true. Never run against Brevo mode.");
const origin = process.env.SMOKE_ORIGIN || "http://localhost:3000";
if (!["localhost", "127.0.0.1"].includes(new URL(origin).hostname)) throw new Error("This test creates and deletes its own generated account; use localhost only.");
const email = `smoke-${Date.now()}@example.com`;
const password = "local smoke passphrase 123!";
const replacement = "replacement smoke passphrase 456!";
async function call(path, body, cookie, headers = {}) {
  return fetch(`${origin}/api/v1${path}`, { method: body === undefined ? "GET" : "POST", headers: {
    ...(body === undefined ? {} : { "Content-Type": "application/json", Origin: origin, "X-SeoulHub-Request": "1" }),
    ...(cookie ? { Cookie: cookie } : {}), ...headers }, ...(body === undefined ? {} : { body: JSON.stringify(body) }) });
}
async function tokenFor(path) {
  const directory = new URL("../apps/backend/data/mail/", import.meta.url);
  const messages = await Promise.all((await readdir(directory)).filter(name => name.endsWith(".json")).map(async name => JSON.parse(await readFile(new URL(name,directory),"utf8"))));
  const latest = messages.filter(message => message.to === email && message.body.includes(path + "#token=")).sort((a,b) => b.createdAt.localeCompare(a.createdAt))[0];
  assert.ok(latest, "Expected local file mailbox; do not run this test with a live mail provider.");
  return latest.body.match(/#token=([A-Za-z0-9_-]{43})/)[1];
}
async function login(pass) {
  const response = await call("/users/login",{email,password:pass}); assert.equal(response.status,200);
  const payload=await response.json(); assert.equal(payload.success,true); assert.equal(payload.accessToken,undefined);
  const cookie=response.headers.get("set-cookie"); assert.match(cookie,/HttpOnly/i); assert.match(cookie,/SameSite=lax/i);
  return cookie.split(";")[0];
}
assert.equal((await call("/users/signup", {email},undefined,{Origin:"https://evil.example"})).status,403);
assert.equal((await call("/users/signup", {email},undefined,{"X-SeoulHub-Request":""})).status,403);
const signup=await call("/users/signup",{email});assert.equal(signup.status,202);
const receipt=await signup.json();assert.equal(receipt.id,undefined);
assert.equal((await call("/users/signup",{email:email.toUpperCase()})).status,202);
assert.equal((await call("/users/login",{email,password})).status,401);
const verifyToken=await tokenFor("/verify-email");
assert.equal((await call("/users/verify-email",{token:verifyToken,name:"검증계정",password})).status,200);
assert.equal((await call("/users/verify-email",{token:verifyToken,name:"검증계정",password})).status,400);
assert.equal((await call("/users/login", {email,password:"incorrect password"})).status,401);
const cookie=await login(password),secondCookie=await login(password);
assert.equal((await call("/users/me",undefined,cookie)).status,200);
assert.equal((await call("/questions")).status,401);
const q=await call("/questions",{title:"보안 검증용 질문",content:"이 스크립트가 만든 로컬 검증 데이터입니다."},cookie);assert.equal(q.status,200);
const {questionId}=await q.json();
const details=await (await call(`/questions/${questionId}`,undefined,cookie)).json();assert.equal(details.writerEmail,undefined);
assert.equal((await call(`/questions/${questionId}/answers`,{content:"검증용 답변"},cookie)).status,200);
assert.equal((await call("/questions",{title:"x",content:"x".repeat(40000)},cookie)).status,413);
assert.equal((await call("/users/forgot-password",{email})).status,202);
const resetToken=await tokenFor("/reset-password");
const reset=await call("/users/reset-password",{token:resetToken,password:replacement},cookie);assert.equal(reset.status,200);assert.match(reset.headers.get("set-cookie"),/Max-Age=0/i);
for (const oldCookie of [cookie,secondCookie]) assert.equal((await call("/users/me",undefined,oldCookie)).status,401);
assert.equal((await call("/users/reset-password",{token:resetToken,password})).status,400);
assert.equal((await call("/users/login",{email,password})).status,401);
const updatedCookie=await login(replacement);
const logout=await call("/users/logout",{},updatedCookie);assert.equal(logout.status,200);assert.match(logout.headers.get("set-cookie"),/Max-Age=0/i);
assert.equal((await call("/users/me",undefined,updatedCookie)).status,401);
const finalCookie=await login(replacement);
assert.equal((await call("/users/delete-account",{password:"wrong-password",confirmation:"회원 탈퇴"},finalCookie)).status,400);
const deletion=await call("/users/delete-account",{password:replacement,confirmation:"회원 탈퇴"},finalCookie);
assert.equal(deletion.status,200);assert.match(deletion.headers.get("set-cookie"),/Max-Age=0/i);
assert.equal((await call("/users/me",undefined,finalCookie)).status,401);
assert.equal((await call("/users/login",{email,password:replacement})).status,401);
console.log("PASS: email verification, one-time links, reset/all-session revocation, password-confirmed deletion, CSRF, cookie security, Q&A privacy, payload cap and logout. Only the generated smoke account and its content were deleted.");
