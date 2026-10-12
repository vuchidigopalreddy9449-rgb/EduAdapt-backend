package com.example.backend.repository;

import com.example.backend.entity.TeacherQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeacherQuestionRepository extends JpaRepository<TeacherQuestion, Long> {
    List<TeacherQuestion> findAllByTeacherIdOrderByIdDesc(Long teacherId);
    Optional<TeacherQuestion> findByIdAndTeacherId(Long id, Long teacherId);
}
