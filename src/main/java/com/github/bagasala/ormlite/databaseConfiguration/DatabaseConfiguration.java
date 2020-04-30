package com.github.bagasala.ormlite.databaseConfiguration;


import com.github.bagasala.ormlite.models.*;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

public class DatabaseConfiguration {
    public static ConnectionSource connectionSource;
    static {
        try{
            connectionSource = new JdbcConnectionSource("jdbc:sqlite:C:/Users/DrBodan/Desktop/mncDB.db");
            TableUtils.createTableIfNotExists(connectionSource, UserDb.class);
            TableUtils.createTableIfNotExists(connectionSource, Subject.class);
            TableUtils.createTableIfNotExists(connectionSource, Group.class);
            TableUtils.createTableIfNotExists(connectionSource, Schedule.class);
            TableUtils.createTableIfNotExists(connectionSource, TeacherSubject.class);
           // TableUtils.createTableIfNotExists(connectionSource, UserGroup.class);
            TableUtils.createTableIfNotExists(connectionSource, Attendance.class);
            TableUtils.createTableIfNotExists(connectionSource, Controls.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
