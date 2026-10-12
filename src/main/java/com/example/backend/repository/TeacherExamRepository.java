package com.example.backend.repository;

import com.example.backend.entity.TeacherExam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeacherExamRepository extends JpaRepository<TeacherExam, Long> {
    List<TeacherExam> findAllByTeacherIdOrderByIdDesc(Long teacherId);
    Optional<TeacherExam> findByIdAndTeacherId(Long id, Long teacherId);
}
