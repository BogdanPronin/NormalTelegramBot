package com.github.bagasala.ormlite.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Objects;

@DatabaseTable(tableName = "teacher_subject")
public class TeacherSubject {
    @DatabaseField(generatedId = true)
    private int id;
    @DatabaseField(foreign = true,foreignAutoRefresh = true,foreignAutoCreate = false)
    private UserDb user;
    @DatabaseField(foreign = true,foreignAutoRefresh = true,foreignAutoCreate = false)
    private Subject subject;


    public TeacherSubject() {
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public UserDb getUser() {
        return user;
    }

    public void setUser(UserDb user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "TeacherSubject{" +
                "id=" + id +
                ", user=" + user +
                ", subject=" + subject +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeacherSubject that = (TeacherSubject) o;
        return id == that.id &&
                Objects.equals(user, that.user) &&
                Objects.equals(subject, that.subject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, user, subject);
    }
}
