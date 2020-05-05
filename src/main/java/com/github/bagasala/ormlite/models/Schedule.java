package com.github.bagasala.ormlite.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Objects;

@DatabaseTable(tableName = "schedule")
public class Schedule {
    @DatabaseField(generatedId = true)
    private int id;
    @DatabaseField
    private String startOfTheLesson;
    @DatabaseField
    private String endOfTheLesson;
    @DatabaseField
    private Days day;
    @DatabaseField
    private int cabinet;
    @DatabaseField(foreign = true,foreignAutoRefresh = true,foreignAutoCreate = false)
    private Group group;
    @DatabaseField(foreign = true,foreignAutoRefresh = true,foreignAutoCreate = false)
    private Subject subject;
    @DatabaseField
    private int serial_number;

    public int getSerial_number() {
        return serial_number;
    }

    public void setSerial_number(int serialNumber) {
        this.serial_number = serialNumber;
    }

    public Schedule() {
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Schedule(int id, String startOfTheLesson, String endOfTheLesson, Days day, int cabinet, Group group, Subject subject, int serial_number) {
        this.id = id;
        this.startOfTheLesson = startOfTheLesson;
        this.endOfTheLesson = endOfTheLesson;
        this.day = day;
        this.cabinet = cabinet;
        this.group = group;
        this.subject = subject;
        this.serial_number = serial_number;
    }

    public Schedule(String startOfTheLesson, String endOfTheLesson, Days day, int cabinet, Group group) {
        this.startOfTheLesson = startOfTheLesson;
        this.endOfTheLesson = endOfTheLesson;
        this.day = day;
        this.cabinet = cabinet;
        this.group = group;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStartOfTheLesson() {
        return startOfTheLesson;
    }

    public void setStartOfTheLesson(String startOfTheLesson) {
        this.startOfTheLesson = startOfTheLesson;
    }

    public String getEndOfTheLesson() {
        return endOfTheLesson;
    }

    public void setEndOfTheLesson(String endOfTheLesson) {
        this.endOfTheLesson = endOfTheLesson;
    }

    public Days getDay() {
        return day;
    }

    public void setDay(Days day) {
        this.day = day;
    }

    public int getCabinet() {
        return cabinet;
    }

    public void setCabinet(int cabinet) {
        this.cabinet = cabinet;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    @Override
    public String toString() {
        return
                serial_number+". "+ subject+" "+startOfTheLesson + " - "+ endOfTheLesson;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Schedule schedule = (Schedule) o;
        return id == schedule.id &&
                cabinet == schedule.cabinet &&
                serial_number == schedule.serial_number &&
                Objects.equals(startOfTheLesson, schedule.startOfTheLesson) &&
                Objects.equals(endOfTheLesson, schedule.endOfTheLesson) &&
                day == schedule.day &&
                Objects.equals(group, schedule.group) &&
                Objects.equals(subject, schedule.subject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, startOfTheLesson, endOfTheLesson, day, cabinet, group, subject, serial_number);
    }
}
