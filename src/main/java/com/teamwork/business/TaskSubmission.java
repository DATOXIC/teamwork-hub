package com.teamwork.business;

import com.teamwork.util.DeliverableStorage;
import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * JPA Entity đại diện cho một Đợt Nộp Bàn Giao / Nghiệm Thu của Thẻ Công Việc Lớn (Task Cha).
 * Cho phép theo dõi lịch sử nộp nhiều phiên bản (Multi-revision Submission History)
 * và chu kỳ thẩm định của Trưởng Dự Án (PM Review Feedback Cycle).
 */
@Entity
@Table(name = "task_submissions")
public class TaskSubmission implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "task_id", nullable = false)
    private int taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", insertable = false, updatable = false)
    private Task task;

    @Column(name = "revision_number", nullable = false)
    private int revisionNumber = 1;

    @Column(name = "submitted_by")
    private Integer submittedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by", insertable = false, updatable = false)
    private User submitter;

    @Transient
    private String submitterName;

    @Column(name = "submission_note", nullable = false)
    private String submissionNote = "";

    @Column(name = "summary")
    private String summary = "";

    @Column(name = "demo_url", length = 500)
    private String demoUrl = "";

    @Column(name = "code_url", length = 500)
    private String codeUrl = "";

    @Column(name = "test_result")
    private String testResult = "";

    @Column(name = "testing_guide")
    private String testingGuide = "";

    @Column(name = "file_path", length = 255)
    private String filePath = "";

    @Column(name = "status", nullable = false, length = 20)
    private String status = "SUBMITTED"; // SUBMITTED, REVISE, REJECTED, APPROVED

    @Column(name = "pm_feedback")
    private String pmFeedback = "";

    @Column(name = "quality_rating")
    private int qualityRating = 0;

    @Column(name = "reviewed_by")
    private Integer reviewedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by", insertable = false, updatable = false)
    private User reviewer;

    @Transient
    private String reviewerName;

    @Column(name = "submitted_at", nullable = false, length = 50)
    private String submittedAt = "";

    @Column(name = "reviewed_at", length = 50)
    private String reviewedAt = "";

    // ===================== CONSTRUCTORS =====================

    public TaskSubmission() {
    }

    public TaskSubmission(int taskId, int revisionNumber, Integer submittedBy, String submissionNote,
                          String filePath, String submittedAt) {
        this.taskId = taskId;
        this.revisionNumber = revisionNumber;
        this.submittedBy = submittedBy;
        this.submissionNote = submissionNote != null ? submissionNote : "";
        this.filePath = filePath != null ? filePath : "";
        this.submittedAt = submittedAt != null ? submittedAt : "";
        this.status = "SUBMITTED";
    }

    // ===================== GETTERS & SETTERS =====================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public int getRevisionNumber() {
        return revisionNumber;
    }

    public void setRevisionNumber(int revisionNumber) {
        this.revisionNumber = revisionNumber;
    }

    public Integer getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(Integer submittedBy) {
        this.submittedBy = submittedBy;
    }

    public User getSubmitter() {
        return submitter;
    }

    public void setSubmitter(User submitter) {
        this.submitter = submitter;
    }

    public String getSubmitterName() {
        return submitterName != null ? submitterName : "Thành viên";
    }

    public void setSubmitterName(String submitterName) {
        this.submitterName = submitterName;
    }

    public String getSubmissionNote() {
        return submissionNote;
    }

    public void setSubmissionNote(String submissionNote) {
        this.submissionNote = submissionNote != null ? submissionNote : "";
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary != null ? summary : "";
    }

    public String getDemoUrl() {
        return demoUrl;
    }

    public void setDemoUrl(String demoUrl) {
        this.demoUrl = demoUrl != null ? demoUrl : "";
    }

    public String getCodeUrl() {
        return codeUrl;
    }

    public void setCodeUrl(String codeUrl) {
        this.codeUrl = codeUrl != null ? codeUrl : "";
    }

    public String getTestResult() {
        return testResult;
    }

    public void setTestResult(String testResult) {
        this.testResult = testResult != null ? testResult : "";
    }

    public String getTestingGuide() {
        return testingGuide;
    }

    public void setTestingGuide(String testingGuide) {
        this.testingGuide = testingGuide != null ? testingGuide : "";
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath != null ? filePath : "";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status != null ? status : "SUBMITTED";
    }

    public String getPmFeedback() {
        return pmFeedback;
    }

    public void setPmFeedback(String pmFeedback) {
        this.pmFeedback = pmFeedback != null ? pmFeedback : "";
    }

    public int getQualityRating() {
        return qualityRating;
    }

    public void setQualityRating(int qualityRating) {
        this.qualityRating = qualityRating;
    }

    public Integer getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Integer reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public User getReviewer() {
        return reviewer;
    }

    public void setReviewer(User reviewer) {
        this.reviewer = reviewer;
    }

    public String getReviewerName() {
        return reviewerName != null ? reviewerName : "Trưởng Dự Án";
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public String getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(String submittedAt) {
        this.submittedAt = submittedAt != null ? submittedAt : "";
    }

    public String getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(String reviewedAt) {
        this.reviewedAt = reviewedAt != null ? reviewedAt : "";
    }

    // ===================== HELPER METHODS CHO JSP =====================

    /**
     * Tên hiển thị gốc của tệp đính kèm (bỏ UUID hex).
     */
    public String getDeliverableDisplayName() {
        return DeliverableStorage.displayName(filePath);
    }

    /**
     * Tệp đã tải lên máy chủ qua quy trình hợp lệ (có UUID).
     */
    public boolean isDeliverableUploaded() {
        return DeliverableStorage.isStoredName(filePath);
    }

    public boolean isApproved() {
        return "APPROVED".equalsIgnoreCase(status) || "DONE".equalsIgnoreCase(status);
    }

    public boolean isRevise() {
        return "REVISE".equalsIgnoreCase(status);
    }

    public boolean isRejected() {
        return "REJECTED".equalsIgnoreCase(status);
    }

    public boolean isPending() {
        return "SUBMITTED".equalsIgnoreCase(status);
    }
}
