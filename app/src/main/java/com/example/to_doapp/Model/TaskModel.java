package com.example.to_doapp.Model;

public class TaskModel {
    private String id;
    private String name;
    private Boolean status;
    private String date;
    private String time;

    public TaskModel() {
    }

    public TaskModel(String id, String name, Boolean status, String date, String time) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.date = date;
        this.time = time;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
