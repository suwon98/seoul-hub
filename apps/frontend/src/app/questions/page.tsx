"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { ApiError, errorMessage, apiClient, QuestionBoardResponse } from "@/lib/api-client";

export default function QuestionsPage() {
  const router = useRouter();

  const [questions, setQuestions] = useState<QuestionBoardResponse[]>([]);

  // 작성 폼 상태 변수
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");

  // UI 인디케이터 상태 변수
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 질문 리스트 로드 함수
  const loadQuestions = useCallback(() => {
    return apiClient.getAllQuestions().then(data => {
      setError(null); setQuestions(data); setLoading(false);
    }).catch((err: unknown) => { if (err instanceof ApiError && err.status === 401) router.replace("/login"); setError(errorMessage(err)); setLoading(false); });
  }, [router]);

  // 인가 가드 레이어
  useEffect(() => {
    void loadQuestions();
  }, [router, loadQuestions]);

  // 질문 등록 처리 핸들러
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (submitting) return;
    if (!title.trim() || !content.trim()) {
      alert("제목과 내용을 모두 기입해 주세요.");
      return;
    }

    try {
      setSubmitting(true);
      const res = await apiClient.createQuestion({ title, content });
      if (res.success) {
        alert("질문이 커뮤니티에 무사히 등록되었습니다.");
        setTitle("");
        setContent("");
        await loadQuestions();
      }
    } catch (err: unknown) {
      alert(errorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="min-h-screen bg-slate-50 p-6 sm:p-10 text-slate-900">
      <div className="max-w-4xl mx-auto space-y-8">

        {/* 상단 라우팅 헤더 */}
        <header className="flex justify-between items-center p-6 bg-white border border-slate-100 rounded-2xl shadow-sm">
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">Seoul-Hub Q&A Board</h1>
            <p className="text-xs text-slate-500">서울의 장소와 방문 경험에 대해 질문하고 답변해 보세요.</p>
          </div>
          <button
            onClick={() => router.push("/dashboard")}
            className="px-4 py-2 text-xs font-bold text-blue-600 bg-blue-50 hover:bg-blue-100 rounded-lg transition-all cursor-pointer"
          >
            혼잡도 대시보드 가기
          </button>
        </header>

        {/* 질문 글쓰기 섹션 폼 */}
        <section className="p-6 bg-white border border-slate-100 rounded-2xl shadow-sm space-y-4">
          <h2 className="text-lg font-bold text-slate-800">새로운 질문 작성하기</h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <input
                type="text"
                aria-label="질문 제목"
                placeholder="질문 제목을 간결하게 입력하세요 (100자 제한)"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-slate-900 text-sm text-slate-800 font-medium"
                maxLength={100}
              />
            </div>
            <div>
              <textarea aria-label="내용" maxLength={10000}
                placeholder="서울 허브 커뮤니티원들과 나눌 질문 상세 내용을 적어주세요."
                value={content}
                onChange={(e) => setContent(e.target.value)}
                rows={4}
                className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-slate-900 text-sm text-slate-800 leading-relaxed"
              />
            </div>
            <div className="text-right">
              <button
                type="submit"
                disabled={submitting}
                className="px-6 py-2.5 bg-slate-900 hover:bg-slate-800 text-white font-bold text-sm rounded-lg shadow-sm transition-all cursor-pointer disabled:opacity-50"
              >
                {submitting ? "등록 중..." : "질문 등록하기"}
              </button>
            </div>
          </form>
        </section>

        {/* 실시간 질문 리스트 렌더링 영역 */}
        <section className="space-y-4">
          <h2 className="text-lg font-bold text-slate-800">최신 질문 ({questions.length})</h2>

          {error && (
            <div className="p-4 bg-rose-50 text-rose-700 font-semibold rounded-xl text-sm border border-rose-100">
              불러오기 실패: {error}
            </div>
          )}

          {loading ? (
            <div className="text-center py-10 text-slate-400 font-medium text-sm">
              질문을 불러오는 중...
            </div>
          ) : questions.length === 0 ? (
            <div className="p-12 text-center bg-white border border-dashed border-slate-200 rounded-2xl text-slate-400 text-sm font-medium">
              아직 등록된 질문이 없습니다. 첫 질문의 주인공이 되어보세요!
            </div>
          ) : (
            <div className="space-y-4">
              {questions.map((q) => (
                <article
                  key={q.id}
                  onClick={() => router.push(`/questions/${q.id}`)}
                  role="link" tabIndex={0} onKeyDown={(e) => { if (e.key === "Enter") router.push(`/questions/${q.id}`); }}
                  className="p-6 bg-white border border-slate-100 rounded-2xl shadow-xs hover:shadow-md hover:border-slate-300 transition-all space-y-3 cursor-pointer"
                >
                  <div className="flex justify-between items-start gap-4">
                    <h3 className="text-base font-extrabold text-slate-900 leading-snug">{q.title}</h3>
                    <span className="text-[10px] bg-slate-100 text-slate-600 font-bold px-2 py-1 rounded-sm shrink-0">
                      No.{q.id}
                    </span>
                  </div>

                  <p className="text-sm text-slate-600 leading-relaxed whitespace-pre-wrap">{q.content}</p>

                  <div className="pt-3 border-t border-slate-50 flex justify-between items-center text-xs text-slate-400 font-medium">
                    <div className="flex items-center gap-2">
                      <span className="text-slate-700 font-bold bg-slate-50 px-2 py-0.5 rounded-sm border border-slate-100">
                        {q.writerName}
                      </span>
                      <span className="text-slate-300">|</span>
                    </div>
                    <span>{new Date(q.createdAt).toLocaleString()}</span>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      </div>
    </main>
  );
}
