package org.seoulhub.backend.domain.amswer;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AnswerCreateRequestDto {

    @NotBlank(message = "답변 내용은 필수 입력 항목입니다.")
    @jakarta.validation.constraints.Size(max=2000,message="내용이 너무 깁니다.")
    private String content;
}
