package com.example.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "teacher_questions")
public class TeacherQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long teacherId;

    @Column(nullable = false, length = 120)
    private String course;

    @Column(nullable = false, length = 2000)
    private String prompt;

    @Column(length = 2000)
    private String answer;

    @Column(nullable = false, length = 30)
    private String difficulty;

    @Column(length = 255)
    private String attachmentName;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] attachment;

    protected TeacherQuestion() { }

    public TeacherQuestion(Long teacherId, String course, String prompt, String answer, String difficulty) {
        this.teacherId = teacherId;
        this.course = course;
        this.prompt = prompt;
        this.answer = answer;
        this.difficulty = difficulty;
    }

    public Long getId() { return id; }
    public Long getTeacherId() { return teacherId; }
    public String getCourse() { return course; }
    public String getPrompt() { return prompt; }
    public String getAnswer() { return answer; }
    public String getDifficulty() { return difficulty; }
    public String getAttachmentName() { return attachmentName; }
    public byte[] getAttachment() { return attachment; }
    public void setAttachmentName(String attachmentName) { this.attachmentName = attachmentName; }
    public void setAttachment(byte[] attachment) { this.attachment = attachment; }
}
