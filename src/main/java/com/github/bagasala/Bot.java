package com.github.bagasala;

import com.github.bagasala.ormlite.NewException;
import com.github.bagasala.ormlite.databaseConfiguration.DatabaseConfiguration;
import com.github.bagasala.ormlite.models.*;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.LoggerFactory;
import org.json.JSONObject;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendDocument;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.telegram.telegrambots.api.objects.Contact;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

public class Bot extends TelegramLongPollingBot {
    private static Logger l = LoggerFactory.getLogger(Bot.class);
    Dao<UserDb, Integer> userDao = DaoManager.createDao(DatabaseConfiguration.connectionSource, UserDb.class);

    Dao<Subject,Integer> subjectDao = DaoManager.createDao(DatabaseConfiguration.connectionSource,Subject.class);
    Dao<Schedule,Integer> scheduleDao = DaoManager.createDao(DatabaseConfiguration.connectionSource,Schedule.class);
    Dao<Group,Integer> groupDao = DaoManager.createDao(DatabaseConfiguration.connectionSource,Group.class);
    Dao<TeacherSubject,Integer> teacherSubjectsDao = DaoManager.createDao(DatabaseConfiguration.connectionSource, TeacherSubject.class);
    Dao<Attendance,Integer> attendanceDao = DaoManager.createDao(DatabaseConfiguration.connectionSource,Attendance.class);
    Dao<Controls,Integer> controlsDao = DaoManager.createDao(DatabaseConfiguration.connectionSource,Controls.class);
    Map<String,Boolean> buttonControlIsPressed = new HashMap<>();
    public boolean buttonScheduleIsPressed=false;
    public static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public Bot() throws SQLException {
    }

    public static void main(String[] args) throws SQLException, IOException {
        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        Message message = new Message();
        NotificationThread thread = new NotificationThread();
        ControlsNotificationThread thread1 = new ControlsNotificationThread();
        thread1.start();
        thread.start();
        try {
            telegramBotsApi.registerBot(new Bot());

        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
        }
    }


