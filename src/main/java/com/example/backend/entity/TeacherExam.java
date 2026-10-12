package com.example.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "teacher_exams")
public class TeacherExam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long teacherId;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 120)
    private String course;

    @Column(length = 10)
    private String scheduledDate;

    @Column(nullable = false, length = 20)
    private String status = "Draft";

    @Column(length = 255)
    private String attachmentName;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] attachment;

    protected TeacherExam() { }

    public TeacherExam(Long teacherId, String title, String course, String scheduledDate) {
        this.teacherId = teacherId;
        this.title = title;
        this.course = course;
        this.scheduledDate = scheduledDate;
    }

    public Long getId() { return id; }
    public Long getTeacherId() { return teacherId; }
    public String getTitle() { return title; }
    public String getCourse() { return course; }
    public String getScheduledDate() { return scheduledDate; }
    public String getStatus() { return status; }
    public String getAttachmentName() { return attachmentName; }
    public byte[] getAttachment() { return attachment; }
    public void setAttachmentName(String attachmentName) { this.attachmentName = attachmentName; }
    public void setAttachment(byte[] attachment) { this.attachment = attachment; }
}
