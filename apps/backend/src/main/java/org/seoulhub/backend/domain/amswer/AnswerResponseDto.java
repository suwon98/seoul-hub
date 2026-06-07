package org.seoulhub.backend.domain.amswer;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AnswerResponseDto {
    private final Long id;
    private final String content;
    private final String writerEmail;
    private final String writerName;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public AnswerResponseDto(Answer answer) {
        this.id = answer.getId();
        this.content = answer.getContent();
        this.writerEmail = answer.getUser().getEmail();
        this.writerName = answer.getUser().getName();
        this.createdAt = answer.getCreatedAt();
        this.updatedAt = answer.getUpdatedAt();
    }
}
