package org.seoulhub.backend.domain.question;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class QuestionResponseDto {
    private final Long id;
    private final String title;
    private final String content;
    private final String writerEmail;
    private final String writerName;
    private final LocalDateTime createdAt;
    private final LocalDateTime updateAt;

    public QuestionResponseDto(Question question) {
        this.id = question.getId();
        this.title = question.getTitle();
        this.content = question.getContent();
        this.writerEmail = question.getUser().getEmail();
        this.writerName = question.getUser().getName();
        this.createdAt = question.getCreatedAt();
        this.updateAt = question.getUpdatedAt();
    }
}
