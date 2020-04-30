package com.github.bagasala.ormlite.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "userDb")
public class UserDb {
    @DatabaseField(generatedId = true)
    private int id;
    @DatabaseField
    private String fname;
    @DatabaseField
    private String lname;
    @DatabaseField
    private String fatherName;
    @DatabaseField(unique = true)
    private String phone;
    @DatabaseField(foreign = true,foreignAutoRefresh = true,foreignAutoCreate = false)
    private Group group;
    @DatabaseField(unique = true)
    private String email;
    @DatabaseField
    private Role role;
    @DatabaseField(unique = true)
    private String chat_id;

    public String getChat_id() {
        return chat_id;
    }

    public void setChat_id(String chat_id) {
        this.chat_id = chat_id;
    }

    public UserDb() {
    }

    public UserDb(String fname, String lname, String fatherName, String phone, Group group, String email, Role role) {
        this.fname = fname;
        this.lname = lname;
        this.fatherName = fatherName;
        this.phone = phone;
        this.group = group;
        this.email = email;
        this.role = role;
    }

    public UserDb(String fname, String lname, String phone, Group group, Role role) {
        this.fname = fname;
        this.lname = lname;
        this.phone = phone;
        this.group = group;
        this.role = role;
    }

    public String getFatherName() {
        return fatherName;
    }

    public void setFatherName(String fatherName) {
        this.fatherName = fatherName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFname() {
        return fname;
    }

    public void setFname(String fname) {
        this.fname = fname;
    }

    public String getLname() {
        return lname;
    }

    public void setLname(String lname) {
        this.lname = lname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }


    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }





    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

}
