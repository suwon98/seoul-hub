package org.seoulhub.backend.domain.amswer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("delete from Answer a where a.user.id=:id or a.question.id in (select q.id from Question q where q.user.id=:id)")
    int deleteForAccount(@org.springframework.data.repository.query.Param("id") Long id);
    List<Answer> findByQuestionIdOrderByIdAsc(Long questionId);
}
