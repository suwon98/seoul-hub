package org.seoulhub.backend.domain.amswer;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.seoulhub.backend.domain.question.Question;
import org.seoulhub.backend.domain.user.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "answers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Answer(String content, Question question, User user) {
        this.content = content;
        this.question = question;
        this.user = user;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }
}
