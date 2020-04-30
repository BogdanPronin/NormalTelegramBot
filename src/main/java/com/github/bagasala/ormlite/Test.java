package com.github.bagasala.ormlite;


import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

public class Test {
    public static void main(String[] args) throws SQLException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime startDate = LocalTime.of(16,49);
        String endDate = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).toString();
    }
}
