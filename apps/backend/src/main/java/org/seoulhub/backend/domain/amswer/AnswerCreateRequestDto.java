package org.seoulhub.backend.domain.amswer;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AnswerCreateRequestDto {

    @NotBlank(message = "답변 내용은 필수 입력 항목입니다.")
    private String content;
}
