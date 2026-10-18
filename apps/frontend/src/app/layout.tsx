import type { Metadata } from "next";
import "./globals.css";
export const metadata: Metadata = {
  title: "Seoul Hub | 서울 혼잡도와 커뮤니티",
  description: "서울 주요 지역의 인구 혼잡도와 시간별 추이를 확인하고 방문 경험을 나누세요.",
};
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="ko"><body>{children}</body></html>;
}
