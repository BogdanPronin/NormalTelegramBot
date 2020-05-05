package com.github.bagasala;

import com.github.bagasala.ormlite.databaseConfiguration.DatabaseConfiguration;
import com.github.bagasala.ormlite.models.Days;
import com.github.bagasala.ormlite.models.Group;
import com.github.bagasala.ormlite.models.Schedule;
import com.github.bagasala.ormlite.models.UserDb;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import static com.github.bagasala.Bot.notification;
import static com.github.bagasala.Bot.scheduleDao;

public class NotificationThread extends Thread {
    private static Logger l = LoggerFactory.getLogger(Bot.class);

    @Override
    public void run() {
        try {
            Dao<UserDb, Integer> userDao = DaoManager.createDao(DatabaseConfiguration.connectionSource, UserDb.class);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            String notificationTime = LocalTime.of(21,00).format(formatter);
            while (true){
                String currentTime = LocalTime.now().format(formatter);
                 if (currentTime.equals(notificationTime)) {
                    for (UserDb userDb : userDao.queryForAll()) {
                        String chat_id = userDb.getChat_id();
                        try {
                            notification(chat_id, "Собери Рюкзак. Вот расписание на завтра:");
                            for(Schedule schedule: getGroupSchedule(userDb.getGroup(),LocalDate.now().plusDays(1).getDayOfWeek())){
                                notification(chat_id,schedule.toString());
                            }
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
    public String getStringGroupSchedule(ArrayList<Schedule> schedules){
        String s = "";
        for(Schedule schedule: schedules){
            s+=schedule.toString()+"\n";

        }
        return s;
    }
    public ArrayList<Schedule> getGroupSchedule(Group group, DayOfWeek day) throws SQLException {
        ArrayList<Schedule> scheduleArrayList = new ArrayList<>();
        for(Schedule schedule:scheduleDao.queryForAll()){
            if(schedule.getGroup().equals(group) && schedule.getDay().toString().equals(day.toString())){
                scheduleArrayList.add(schedule);
            }
        }
        return scheduleArrayList;
    }

}
