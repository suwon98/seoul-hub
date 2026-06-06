package org.seoulhub.backend.domain.question;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findAllByOrderByIdDesc();

    List<Question> findByUserIdOrderByIdDesc(Long userId);
}
