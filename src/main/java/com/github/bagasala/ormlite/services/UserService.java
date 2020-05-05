package com.github.bagasala.ormlite.services;

import com.github.bagasala.Bot;
import com.github.bagasala.ormlite.models.Group;
import com.github.bagasala.ormlite.models.UserDb;

import java.sql.SQLException;

public class UserService {
    public static UserDb getByLName(String lname) throws SQLException {
        for(UserDb u: Bot.userDao.queryForAll()){
            if(u.getLname()!=null) {
                if (u.getLname().equalsIgnoreCase(lname)) {
                    return u;
                }
            }
        }
        return null;
    }

    public static boolean isUserGroup(Group group, UserDb u) throws SQLException {
        return u.getGroup().getId() == group.getId();
    }
}