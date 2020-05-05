package com.github.bagasala.ormlite.services;

import com.github.bagasala.Bot;
import com.github.bagasala.ormlite.models.Group;
import com.github.bagasala.ormlite.models.UserDb;

import java.sql.SQLException;

public class GroupService {
    public static Group getGroupById(int id) throws SQLException {
        for(Group g: Bot.groupDao.queryForAll()){
            if(g.getId() == id){
                return g;
            }
        }
        return null;
    }

    public static Group getByUser(UserDb u) throws SQLException {
        for(Group g: Bot.groupDao.queryForAll()){
            if(u.getGroup().getId() == g.getId()){
                return g;
            }
        }
        return null;
    }

    public static Group getGroupByName(String name) throws SQLException {
        for(Group g: Bot.groupDao.queryForAll()){
            if(g.getGroupName().equalsIgnoreCase(name)){
                return g;
            }
        }
        return null;
    }
}