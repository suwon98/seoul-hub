import { readdir, readFile } from "node:fs/promises";
const email=process.argv[2]?.trim().toLowerCase();
if(!email) { console.error("Usage: node scripts/read-local-mail.mjs your-test@example.com"); process.exit(1); }
const directory=new URL("../apps/backend/data/mail/",import.meta.url);
try {
  const messages=await Promise.all((await readdir(directory)).filter(name=>/^[a-f0-9-]+\.json$/.test(name)).map(async name=>JSON.parse(await readFile(new URL(name,directory),"utf8"))));
  const latest=messages.filter(message=>message.to===email).sort((a,b)=>b.createdAt.localeCompare(a.createdAt))[0];
  if(!latest) {console.log("해당 이메일의 로컬 메일이 없습니다. local 프로필에서 메일을 요청해 주세요.");process.exit(1);}
  console.log(latest.subject + "\n" + latest.createdAt + "\n\n" + latest.body);
} catch {console.error("로컬 수신함을 읽지 못했습니다. apps/backend 디렉터리에서 local 프로필로 서버를 실행해 주세요.");process.exit(1);}
