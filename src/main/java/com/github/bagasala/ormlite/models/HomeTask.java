package com.github.bagasala.ormlite.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@DatabaseTable(tableName = "home_task")
public class HomeTask {
    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField
    private String filePath;

    @DatabaseField
    private String description;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private Subject subject;

    @DatabaseField
    private String date;

    @DatabaseField
    private String deadLine;

    public static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("YYYY MM dd");

    public HomeTask(){}

    public HomeTask(int id, String filePath, String description, Subject subject, LocalDate date, LocalDate deadLine) {
        this.id = id;
        this.filePath = filePath;
        this.description = description;
        this.subject = subject;
        this.date = date.format(dateTimeFormatter);
        this.deadLine = deadLine.format(dateTimeFormatter);
    }

    public HomeTask(int id, String filePath, String description, Subject subject, String date, String deadLine) {
        this.id = id;
        this.filePath = filePath;
        this.description = description;
        this.subject = subject;
        this.date = date;
        this.deadLine = deadLine;
    }

    @Override
    public String toString() {
        return "Hometask{" +
                "id=" + id +
                ", filePath='" + filePath + '\'' +
                ", description='" + description + '\'' +
                ", subject=" + subject.getId() +
                '}';
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDeadLine() {
        return deadLine;
    }

    public void setDeadLine(String deadLine) {
        this.deadLine = deadLine;
    }
}
