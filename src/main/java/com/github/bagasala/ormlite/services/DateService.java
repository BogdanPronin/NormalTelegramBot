package com.github.bagasala.ormlite.services;

import com.github.bagasala.ormlite.NewException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateService {
    public static void isValidDate(String text, long chatId){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy MM dd");
        try {
            LocalDate parsedDate = LocalDate.parse(text, formatter);
        }catch (DateTimeParseException e){
            throw new NewException("Неправильный формат введенной даты, повторите попытку.",chatId);
        }
    }
}
