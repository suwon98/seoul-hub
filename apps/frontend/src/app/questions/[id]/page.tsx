"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter, useParams } from "next/navigation";
import { apiClient, QuestionBoardResponse, AnswerResponse } from "@/lib/api-client";

export default function QuestionDetailPage() {
  const router = useRouter();
  const params = useParams();
  const id = Number(params.id); // URL 경로변수에서 질문글 ID 추출

  const [token, setToken] = useState<string | null>(null);
  const [question, setQuestion] = useState<QuestionBoardResponse | null>(null);
  const [answers, setAnswers] = useState<AnswerResponse[]>([]);
  
  // 답변 작성 폼 상태
  const [content, setContent] = useState("");
  
  // UI 상태 지시어
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 데이터 통합 로드 파이프라인
  const loadPageData = useCallback(async (authToken: string) => {
    try {
      setLoading(true);
      setError(null);
      
      // 두 요청을 병렬 처리하여 네트워킹 레이턴시 최적화
      const [questionData, answersData] = await Promise.all([
        apiClient.getQuestion(id, authToken),
        apiClient.getAnswers(id, authToken)
      ]);
      
      setQuestion(questionData);
      setAnswers(answersData);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [id]);

  // 인가 가드 실행
  useEffect(() => {
    const storedToken = localStorage.getItem("accessToken");
    if (!storedToken) {
      alert("🔒 회원 전용 상세 페이지입니다. 로그인을 진행해 주세요.");
      router.push("/login");
      return;
    }
    setToken(storedToken);
    loadPageData(storedToken);
  }, [router, loadPageData]);

  // 답변(댓글) 등록 핸들러
  const handleAnswerSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token || !content.trim()) return;

    try {
      setSubmitting(true);
      const res = await apiClient.createAnswer(id, { content }, token);
      if (res.success) {
        setContent("");
        // 댓글 컴포넌트 실시간 동기화 리로드
        await loadPageData(token);
      }
    } catch (err: any) {
      alert(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center text-slate-400 font-medium text-sm">
        백엔드로부터 Q&A 통합 자산 수송 중...
      </div>
    );
  }

  if (error || !question) {
    return (
      <div className="min-h-screen bg-slate-50 p-10">
        <div className="max-w-2xl mx-auto p-6 bg-rose-50 border border-rose-100 rounded-2xl text-rose-700 text-sm font-semibold shadow-sm">
          ❌ 오류 발생: {error || "게시글을 찾을 수 없습니다."}
        </div>
      </div>
    );
  }

  return (
    <main className="min-h-screen bg-slate-50 p-6 sm:p-10 text-slate-900">
      <div className="max-w-3xl mx-auto space-y-8">
        
        {/* 뒤로가기 제어 바 */}
        <button
          onClick={() => router.push("/questions")}
          className="text-xs font-bold text-slate-600 hover:text-slate-900 flex items-center gap-2 transition-all cursor-pointer"
        >
          ⬅️ 질문 목록으로 탈출
        </button>

        {/* 질문글 본문 디스플레이 카드 */}
        <article className="p-8 bg-white border border-slate-100 rounded-3xl shadow-xs space-y-6">
          <div className="space-y-2">
            <span className="text-[10px] bg-slate-900 text-white font-black px-2.5 py-1 rounded-sm">
              QUESTION
            </span>
            <h1 className="text-2xl font-black tracking-tight text-slate-900 pt-1">
              {question.title}
            </h1>
          </div>

          <div className="p-6 bg-slate-50 border border-slate-100 rounded-xl text-sm text-slate-700 leading-relaxed whitespace-pre-wrap min-h-[150px]">
            {question.content}
          </div>

          <div className="flex justify-between items-center text-xs text-slate-400 pt-2 border-t border-slate-100 font-medium">
            <div className="flex items-center gap-2">
              <span className="text-slate-800 font-bold bg-slate-100 px-2 py-0.5 rounded-sm">
                {question.writerName}
              </span>
              <span>{question.writerEmail}</span>
            </div>
            <span>작성일시: {new Date(question.createdAt).toLocaleString()}</span>
          </div>
        </article>

        {/* 답변(댓글) 타임라인 보드 섹션 */}
        <section className="space-y-4">
          <h2 className="text-base font-bold text-slate-800">
            전문가 답변 스택 ({answers.length})
          </h2>

          {answers.length === 0 ? (
            <div className="p-8 text-center bg-white border border-slate-100 rounded-2xl text-slate-400 text-xs font-medium">
              아직 등록된 답변이 없습니다. 지식을 나누어 보세요!
            </div>
          ) : (
            <div className="space-y-4">
              {answers.map((ans) => (
                <article key={ans.id} className="p-5 bg-white border border-slate-100 rounded-2xl shadow-xs space-y-2">
                  <p className="text-sm text-slate-700 leading-relaxed whitespace-pre-wrap">
                    {ans.content}
                  </p>
                  <div className="flex justify-between items-center text-[11px] text-slate-400 pt-2 border-t border-slate-50 font-medium">
                    <div className="flex items-center gap-1.5">
                      <span className="text-blue-600 font-extrabold bg-blue-50 px-1.5 py-0.5 rounded-sm">
                        {ans.writerName}
                      </span>
                      <span>({ans.writerEmail})</span>
                    </div>
                    <span>{new Date(ans.createdAt).toLocaleString()}</span>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>

        {/* 답변 작성 작성 폼 */}
        <section className="p-6 bg-white border border-slate-100 rounded-2xl shadow-sm">
          <form onSubmit={handleAnswerSubmit} className="space-y-4">
            <h3 className="text-sm font-bold text-slate-800">나의 답변 남기기 </h3>
            <div>
              <textarea
                placeholder="답변 내용을 책임감 있게 기입해 주세요. 깨끗한 커뮤니티 환경에 동참해 주셔서 감사합니다."
                value={content}
                onChange={(e) => setContent(e.target.value)}
                rows={3}
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-slate-900 text-sm text-slate-800 leading-relaxed"
                maxLength={2000}
              />
            </div>
            <div className="text-right">
              <button
                type="submit"
                disabled={submitting || !content.trim()}
                className="px-5 py-2 bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs rounded-lg shadow-sm transition-all cursor-pointer disabled:opacity-50"
              >
                {submitting ? "답변 인서트 중..." : "답변 등록하기"}
              </button>
            </div>
          </form>
        </section>

      </div>
    </main>
  );
}