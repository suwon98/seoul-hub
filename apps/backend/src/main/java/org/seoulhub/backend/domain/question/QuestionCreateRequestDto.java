package org.seoulhub.backend.domain.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.seoulhub.backend.domain.user.User;

@Getter
@NoArgsConstructor
public class QuestionCreateRequestDto {

    @NotBlank(message = "질문 제목은 필수 입력 항목입니다.")
    @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
    private String title;

    @NotBlank(message = "질문 내용은 필수 입력 항목입니다.")
    private String content;

    public Question toEntity(User user) {
        return Question.builder()
                .title(this.title)
                .content(this.content)
                .user(user)
                .build();
    }
}
