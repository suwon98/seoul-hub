package org.seoulhub.backend.domain.question;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Tag(name = "question", description = "Q&A 게시판 커뮤니티 API")
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @Operation(summary = "신규 질문글 등록 (JWT 인증 필요)", description = "헤더에 JWT Access Token을 실어 보내어 새로운 Q&A 게시글을 안전하게 등록합니다.")
    @PostMapping
    public ResponseEntity<QuestionCreateResponse> createQuestion(@RequestBody @Valid QuestionCreateRequestDto requestDto,
                                                                 Principal principal) {
        String email = principal.getName();
        Long questionId = questionService.createQuestion(email, requestDto);
        return ResponseEntity.ok(new QuestionCreateResponse(true, questionId));
    }

    @Operation(summary = "전체 질문글 목록 조회 (JWT 인증 필요)", description = "시스템 내 등록된 모든 Q&A 질문 목록을 최신 등록 순서대로 전수 반환합니다.")
    @GetMapping
    public ResponseEntity<List<QuestionResponseDto>> getAllQuestions() {
        List<QuestionResponseDto> list = questionService.getAllQuestions();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "질문글 단건 정밀 상세 조회 (JWT 인증 필요)", description = "고유 글 ID 식별자를 기반으로 특정 질문글의 본문 상세 내역을 정밀 수송합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<QuestionResponseDto> getQuestion(@PathVariable Long id) {
        QuestionResponseDto dto = questionService.getQuestion(id);
        return ResponseEntity.ok(dto);
    }

    public record QuestionCreateResponse(boolean success, Long questionId) {}
}