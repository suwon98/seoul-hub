package org.seoulhub.backend.domain.amswer;

import lombok.RequiredArgsConstructor;
import org.seoulhub.backend.domain.question.Question;
import org.seoulhub.backend.domain.question.QuestionRepository;
import org.seoulhub.backend.domain.user.User;
import org.seoulhub.backend.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long createAnswer(Long questionId, String email, AnswerCreateRequestDto requestDto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 인증이 만료된 유저 계정입니다."));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("대상 질문 게시글이 존재하지 않습니다."));

        Answer answer = Answer.builder()
                .content(requestDto.getContent())
                .question(question)
                .user(user)
                .build();

        Answer savedAnswer = answerRepository.save(answer);
        return savedAnswer.getId();
    }

    public List<AnswerResponseDto> getAnswersByQuestion(Long questionId) {
        if (!questionRepository.existsById(questionId)) {
            throw new IllegalArgumentException("대상 질문 게시글이 존재하지 않습니다.");
        }

        return answerRepository.findByQuestionIdOrderByIdAsc(questionId).stream()
                .map(AnswerResponseDto::new)
                .collect(Collectors.toList());
    }
}
