package com.github.bagasala.ormlite;


import com.github.bagasala.ormlite.models.Days;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

public class Test {
    public static void main(String[] args) throws SQLException {
        System.out.println(LocalDate.now().getDayOfWeek().toString().equals(Days.MONDAY.toString()));
    }
}
