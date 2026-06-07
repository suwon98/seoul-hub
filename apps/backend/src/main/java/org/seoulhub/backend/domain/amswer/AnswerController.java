package org.seoulhub.backend.domain.amswer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Tag(name = "answer", description = "Q&A 게시판 답변 API")
@RestController
@RequestMapping("/api/v1/questions/{questionId}/answers")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    @Operation(summary = "신규 답변(댓글) 등록 (JWT 인증 필요)", description = "지정한 질문 게시글에 대한 답변 내용을 안전하게 등록합니다.")
    @PostMapping
    public ResponseEntity<AnswerCreateResponse> createAnswer(@PathVariable Long questionId,
                                                             @RequestBody @Valid AnswerCreateRequestDto requestDto,
                                                             Principal principal) {
        String email = principal.getName();
        Long answerId = answerService.createAnswer(questionId, email, requestDto);
        return ResponseEntity.ok(new AnswerCreateResponse(true, answerId));
    }

    @Operation(summary = "특정 질문글의 답변 목록 조회 (JWT 인증 필요)", description = "질문 게시글 하나에 매핑되어 작성된 답변 세트들을 시간 등록 순서대로 전수 반환합니다.")
    @GetMapping
    public ResponseEntity<List<AnswerResponseDto>> getAnswers(@PathVariable Long questionId) {
        List<AnswerResponseDto> list = answerService.getAnswersByQuestion(questionId);
        return ResponseEntity.ok(list);
    }

    public record AnswerCreateResponse(boolean success, Long answerId) {}
}
