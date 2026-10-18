package org.seoulhub.backend.domain.question;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("delete from Question q where q.user.id=:id")
    int deleteForAccount(@org.springframework.data.repository.query.Param("id") Long id);
    List<Question> findAllByOrderByIdDesc();

    List<Question> findByUserIdOrderByIdDesc(Long userId);
}
