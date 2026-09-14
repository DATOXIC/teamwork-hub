package com.teamwork.business;

import java.io.Serializable;
import java.util.Objects;

/**
 * Khóa chính tổng hợp (Composite Primary Key) cho thực thể ProjectMember trong JPA.
 */
public class ProjectMemberId implements Serializable {

    private static final long serialVersionUID = 1L;

    private int projectId;
    private int userId;

    public ProjectMemberId() {}

    public ProjectMemberId(int projectId, int userId) {
        this.projectId = projectId;
        this.userId = userId;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectMemberId that = (ProjectMemberId) o;
        return projectId == that.projectId && userId == that.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, userId);
    }
}
