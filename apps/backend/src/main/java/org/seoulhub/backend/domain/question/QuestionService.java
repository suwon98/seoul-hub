package org.seoulhub.backend.domain.question;

import lombok.RequiredArgsConstructor;
import org.seoulhub.backend.domain.user.User;
import org.seoulhub.backend.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long createQuestion(String email, QuestionCreateRequestDto requestDto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 인증이 만료된 계정입니다."));

        Question question = requestDto.toEntity(user);
        Question savedQuestion = questionRepository.save(question);
        return savedQuestion.getId();
    }

    public List<QuestionResponseDto> getAllQuestions() {
        return questionRepository.findAllByOrderByIdDesc().stream()
                .map(QuestionResponseDto::new)
                .collect(Collectors.toList());
    }

    public QuestionResponseDto getQuestion(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("요청하신 게시글이 존재하지 않거나 소멸되었습니다."));
        return new QuestionResponseDto(question);
    }
}
