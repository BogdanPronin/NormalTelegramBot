package com.github.bagasala;

import com.github.bagasala.ormlite.databaseConfiguration.DatabaseConfiguration;
import com.github.bagasala.ormlite.models.Controls;
import com.github.bagasala.ormlite.models.Group;
import com.github.bagasala.ormlite.models.UserDb;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static com.github.bagasala.Bot.notification;

public class ControlsNotificationThread extends Thread{
    Dao<UserDb, Integer> userDao = DaoManager.createDao(DatabaseConfiguration.connectionSource, UserDb.class);
    Dao<Controls,Integer> controlsDao = DaoManager.createDao(DatabaseConfiguration.connectionSource,Controls.class);

    public ControlsNotificationThread() throws SQLException {
    }

    @Override
    public void run() {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            String notificationTime = LocalTime.of(14,58).format(formatter);
            while (true) {
                String currentTime = LocalTime.now().format(formatter);
                if (currentTime.equals(notificationTime)) {

                    for (UserDb userDb : userDao.queryForAll()) {
                        String chat_id = userDb.getChat_id();
                        if (getControls(LocalDate.now().plusDays(1),userDb.getGroup())!="") {
                            try {
                                notification(chat_id, getControls(LocalDate.now().plusDays(1),userDb.getGroup()));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                notification(chat_id, "На завтра работ не планируется");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                    sleep(86400000);
                }
            }
        } catch (SQLException | InterruptedException throwables) {
            throwables.printStackTrace();
        }

    }
    public String getControls(LocalDate date, Group group){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String s="";
        for(Controls control: controlsDao){
            if(control.getGroup().equals(group)) {
                LocalDate parsedDate = LocalDate.parse(control.getDate(), formatter);
                date = LocalDate.parse(date.toString(), formatter);
                if (parsedDate.equals(date)) {
                    s+=control.toString();
                }
            }
        }
        return s;
    }
}
