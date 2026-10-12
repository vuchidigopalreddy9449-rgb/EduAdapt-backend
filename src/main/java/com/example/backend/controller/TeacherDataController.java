package com.example.backend.controller;

import com.example.backend.entity.TeacherExam;
import com.example.backend.entity.TeacherQuestion;
import com.example.backend.entity.User;
import com.example.backend.repository.TeacherExamRepository;
import com.example.backend.repository.TeacherQuestionRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
public class TeacherDataController {
    private static final long MAX_ATTACHMENT_BYTES = 10L * 1024 * 1024;

    private final TeacherQuestionRepository questionRepository;
    private final TeacherExamRepository examRepository;
    private final UserRepository userRepository;

    public TeacherDataController(TeacherQuestionRepository questionRepository,
                                 TeacherExamRepository examRepository,
                                 UserRepository userRepository) {
        this.questionRepository = questionRepository;
        this.examRepository = examRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/questions")
    public List<QuestionResponse> questions(@RequestParam Long teacherId, HttpServletRequest request) {
        requireTeacher(teacherId, request);
        return questionRepository.findAllByTeacherIdOrderByIdDesc(teacherId).stream()
                .map(QuestionResponse::from).toList();
    }

    @PostMapping("/questions")
    public ResponseEntity<?> addQuestion(@RequestBody QuestionRequest request, HttpServletRequest servletRequest) {
        try {
            requireTeacher(request.teacherId(), servletRequest);
            TeacherQuestion question = questionRepository.save(new TeacherQuestion(
                    request.teacherId(), clean(request.course(), 120, "Course"),
                    clean(request.prompt(), 2000, "Question"), clean(request.answer(), 2000, "Answer"),
                    clean(request.difficulty(), 30, "Difficulty")));
            return ResponseEntity.ok(QuestionResponse.from(question));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/questions/batch")
    public ResponseEntity<?> addQuestionBatch(@RequestBody QuestionBatchRequest request, HttpServletRequest servletRequest) {
        try {
            requireTeacher(request.teacherId(), servletRequest);
            if (request.questions() == null || request.questions().isEmpty() || request.questions().size() > 10) {
                throw new IllegalArgumentException("Send between 1 and 10 questions at a time.");
            }
            List<TeacherQuestion> saved = request.questions().stream().map(question -> new TeacherQuestion(
                    request.teacherId(), clean(request.course(), 120, "Course"),
                    clean(question.prompt(), 2000, "Question"), clean(question.answer(), 2000, "Answer"),
                    clean(request.difficulty(), 30, "Difficulty"))).toList();
            return ResponseEntity.ok(questionRepository.saveAll(saved).stream().map(QuestionResponse::from).toList());
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping(value = "/questions/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadQuestions(@RequestParam Long teacherId,
                                             @RequestParam String course,
                                             @RequestParam MultipartFile file,
                                             HttpServletRequest request) {
        try {
            requireTeacher(teacherId, request);
            validateFile(file);
            String fileName = safeFileName(file.getOriginalFilename());
            TeacherQuestion question = new TeacherQuestion(teacherId, clean(course, 120, "Course"),
                    "Imported file: " + fileName, "Uploaded question material", "—");
            question.setAttachmentName(fileName);
            question.setAttachment(file.getBytes());
            return ResponseEntity.ok(QuestionResponse.from(questionRepository.save(question)));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        } catch (IOException exception) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Could not read the uploaded file."));
        }
    }

    @GetMapping("/questions/{id}/file")
    @Transactional(readOnly = true)
    public ResponseEntity<ByteArrayResource> questionFile(@PathVariable Long id, @RequestParam Long teacherId, HttpServletRequest request) {
        requireTeacher(teacherId, request);
        TeacherQuestion question = questionRepository.findByIdAndTeacherId(id, teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Uploaded file was not found."));
        return fileResponse(question.getAttachmentName(), question.getAttachment());
    }

    @GetMapping("/exams")
    public List<ExamResponse> exams(@RequestParam Long teacherId, HttpServletRequest request) {
        requireTeacher(teacherId, request);
        return examRepository.findAllByTeacherIdOrderByIdDesc(teacherId).stream().map(ExamResponse::from).toList();
    }

    @PostMapping(value = "/exams", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addExam(@RequestParam Long teacherId,
                                     @RequestParam String title,
                                     @RequestParam String course,
                                     @RequestParam(required = false) String date,
                                     @RequestParam(required = false) MultipartFile file,
                                     HttpServletRequest request) {
        try {
            requireTeacher(teacherId, request);
            TeacherExam exam = new TeacherExam(teacherId, clean(title, 180, "Assessment name"),
                    clean(course, 120, "Course"), cleanOptional(date, 10));
            if (file != null && !file.isEmpty()) {
                validateFile(file);
                exam.setAttachmentName(safeFileName(file.getOriginalFilename()));
                exam.setAttachment(file.getBytes());
            }
            return ResponseEntity.ok(ExamResponse.from(examRepository.save(exam)));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        } catch (IOException exception) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Could not read the attached exam file."));
        }
    }

    @GetMapping("/exams/{id}/file")
    @Transactional(readOnly = true)
    public ResponseEntity<ByteArrayResource> examFile(@PathVariable Long id, @RequestParam Long teacherId, HttpServletRequest request) {
        requireTeacher(teacherId, request);
        TeacherExam exam = examRepository.findByIdAndTeacherId(id, teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment was not found."));
        return fileResponse(exam.getAttachmentName(), exam.getAttachment());
    }

    private void requireTeacher(Long teacherId, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object sessionUserId = session == null ? null : session.getAttribute("EDUADAPT_USER_ID");
        Object sessionRole = session == null ? null : session.getAttribute("EDUADAPT_ROLE");
        if (!(sessionUserId instanceof Long id) || !id.equals(teacherId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in again to access teacher data.");
        }
        if (!"TEACHER".equalsIgnoreCase(String.valueOf(sessionRole))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher access is required.");
        }
        User user = userRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher account was not found. Please sign in again."));
        if (!"TEACHER".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException("This account is not a teacher account.");
        }
    }

    private static String clean(String value, int maxLength, String field) {
        if (value == null || value.isBlank() || value.trim().length() > maxLength) {
            throw new IllegalArgumentException(field + " is required and must be no longer than " + maxLength + " characters.");
        }
        return value.trim();
    }

    private static String cleanOptional(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        if (value.trim().length() > maxLength) throw new IllegalArgumentException("Date value is too long.");
        return value.trim();
    }

    private static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Choose a file to upload.");
        if (file.getSize() > MAX_ATTACHMENT_BYTES) throw new IllegalArgumentException("Files must be 10 MB or smaller.");
    }

    private static String safeFileName(String name) {
        String safe = name == null ? "uploaded-file" : name.replace('\\', '/');
        safe = safe.substring(safe.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        if (safe.isBlank()) safe = "uploaded-file";
        return safe.length() > 255 ? safe.substring(safe.length() - 255) : safe;
    }

    private static ResponseEntity<ByteArrayResource> fileResponse(String fileName, byte[] bytes) {
        if (bytes == null || fileName == null) throw new IllegalArgumentException("No file is attached to this item.");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .body(new ByteArrayResource(bytes));
    }

    public record QuestionRequest(Long teacherId, String course, String prompt, String answer, String difficulty) { }
    public record GeneratedQuestion(String prompt, String answer) { }
    public record QuestionBatchRequest(Long teacherId, String course, String difficulty, List<GeneratedQuestion> questions) { }
    public record QuestionResponse(Long id, String course, String prompt, String answer, String difficulty, String attachmentName) {
        static QuestionResponse from(TeacherQuestion q) {
            return new QuestionResponse(q.getId(), q.getCourse(), q.getPrompt(), q.getAnswer(), q.getDifficulty(), q.getAttachmentName());
        }
    }
    public record ExamResponse(Long id, String title, String course, String date, int questions, String status, String file) {
        static ExamResponse from(TeacherExam exam) {
            return new ExamResponse(exam.getId(), exam.getTitle(), exam.getCourse(), exam.getScheduledDate(), 0,
                    exam.getStatus(), exam.getAttachmentName());
        }
    }
}