    public void onUpdateReceived(Update update) {
    SendMessage sendMessage = new SendMessage();
        Message message = update.getMessage();
        try{
            if(update.hasMessage()){
                checkUserInBase(update);
                checkIfMessageEqualsSchedule(update);
                checkIfMessageEqualsControls(update);
            }
        }catch (NewException e){
            sendMessage.setChatId(e.getChat_id())
                    .setText(e.getMessage());
            try {
                sendMessage(sendMessage);
            } catch (TelegramApiException telegramApiException) {
                telegramApiException.printStackTrace();
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

    }

    public void checkUserInBase(Update update) throws SQLException {
        if(userInBaseByChatId(update.getMessage().getChatId().toString())){
            return;
        }
        if(update.getMessage().getContact()==null){
            throw new NewException("Поделитесь своими контактами, чтобы мы проверили, есть ли вы в базе",update.getMessage().getChatId());
        }
        else {
            boolean userInBase = false;
            for(UserDb user:userDao.queryForAll()){
                if(user.getPhone().equals(update.getMessage().getContact().getPhoneNumber())){
                    userInBase = true;
                    user.setChat_id(update.getMessage().getChatId().toString());
                    userDao.update(user);
                    return;
                }
            }
            throw new NewException("Вас нет в базе \uD83D\uDE15",update.getMessage().getChatId());
        }
    }
    public Boolean userInBaseByChatId(String chat_id) throws SQLException {
        for(UserDb user: userDao.queryForAll()){
            if(user.getChat_id().equals(chat_id)){
                return true;
            }
        }
        return false;
    }
    public static void notification(String chat_id,String message) throws IOException {
        String urlString = "https://api.telegram.org/bot"+getStaticBotToken()+"/sendMessage?chat_id="+chat_id+"&text="+message;

        String apiToken =getStaticBotToken();
        String chatId = "@StudyControlBot";
        String text = "Hello world!";

        urlString = String.format(urlString, apiToken, chatId, text);

        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();

        StringBuilder sb = new StringBuilder();
        InputStream is = new BufferedInputStream(conn.getInputStream());
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String inputLine = "";
        while ((inputLine = br.readLine()) != null) {
            sb.append(inputLine);
        }
        String response = sb.toString();
    }

    public void checkIfMessageEqualsSchedule(Update update) throws SQLException {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message_text = update.getMessage().getText();
            long chat_id = update.getMessage().getChatId();
            String photo_name = null;
            Days day = null;
            SendMessage message = new SendMessage();
            Contact contact = update.getMessage().getContact();
            if (message_text.equals("/schedule")) {
                setScheduleButtons(message,chat_id);
                buttonScheduleIsPressed = true;
                try {
                    sendMessage(message); // Sending our message object to user
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }else if (buttonScheduleIsPressed){
                if (message_text.equals("Понедельник")) {
                    photo_name = "monday.jpg";
                    day = Days.MONDAY;
                }else if(message_text.equals("Вторник")){
                    photo_name = "tuesday.jpg";
                    day = Days.TUESDAY;
                } else if(message_text.equals("Среда")){
                    photo_name = "wednesday.jpg";
                    day = Days.WEDNESDAY;
                }else if(message_text.equals("Четверг")){
                    photo_name = "thursday.jpg";
                    day = Days.THURSDAY;
                }else if(message_text.equals("Пятница")){
                    photo_name = "friday.jpg";
                    day = Days.FRIDAY;
                }else if(message_text.equals("Суббота")){
                    photo_name = "saturday.jpg";
                    day = Days.SATURDAY;
                }

                if(photo_name!=null){
                    SendPhoto msg = new SendPhoto()
                            .setChatId(chat_id)
                            .setNewPhoto(new File(getSchedulePath()+photo_name))
                            .setCaption(photo_name);
                    SendMessage sendMessage = new SendMessage()
                            .setText(getStringGroupSchedule(getGroupSchedule(getGroupByChatId(update.getMessage().getChatId().toString()),day)))
                            .setChatId(chat_id);
                    try {
                        sendPhoto(msg); // Call method to send the photo
                        sendMessage(sendMessage);
                        hideKeyboard(chat_id);
                        buttonScheduleIsPressed=false;
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }else if (message_text.equals("/hide") || message_text.equals("❌ Закрыть ❌") || message_text.equals("Закрыть")) {
                    SendMessage msg = new SendMessage()
                            .setChatId(chat_id)
                            .setText("Keyboard hidden");
                    ReplyKeyboardRemove keyboardMarkup = new ReplyKeyboardRemove();
                    msg.setReplyMarkup(keyboardMarkup);
                    buttonScheduleIsPressed=false;
                    try {
                        sendMessage(msg); // Call method to send the photo
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }

            }


        }
    }
    public String getStringGroupSchedule(ArrayList<Schedule> schedules){
        String s = "";
        for(Schedule schedule: schedules){
            s+=schedule.toString()+"\n";

        }
        return s;
    }
    public ArrayList<Schedule> getGroupSchedule(Group group,Days day) throws SQLException {
        ArrayList<Schedule> scheduleArrayList = new ArrayList<>();
        for(Schedule schedule:scheduleDao.queryForAll()){
            if(schedule.getGroup().equals(group) && schedule.getDay()==day){
                scheduleArrayList.add(schedule);
            }
        }
        return scheduleArrayList;
    }
    public void setScheduleButtons(SendMessage message,long chat_id){
        message.setChatId(chat_id)
                .setText("Выберите день недели");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("Понедельник");
        keyboard.add(row);

        row = new KeyboardRow();
        row.add("Вторник");
        keyboard.add(row);

        row = new KeyboardRow();
        row.add("Среда");
        keyboard.add(row);

        row = new KeyboardRow();
        row.add("Четверг");
        keyboard.add(row);

        row = new KeyboardRow();
        row.add("Пятница");
        keyboard.add(row);

        row = new KeyboardRow();
        row.add("Суббота");
        keyboard.add(row);

        row = new KeyboardRow();
        row.add("❌ Закрыть ❌");
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);
    }
    public void hideKeyboard(long chat_id){
        SendMessage msg = new SendMessage()
                .setChatId(chat_id)
                .setText("Keyboard hidden");
        ReplyKeyboardRemove keyboardMarkup = new ReplyKeyboardRemove();
        msg.setReplyMarkup(keyboardMarkup);
        try {
            sendMessage(msg); // Call method to send the photo
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void checkIfMessageEqualsControls(Update update) throws SQLException {
        String message_text = update.getMessage().getText();
        String chat_id = update.getMessage().getChatId().toString();
        SendMessage message = new SendMessage();
        Contact contact = update.getMessage().getContact();
        LocalDate startDate = null;
        LocalDate endDate = null;

        if (message_text.equals("/controls")) {
            buttonControlIsPressed.put(chat_id,true);
            message.setChatId(chat_id)
                    .setText("Выберите период работ:");
            setControlButtons(message,update.getMessage().getChatId());
            try {
                    sendMessage(message); // Sending our message object to user
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }else if (buttonControlIsPressed.get(chat_id)){
                if (message_text.equals("Работы на этой неделе")) {
                    startDate = LocalDate.now();
                    endDate = LocalDate.now().with(DayOfWeek.SATURDAY);
                }else if(message_text.equals("Работы на следующей неделе")){
                    startDate = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
                    endDate = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
                }else if(message_text.equals("Все будущие работы")){
                    startDate = LocalDate.now();
                    endDate = LocalDate.now().plusMonths(3);
                }

                if(startDate!=null && endDate!=null){
                    buttonControlIsPressed.put(chat_id,false);
                    hideKeyboard(update.getMessage().getChatId());
                    throw new NewException(getStringControls(getControls(startDate,endDate,getGroupByChatId(chat_id))),update.getMessage().getChatId());
                }
            }else if (message_text.equals("/hide") || message_text.equals("❌ Закрыть ❌") || message_text.equals("Закрыть")) {
                SendMessage msg = new SendMessage()
                        .setChatId(chat_id)
                        .setText("Keyboard hidden");
                ReplyKeyboardRemove keyboardMarkup = new ReplyKeyboardRemove();
                msg.setReplyMarkup(keyboardMarkup);
                buttonControlIsPressed.put(chat_id,false);
                try {
                    sendMessage(msg); // Call method to send the photo
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

    }
    public void setControlButtons(SendMessage message,long chat_id){
        message.setChatId(chat_id)
                .setText("Выберите неделю");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("Работы на этой неделе");
        keyboard.add(row);

        row = new KeyboardRow();
        row.add("Работы на следующей неделе");
        keyboard.add(row);

        row = new KeyboardRow();
        row.add("Все будущие работы");
        keyboard.add(row);

        row = new KeyboardRow();
        row.add("❌ Закрыть ❌");
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);
    }
    public ArrayList<Controls> getControls(LocalDate start, LocalDate end,Group group){
        ArrayList<Controls> controls = new ArrayList<>();
        for(Controls control: controlsDao){
            if(control.getGroup().equals(group)) {
                LocalDate parsedDate = LocalDate.parse(control.getDate(), formatter);
                l.info(start + " " + parsedDate + " " + end);
                start = LocalDate.parse(start.toString(), formatter);
                end = LocalDate.parse(end.toString(), formatter);
                if (parsedDate.isBefore(end) && parsedDate.isAfter(start) || parsedDate.equals(start)|| parsedDate.equals(end)) {
                    controls.add(control);
                }
            }
        }
        return controls;
    }
    public String getStringControls(ArrayList<Controls> controls){
        ArrayList<String> list = new ArrayList<>();
        String s="";
        for(Controls control:controls){
            s=control.getDate()+" "+control.getType()+" "+control.getSubject()+"\n";
            list.add(s);
        }
        list.sort(ALPHABETICAL_ORDER);
        s="";
        for(int i = 0; i<list.size();i++){
            s+=list.get(i);
        }
        if(s == ""){
            return "Работ пока не предвидится \uD83D\uDE42";
        }
        return s;
    }
    private static Comparator<String> ALPHABETICAL_ORDER = new Comparator<String>() {
        public int compare(String str1, String str2) {
            int res = String.CASE_INSENSITIVE_ORDER.compare(str1, str2);
            if (res == 0) {
                res = str1.compareTo(str2);
            }
            return res;
        }
    };
    public Group getGroupByChatId(String chat_id) throws SQLException {
        for(UserDb user :userDao.queryForAll()){
            if(user.getChat_id().equals(chat_id)){
                return user.getGroup();
            }
        }
        return null;
    }

    public void setButtons(SendMessage sendMessage) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboardRowList = new ArrayList<>();
        KeyboardRow keyboardFirstRow = new KeyboardRow();

        keyboardFirstRow.add(new KeyboardButton("/help"));
        keyboardFirstRow.add(new KeyboardButton("/schedule"));
        keyboardRowList.add(keyboardFirstRow);
        replyKeyboardMarkup.setKeyboard(keyboardRowList);

    }
    public void sendMsg(Message message, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        sendMessage.setChatId(message.getChatId().toString());

        sendMessage.setText(text);
        try {
            setButtons(sendMessage);
            sendMessage(sendMessage);

        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void download(Message message, String file_name){
        SendDocument sendDocument = new SendDocument();
        sendDocument.setNewDocument(new File("C:\\Users\\DrBodan\\Desktop\\"+file_name));
        sendDocument.setChatId(message.getChatId());
        sendDocument.setCaption(file_name);
        try {
            sendDocument(sendDocument);
            sendMsg(message, "success output");
            l.info("@@@\tsent file "+file_name);
        } catch (TelegramApiException e) {
            sendMsg(message, "fail output");
        }
    }
    public void upload(Message message){
        String file_id = message.getDocument().getFileId();
        try {
            uploadFile(message.getDocument().getFileName(), file_id);
            sendMsg(message, "success");
            l.info("File successfully downloaded");
        } catch (IOException e) {
            sendMsg(message,"fail");
            l.warn("Something goes wrong, check your code or connection");
        }
    }
    public void uploadFile(String file_name, String file_id) throws IOException{
        l.info("@@@\tdownloading file " + file_name + "to server");
        URL url = new URL("https://api.telegram.org/bot"+getBotToken()+"/getFile?file_id="+file_id);
        BufferedReader in = new BufferedReader(new InputStreamReader( url.openStream()));
        String res = in.readLine();
        JSONObject jresult = new JSONObject(res);
        JSONObject path = jresult.getJSONObject("result");
        String file_path = path.getString("file_path");
        URL downoload = new URL("https://api.telegram.org/file/bot" + getBotToken() + "/" + file_path);
        String upPath = "C:\\Users\\DrBodan\\Desktop\\";
        FileOutputStream fos = new FileOutputStream(upPath + file_name);
        l.info("@@@\tstart downloading...");
        ReadableByteChannel rbc = Channels.newChannel(downoload.openStream());
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();
        rbc.close();
        l.info("@@@\tfile downloaded on server(success)");
    }

    public String getBotUsername() {
        return "@StudyControlBot";
    }
    public static String getStaticBotToken(){
        return "1213409409:AAEpGc8wuxiF-TRbdKFmyZEy7ltsfFnuBfo";
    }
    public String getBotToken() {
        return "1213409409:AAEpGc8wuxiF-TRbdKFmyZEy7ltsfFnuBfo";
    }
    public String getSchedulePath(){
        return "C:\\Users\\DrBodan\\Desktop\\schedule\\";
    }

}
