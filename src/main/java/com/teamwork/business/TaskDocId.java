package com.teamwork.business;

import java.io.Serializable;
import java.util.Objects;

/**
 * Khóa chính tổng hợp (Composite Primary Key) cho thực thể TaskDoc trong JPA.
 */
public class TaskDocId implements Serializable {

    private static final long serialVersionUID = 1L;

    private int taskId;
    private int docId;

    public TaskDocId() {}

    public TaskDocId(int taskId, int docId) {
        this.taskId = taskId;
        this.docId = docId;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public int getDocId() {
        return docId;
    }

    public void setDocId(int docId) {
        this.docId = docId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskDocId that = (TaskDocId) o;
        return taskId == that.taskId && docId == that.docId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, docId);
    }
}
