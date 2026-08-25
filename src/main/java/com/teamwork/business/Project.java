package com.teamwork.business;
import java.io.Serializable;
public class Project implements Serializable
{
    private int id;
    private String name;
    private String description;
    private int ownerId;
    private String createdAt;
    private int totalTasks;
    private int doneTasks;

    public Project()
    {
        this.id=0;
        this.name="";
        this.description="";
        this.ownerId=0;
        this.createdAt="";
        this.totalTasks=0;
        this.doneTasks=0;
    }

    // Tiến độ công việc
    public int getProgressPercentage()
    {
        if( totalTasks == 0) return 0;
        return (int) Math.round(((double) doneTasks / totalTasks) * 100);
    }

        // Constructor đầy đủ tham số để khởi tạo nhanh (Seed data)
    public Project(int id, String name, String description, int ownerId, String createdAt, int totalTasks, int doneTasks) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.totalTasks = totalTasks;
        this.doneTasks = doneTasks;
    }

    // --- GETTERS & SETTERS (Bắt buộc để Jakarta EL đọc được dữ liệu) ---
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public int getOwnerId() {
        return ownerId;
    }
    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public String getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public int getTotalTasks() {
        return totalTasks;
    }
    public void setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
    }

    public int getDoneTasks() {
        return doneTasks;
    }
    public void setDoneTasks(int doneTasks) {
        this.doneTasks = doneTasks;
    }
}

