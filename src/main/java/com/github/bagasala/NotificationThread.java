package com.github.bagasala;

import com.github.bagasala.ormlite.databaseConfiguration.DatabaseConfiguration;
import com.github.bagasala.ormlite.models.UserDb;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static com.github.bagasala.Bot.notification;

public class NotificationThread extends Thread {
    private static Logger l = LoggerFactory.getLogger(Bot.class);

    @Override
    public void run() {
        try {
            Dao<UserDb, Integer> userDao = DaoManager.createDao(DatabaseConfiguration.connectionSource, UserDb.class);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            String notificationTime = LocalTime.of(14, 05).format(formatter);
            while (true){
                String currentTime = LocalTime.now().format(formatter);
                 if (currentTime.equals(notificationTime)) {
                    for (UserDb userDb : userDao.queryForAll()) {
                        String chat_id = userDb.getChat_id();
                        try {
                            notification(chat_id, "Собери Рюкзак");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    sleep(86400000);
                }
            }
        } catch (SQLException | InterruptedException throwables) {
            throwables.printStackTrace();
        }
    }
}
