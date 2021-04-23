package com.github.bagasala;

import com.github.bagasala.ormlite.NewException;
import com.github.bagasala.ormlite.databaseConfiguration.DatabaseConfiguration;
import com.github.bagasala.ormlite.models.*;
import com.github.bagasala.ormlite.services.*;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.LoggerFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

import static com.github.bagasala.ormlite.services.DateService.isValidDate;

public class Bot extends TelegramLongPollingBot {
    private static Logger l = LoggerFactory.getLogger(Bot.class);
    public static Dao<HomeTask, Integer> homeTaskDao;
    public static Dao<Subject,Integer> subjectDao;
    public static Dao<UserDb,Integer> userDao;
    public static Dao<Group, Integer> groupDao;
    public static Dao<Schedule, Integer> scheduleDao;
    public static Dao<Controls, Integer> controlsDao;
    public static Dao<Attendance, Integer> attendanceDao;
    private Map<String, Integer> map = new HashMap<>();
    private Map<String, HomeTask> homeTasks = new HashMap<>();
    private Map<String,Boolean> buttonControlIsPressed = new HashMap<>();
    private Map<String,Boolean> buttonScheduleIsPressed = new HashMap<>();
    private Map<String, String> dates = new HashMap<>();
    public static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy MM dd");

    //
    public String getSchedulePath(){
        return getDirPath()+"/schedule/";
    }

    public String getDirPath(){
        return "/Users/bogdan/Desktop/telegramBot";
    }
    //

    static {
        try {
            userDao = DaoManager.createDao(DatabaseConfiguration.connectionSource,UserDb.class);
            subjectDao = DaoManager.createDao(DatabaseConfiguration.connectionSource,Subject.class);
            homeTaskDao = DaoManager.createDao(DatabaseConfiguration.connectionSource, HomeTask.class);
            groupDao = DaoManager.createDao(DatabaseConfiguration.connectionSource, Group.class);
            scheduleDao = DaoManager.createDao(DatabaseConfiguration.connectionSource, Schedule.class);
            controlsDao = DaoManager.createDao(DatabaseConfiguration.connectionSource, Controls.class);
            attendanceDao = DaoManager.createDao(DatabaseConfiguration.connectionSource,Attendance.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
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
                help(update);
                checkUserInBase(update);
                checkIfMessageEqualsSchedule(update);
                checkIfMessageEqualsControls(update);
                addStudentInterface(update);
                addTeacherInterface(update);
                readSchedule(update);
                readControlGraph(update);
                postScheduleHelp(update);
                postControlHelp(update);
                readAttendance(update);
                sendAttendance(update);
                postAttendanceHelp(update);
            }
        }catch (NewException e){
            sendMessage.setChatId(e.getChat_id())
                    .setText(e.getMessage());
            try {
                sendMessage(sendMessage);
            } catch (TelegramApiException telegramApiException) {
                telegramApiException.printStackTrace();
            }
        } catch (SQLException | IOException throwables) {
            throwables.printStackTrace();
        }

    }
    public void help(Update update){
        if(update.hasMessage()){
            Message message = update.getMessage();
            if(message.hasText()){
                if(message.getText().equals("/help") || message.getText().equals("/start")){
                    l.info("@sending help message to client");
                    sendMsg(message, "Данный бот имеет интерфейс как для простого ученика, так для учителя и администратора.\n" +
                            "\n" +
                            "Интерфейс ученика\uD83E\uDD13:\n" +
                            "\n" +
                            "/homework - запрос домашнего задания на введенную дату.\n" +
                            "/controls - запрос контрольных и самостоятельных работ на текущую, следующую неделю и за все время.\n" +
                            "/schedule - просмотр расписания по введенному дню недели.\n" +
                            "\n" +
                            "Интерфейс учителя\uD83D\uDE01:\n" +
                            "\n" +
                            "/posthometask - добавление в базу домашнего задания на дату.\n" +
                            "/postcontrol - добавление в базу графика контрольных работ с помощью Excel файла.\n" +
                            "\n" +
                            "Интерфейс администратора\uD83D\uDE08:\n" +
                            "\n" +
                            "/postschedule - добавление в базу расписания с помощью Excel файла.\n" +
                            "\n" +
                            "Также этот бот\uD83E\uDD16 автоматически оповещает Вас о контрольных работах за день до их проведения. Каждый вечер бот, заботясь о Вас \uD83D\uDE0A, уведомляет о том, что необходимо собрать рюкзак\uD83C\uDF92,и для удобства отправляет расписание на следующий день.");
                }
            }
        }
    }
    public void postScheduleHelp(Update update) throws SQLException {
        if(update.hasMessage()){
            Message message = update.getMessage();
            if(message.hasText()){
                if(message.getText().equals("/postschedulehelp")){
                    checkIfUserTeacherOrAdmin(getUserByChatId(message.getChatId().toString()),message.getChatId());
                    l.info("@sending /postschedulehelp message to client");
                    sendMsg(message, "Если вы не знаете как записать расписание, вот вам шаблон для отправки: \n");
                    download(message,getExelSchedule());
                 }
            }
        }
    }
    public void postControlHelp(Update update) throws SQLException {
        if(update.hasMessage()){
            Message message = update.getMessage();
            if(message.hasText()){
                if(message.getText().equals("/postcontrolhelp")){
                    checkIfUserAdmin(getUserByChatId(message.getChatId().toString()),message.getChatId());
                    l.info("@sending /postcontrolhelp message to client");
                    sendMsg(message, "Если вы не знаете как записать график, вот вам шаблон для отправки: \n");
                    download(message,getExelControl());
                }
            }
        }
    }
    public void postAttendanceHelp(Update update) throws SQLException {
        if(update.hasMessage()){
            Message message = update.getMessage();
            if(message.hasText()){
                if(message.getText().equals("/postattendancehelp")){
                    checkIfUserAdmin(getUserByChatId(message.getChatId().toString()),message.getChatId());
                    l.info("@sending /postattendancehelp message to client");
                    sendMsg(message, "Если вы не знаете как записать таблицу посещаемости, вот вам шаблон для отправки: \n");
                    download(message,getExelControl());
                }
            }
        }
    }
    public String getExelSchedule(){
        return "/Users/bogdan/Desktop/telegramBot/schedule/post_schedule_example.xlsx";
    }
    public String getExelControl(){
        return "/Users/bogdan/Desktop/telegramBot/control/post_controls_example.xlsx";
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
                    throw new NewException("Вы в базе",update.getMessage().getChatId());

                }
            }
            throw new NewException("Вас нет в базе \uD83D\uDE15. Обратитесь к системному администратору вашей школы",update.getMessage().getChatId());
        }
    }
    public Boolean userInBaseByChatId(String chat_id) throws SQLException {
        for(UserDb user: userDao.queryForAll()){
            if(user.getChat_id()!=null){
                if(user.getChat_id().equals(chat_id)){
                    return true;
                }
            }
        }
        return false;
    }
    public UserDb getUserByChatId(String chat_id) throws SQLException {
        for(UserDb user:userDao.queryForAll()){
            if(user.getChat_id()!=null) {
                if (user.getChat_id().equals(chat_id)) {
                    return user;
                }
            }
        }
        return null;
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
            String chId = update.getMessage().getChatId().toString();
            String photo_name = null;
            Days day = null;
            SendMessage message = new SendMessage();
            Contact contact = update.getMessage().getContact();
            if (message_text.equals("/schedule")) {
                setScheduleButtons(message,chat_id);
                buttonScheduleIsPressed.put(chId,true);
                try {
                    sendMessage(message); // Sending our message object to user
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }else if(buttonScheduleIsPressed.get(chId)!=null){
                 if(buttonScheduleIsPressed.get(chId)) {
                    if (message_text.equals("Понедельник")) {
                        photo_name = "monday.jpg";
                        day = Days.MONDAY;
                    } else if (message_text.equals("Вторник")) {
                        photo_name = "tuesday.jpg";
                        day = Days.TUESDAY;
                    } else if (message_text.equals("Среда")) {
                        photo_name = "wednesday.jpg";
                        day = Days.WEDNESDAY;
                    } else if (message_text.equals("Четверг")) {
                        photo_name = "thursday.jpg";
                        day = Days.THURSDAY;
                    } else if (message_text.equals("Пятница")) {
                        photo_name = "friday.jpg";
                        day = Days.FRIDAY;
                    } else if (message_text.equals("Суббота")) {
                        photo_name = "saturday.jpg";
                        day = Days.SATURDAY;
                    }

                    if (photo_name != null) {
                        SendPhoto msg = new SendPhoto()
                                .setChatId(chat_id)
                                .setNewPhoto(new File(getSchedulePath() + photo_name))
                                .setCaption(photo_name);
                        SendMessage sendMessage = new SendMessage()
                                .setText(getStringGroupSchedule(getGroupSchedule(getGroupByChatId(update.getMessage().getChatId().toString()), day)))
                                .setChatId(chat_id);
                        try {
                            sendPhoto(msg); // Call method to send the photo
                            hideKeyboard(chat_id);
                            sendMessage(sendMessage);

                            buttonScheduleIsPressed.put(chId, false);
                            buttonScheduleIsPressed.remove(chId);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    } else if (message_text.equals("/hide") || message_text.equals("❌ Закрыть ❌") || message_text.equals("Закрыть")) {
                        SendMessage msg = new SendMessage()
                                .setChatId(chat_id)
                                .setText("Keyboard hidden");
                        ReplyKeyboardRemove keyboardMarkup = new ReplyKeyboardRemove();
                        msg.setReplyMarkup(keyboardMarkup);
                        buttonScheduleIsPressed.put(chId, false);
                        try {
                            sendMessage(msg); // Call method to send the photo
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                        buttonScheduleIsPressed.remove(chId);
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
                .setText("(Клавиатура спрятана)");
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
        if(message_text!=null) {
            if (message_text.equals("/controls")) {
                buttonControlIsPressed.put(chat_id, true);
                message.setChatId(chat_id)
                        .setText("Выберите период работ:");
                setControlButtons(message, update.getMessage().getChatId());
                try {
                    sendMessage(message); // Sending our message object to user
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (buttonControlIsPressed.containsKey(chat_id)) {
                if (buttonControlIsPressed.get(chat_id)) {
                    if (message_text.equals("Работы на этой неделе")) {
                        startDate = LocalDate.now();
                        endDate = LocalDate.now().with(DayOfWeek.SATURDAY);
                    } else if (message_text.equals("Работы на следующей неделе")) {
                        startDate = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
                        endDate = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
                    } else if (message_text.equals("Все будущие работы")) {
                        startDate = LocalDate.now();
                        endDate = LocalDate.now().plusMonths(3);
                    }

                    if (startDate != null && endDate != null) {
                        buttonControlIsPressed.put(chat_id, false);
                        hideKeyboard(update.getMessage().getChatId());
                        throw new NewException(getStringControls(getControls(startDate, endDate, getGroupByChatId(chat_id))), update.getMessage().getChatId());
                    }
                } else if (message_text.equals("/hide") || message_text.equals("❌ Закрыть ❌") || message_text.equals("Закрыть")) {
                    SendMessage msg = new SendMessage()
                            .setChatId(chat_id);
                    ReplyKeyboardRemove keyboardMarkup = new ReplyKeyboardRemove();
                    msg.setReplyMarkup(keyboardMarkup);
                    buttonControlIsPressed.put(chat_id, false);
                    try {
                        sendMessage(msg); // Call method to send the photo
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                    buttonControlIsPressed.remove(chat_id);
                }
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
            if(user.getChat_id()!=null){
                if(user.getChat_id().equals(chat_id)){
                    return user.getGroup();
                }
            }
        }
        return null;
    }

    public void setButtons(SendMessage sendMessage) {
//        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
//        sendMessage.setReplyMarkup(replyKeyboardMarkup);
//        replyKeyboardMarkup.setSelective(true);
//        replyKeyboardMarkup.setResizeKeyboard(true);
//        replyKeyboardMarkup.setOneTimeKeyboard(false);
//
//        List<KeyboardRow> keyboardRowList = new ArrayList<>();
//        KeyboardRow keyboardFirstRow = new KeyboardRow();
//
//        keyboardFirstRow.add(new KeyboardButton("/help"));
//        keyboardRowList.add(keyboardFirstRow);
//        replyKeyboardMarkup.setKeyboard(keyboardRowList);

    }

    public void checkIfUserAdmin(UserDb user,long chat_id){
        if(user.getRole() != Role.ADMIN){
            throw new NewException("Извините, но эта команда для администратора",chat_id);
        }
    }
    public void checkIfUserTeacherOrAdmin(UserDb user,long chat_id){
        if(user.getRole() == Role.TEACHER || user.getRole() == Role.ADMIN){
            l.info(user.getRole().toString());
        }else throw new NewException("Извините, но эта команда для учителя",chat_id);
    }
    public void setHideButton(SendMessage message, long chat_id){
        message.setChatId(chat_id);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row = new KeyboardRow();
        row.add("❌ Закрыть ❌");
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);
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
    public void setHomeTaskButtons(SendMessage message,long chat_id) throws SQLException {
        message.setChatId(chat_id)
                .setText("Выберете предмет:");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        for(Subject subject: subjectDao.queryForAll()){
            row = new KeyboardRow();
            row.add(subject.getName());
            keyboard.add(row);
        }

        row = new KeyboardRow();
        row.add("❌ Закрыть ❌");
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);
    }
    public void uploadFile(String file_name, String file_id) throws IOException{
        l.info("@@@\tdownloading file " + file_name + " to server");
        URL url = new URL("https://api.telegram.org/bot"+getBotToken()+"/getFile?file_id="+file_id);
        BufferedReader in = new BufferedReader(new InputStreamReader( url.openStream()));
        String res = in.readLine();
        JSONObject jresult = new JSONObject(res);
        JSONObject path = jresult.getJSONObject("result");
        String file_path = path.getString("file_path");
        URL downoload = new URL("https://api.telegram.org/file/bot" + getBotToken() + "/" + file_path);
        String upPath = getDirPath();
        FileOutputStream fos = new FileOutputStream(upPath + file_name);
        l.info("@@@\tstart downloading...");
        ReadableByteChannel rbc = Channels.newChannel(downoload.openStream());
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();
        rbc.close();
        l.info("@@@\tfile downloaded on server(success)");
    }
    public void addTeacherInterface(Update update) throws SQLException, IOException {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            Long chId = message.getChatId();
            String chatId = chId.toString()+"teach";
            if (message.hasText()) {
                if (message.getText().equals("/posthometask")) {
                    checkIfUserTeacherOrAdmin(getUserByChatId(chId.toString()),update.getMessage().getChatId());
                    l.info("@posting hometask initialized, chatId = "+chatId);
                    map.put(chatId, 1);
                    homeTasks.put(chatId, new HomeTask());
                    SendMessage sendMessage = new SendMessage()
                            .setChatId(chId)
                            .setText("Выберете предмет");
                    setHomeTaskButtons(sendMessage,chId);
                    try {
                        sendMessage(sendMessage); // Sending our message object to user
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                } else if (map.containsKey(chatId)) {
                    if (map.get(chatId) == 1) {
                        String text = message.getText();
                        l.info("@getting subject's name from client chatId = "+chatId);
                        if (SubjectService.getSubjectByName(text) != null) {
                            l.info("@setting subject to hometask chatId = "+chatId);
                            homeTasks.get(chatId).setId(1);
                            homeTasks.get(chatId).setSubject(SubjectService.getSubjectByName(text));
                            map.put(chatId, 2);
                            sendMsg(message, "Установлен предмет: " + text + ".\nВведите комментарий к домашнему заданию");
                            hideKeyboard(chId);
                        } else {
                            l.info("client sent wrong subject");
                            sendMsg(message, "Неправильное название предмета, повторите попытку");
                        }
                    } else if (map.get(chatId) == 2) {
                        l.info("@get hometask's description from client chatId = "+chatId);
                        String desc = message.getText();
                        homeTasks.get(chatId).setDescription(desc);
                        sendMsg(message, "Отлично, теперь отправьте нам дату, на которую вы хотите добавить домашнее задание в формате ГГГГ ММ ДД");
                        map.put(chatId, 3);
                    } else if (map.get(chatId) == 3){
                        l.info("@get hometask's date");
                        String date = message.getText();
                        isValidDate(date,update.getMessage().getChatId());
                        l.info("@date is valid, continue");
                        homeTasks.get(chatId).setDate(date);
                        sendMsg(message, "Отлично, теперь отправьте нам файл с домашним заданием");
                        map.put(chatId, 4);
                    } else if (message.getText().equals("/hide") || message.getText().equals("❌ Закрыть ❌") || message.getText().equals("Закрыть")) {
                        SendMessage msg = new SendMessage()
                                .setChatId(message.getChatId())
                                .setText("(клавиатура спрятана)");
                        ReplyKeyboardRemove keyboardMarkup = new ReplyKeyboardRemove();
                        msg.setReplyMarkup(keyboardMarkup);

                        try {
                            sendMessage(msg); // Call method to send the photo
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                        throw new NewException("Команда завершена",chId);
                    }
                }
            } else if (message.hasDocument()) {
                if(map.containsKey(chatId)){
                    if (map.get(chatId) == 4) {
                        l.info("@get file from client chatId = "+chatId);
                        String fileName = message.getDocument().getFileName();
                        String fileId = message.getDocument().getFileId();
                        Runnable task = () -> {
                            try {
                                uploadFile(homeTasks.get(chatId).getSubject().getName() + "\\" + fileName, fileId);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        };
                        Thread thread = new Thread(task);
                        thread.start();
                        l.info("@setting file to hometask");
                        homeTasks.get(chatId).setFilePath(getDirPath() + homeTasks.get(chatId).getSubject().getName() + "\\" + fileName);
                        homeTaskDao.create(homeTasks.get(chatId));
                        l.info("@hometask object was created");
                        map.remove(chatId);
                        homeTasks.remove(chatId);
                        sendMsg(message, "Отлично! Домашнее задание занесено в базу!");
                        l.info("@hometask creating cycle ended");
                    }
                }
            }
        }
    }

    public void readSchedule(Update update) throws IOException, SQLException {
        if(update.hasMessage()){
            Message message = update.getMessage();
            String chatId = message.getChatId().toString()+"sched";
            if(message.hasText()){

                if(message.getText().equals("/postschedule")){
                    checkIfUserAdmin(getUserByChatId(update.getMessage().getChatId().toString()),update.getMessage().getChatId());
                    map.put(chatId, 1);
                    sendMsg(message, "Отправьте нам Excel файл в нужном формате. Для того, чтобы узнать, в каком формате нужно отправлять Excel файл, введите /postschedulehelp");
                    l.info("@posting schedule initialized, chatId = "+chatId);
                } else if(message.getText().equals("/postschedulehelp")){
                    l.info("@sending Excel schedule example to client, chatId = "+chatId);
                    download(message, getDirPath()+"examples\\schedule.xlsx");
                }
            } else if(message.hasDocument()){
                if(map.containsKey(chatId)){
                    if(map.get(chatId) == 1){
                        l.info("@getting file from client, chatId = "+chatId);
                        uploadFile("schedule\\"+message.getDocument().getFileName(), message.getDocument().getFileId());
                        File file = new File(getDirPath()+"schedule\\"+message.getDocument().getFileName());
                        try{
                            scheduleDao.create(parceScheduleExcel(file));
                            sendMsg(message, "Файл был успешно загружен и прочитан");
                        } catch (IndexOutOfBoundsException | NullPointerException e){
                            e.printStackTrace();
                            sendMsg(message,"Ошибка при считывании файла, повторите попытку");
                        } finally {
                            map.remove(chatId);
                        }
                    }
                }
            }
        }
    }

    public void readControlGraph(Update update) throws IOException, SQLException {
        if(update.hasMessage()){
            Message message = update.getMessage();
            String chatId = message.getChatId().toString()+"contr";
            if(message.hasText()){
                if(message.getText().equals("/postcontrol")){
                    checkIfUserTeacherOrAdmin(getUserByChatId(update.getMessage().getChatId().toString()),update.getMessage().getChatId());
                    l.info("@posting controls initialized, chatId = "+chatId);
                    map.put(chatId, 1);
                    sendMsg(message, "Отправьте нам Excel файл в нужном формате. Для того, чтобы узнать, в каком формате нужно отправлять Excel файл, введите /postcontrolhelp");
                } else if(message.getText().equals("/postcontrolhelp")){
                    l.info("@sending Excel control example to client, chatId = "+chatId);
                    download(message, getDirPath()+"examples\\controls.xlsx");
                }
            } else if(message.hasDocument()){
                if(map.containsKey(chatId)){
                    if(map.get(chatId) == 1){
                        l.info("@getting file from client, chatId = "+chatId);
                        uploadFile("controls\\"+message.getDocument().getFileName(), message.getDocument().getFileId());
                        File file = new File(getDirPath()+"controls\\"+message.getDocument().getFileName());
                        try{
                            controlsDao.create(parceControlExcel(file));
                            sendMsg(message, "Файл был успешно загружен и прочитан");
                        } catch (IndexOutOfBoundsException | NullPointerException e){
                            e.printStackTrace();
                            sendMsg(message,"Ошибка при считывании файла, повторите попытку");
                        } finally {
                            map.remove(chatId);
                        }
                    }
                }
            }
        }
    }

    public ArrayList<Schedule> parceScheduleExcel(File excel) throws IOException, SQLException {
        l.info("@@@start parcing excel file");
        ArrayList<Double> startOfLesson = new ArrayList<>();
        ArrayList<Double> endOfLesson = new ArrayList<>();
        ArrayList<String> day = new ArrayList<>();
        ArrayList<Integer> cabinet = new ArrayList<>();
        ArrayList<Integer> group = new ArrayList<>();
        ArrayList<String> subject = new ArrayList<>();
        ArrayList<Integer> serialNum = new ArrayList<>();
        ArrayList<Schedule> schedules = new ArrayList<>();
        Group grp;
        Subject sbjct;
        int position;
        int rowCounter = 1;
        FileInputStream file = new FileInputStream(excel);
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        XSSFSheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.iterator();
        while(rowIterator.hasNext()){
            Row row = rowIterator.next();
            Iterator<Cell> cellIterator = row.cellIterator();
            position = 1;
            while(cellIterator.hasNext()){
                if(rowCounter == 1){
                    rowCounter++;
                    break;
                }
                Cell cell = cellIterator.next();
                if(cell.getCellType() == CellType.NUMERIC){
                    l.info("@@@handling "+cell.getNumericCellValue()+", position = "+position);
                    if(position == 4){
                        cabinet.add((int)cell.getNumericCellValue());
                    } else if(position == 5){
                        group.add((int)cell.getNumericCellValue());
                    } else if(position == 7){
                        serialNum.add((int)cell.getNumericCellValue());
                    } else if(position == 1){
                        startOfLesson.add(cell.getNumericCellValue());
                    } else if(position == 2){
                        endOfLesson.add(cell.getNumericCellValue());
                    }
                } else if(cell.getCellType() == CellType.STRING){
                    l.info("@@@handling "+cell.getStringCellValue()+", position = "+position);
                    if(position == 3){
                        day.add(cell.getStringCellValue());
                    } else if(position == 6){
                        subject.add(cell.getStringCellValue());
                    }
                }
                position ++;
            }
            System.out.println();
        }
        file.close();
        l.info("startOfLesson "+startOfLesson.size()+"\n"
                +"endOfLesson " + endOfLesson.size()+"\n"
                +"day " + day.size() + "\n"
                +"cabinet " + cabinet.size() +"\n"
                +"group " + group.size() + "\n"
                +"subject "+subject.size()+"\n"
                +"serialNum "+serialNum.size());
        for(int i=0;i<cabinet.size();i++){
            grp = GroupService.getGroupById(group.get(i));
            sbjct = SubjectService.getSubjectByName(subject.get(i));
            if(grp == null || sbjct == null){
                throw new NullPointerException();
            }
            schedules.add(new Schedule(i, String.valueOf(startOfLesson.get(i)), String.valueOf(endOfLesson.get(i)),
                    Days.valueOf(day.get(i).toUpperCase()), cabinet.get(i), grp, sbjct, serialNum.get(i)));
        }
        l.info("@@@parcing done");
        return schedules;
    }

    public ArrayList<Controls> parceControlExcel(File excel) throws IOException, SQLException {
        ArrayList<String> type = new ArrayList<>();
        ArrayList<String> date = new ArrayList<>();
        ArrayList<String> subject = new ArrayList<>();
        ArrayList<Integer> group = new ArrayList<>();
        ArrayList<Controls> controls = new ArrayList<>();
        Subject sbjct;
        Group grp;
        FileInputStream fileInputStream = new FileInputStream(excel);
        XSSFWorkbook workbook = new XSSFWorkbook(fileInputStream);
        XSSFSheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.iterator();
        int position, rowCounter = 1;
        while(rowIterator.hasNext()){
            Row row = rowIterator.next();
            Iterator<Cell> cellIterator = row.iterator();
            position = 1;
            while(cellIterator.hasNext()){
                if(rowCounter == 1){
                    rowCounter++;
                    break;
                }
                Cell cell = cellIterator.next();
                if(cell.getCellType() == CellType.NUMERIC){
                    l.info("@@@handling "+cell.getNumericCellValue()+", position = "+position);
                    if(position == 4){
                        group.add((int)cell.getNumericCellValue());
                    }
                } else if(cell.getCellType() == CellType.STRING){
                    l.info("@@@handling "+cell.getStringCellValue()+", position = "+position);
                    if(position == 1){
                        type.add(cell.getStringCellValue());
                    } else if(position == 2){
                        date.add(cell.getStringCellValue());
                    } else if(position == 3){
                        subject.add(cell.getStringCellValue());
                    }
                }
                position ++;
            }
            System.out.println();
        }
        l.info("type "+type.size()+"\n"
                +"date "+date.size()+"\n"
                +"subject "+subject.size()+"\n"
                +"group "+group.size());
        for(int i=0;i<type.size();i++){
            grp = GroupService.getGroupById(group.get(i));
            sbjct = SubjectService.getSubjectByName(subject.get(i));
            if(grp == null || sbjct == null){
                throw new NullPointerException();
            }
            controls.add(new Controls(i, type.get(i), date.get(i), sbjct, grp));
        }
        l.info("@@@parcing done");
        return controls;
    }


    public void addStudentInterface(Update update) throws SQLException {
        if(update.hasMessage()){
            Message message = update.getMessage();
            Long chId = message.getChatId();
            String chatId = chId.toString()+"stud";
            SendMessage sendMessage = new SendMessage();
            if(message.hasText()){
                if(message.getText().equals("/homework")){

                    sendMessage.setChatId(update.getMessage().getChatId())
                            .setText("Выберите предмет:");
                    setHomeTaskButtons(sendMessage,update.getMessage().getChatId());
                    try {
                        sendMessage(sendMessage); // Sending our message object to user
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                    l.info("@getting hometask initialized, chatId = "+chatId);
                    map.put(chatId, 1);
                    homeTasks.put(chatId, new HomeTask());
                } else if (message.getText().equals("/hide") || message.getText().equals("❌ Закрыть ❌") || message.getText().equals("Закрыть")) {
                    SendMessage msg = new SendMessage()
                            .setChatId(message.getChatId())
                            .setText("Keyboard hidden");
                    ReplyKeyboardRemove keyboardMarkup = new ReplyKeyboardRemove();
                    msg.setReplyMarkup(keyboardMarkup);
                    buttonControlIsPressed.put(message.getChatId().toString(),false);
                    map.remove(chatId);
                    try {
                        sendMessage(msg); // Call method to send the photo
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                    return;
                } else if(map.containsKey(chatId)){
                    hideKeyboard(chId);
                    if(map.get(chatId) == 1){
                        l.info("@getting subject name from client, chatId = "+chatId);
                        String text = message.getText();
                        if(SubjectService.getSubjectByName(text) == null){
                            l.info("client sent wrong subject");
                            sendMsg(message, "Неправильное название предмета, повторите попытку");
                            sendMessage.setChatId(update.getMessage().getChatId());
                            setHomeTaskButtons(sendMessage,update.getMessage().getChatId());
                            try {
                                sendMessage(sendMessage); // Sending our message object to user
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                            }
                        } else {
                            l.info("@setting hometask's subject, chatId = "+chatId);
                            homeTasks.get(chatId).setSubject(SubjectService.getSubjectByName(text));
                            homeTasks.get(chatId).setId(1);
                            map.put(chatId, 2);
                            sendMsg(message, "Установлен предмет: "+text+"\nВведите дату домашнего задания в формате ГГГГ ММ ДД");
                            setHideButton(sendMessage,chId);
                        }
                    } else if(map.get(chatId) == 2){
                        l.info("@getting homework's date, chatId = "+chatId);
                        String date = message.getText();
                        isValidDate(date, message.getChatId());
                        l.info("@setting hometask's date, chatId = "+chatId);
                        homeTasks.get(chatId).setDate(date);
                        HomeTask hometask = HometaskService.getByParams(homeTasks.get(chatId).getDate(), homeTasks.get(chatId).getSubject());
                        if(hometask == null){
                            l.info("@hometask isn't exists, try again");
                            sendMsg(message, "Домашки не существует, повторите попытку снова");
                        } else {
                            sendMsg(message, "Отлично, отправляем домашнее задание!");
                            Runnable task = () -> {
                                download(message, hometask.getFilePath());
                            };
                            Thread thread = new Thread(task);
                            thread.start();
                            sendMsg(message, "комментарии учителя: "+hometask.getDescription());
                            homeTasks.remove(chatId);
                            map.remove(chatId);
                            l.info("@hometask sending cycle ended");
                        }
                    }
                }
            }
        }
    }
    public void download(Message message, String file_name){
        SendDocument sendDocument = new SendDocument();
        sendDocument.setNewDocument(new File(file_name));
        sendDocument.setChatId(message.getChatId());
        try {
            sendDocument(sendDocument);
            l.info("@@@\tsent file "+file_name);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void readAttendance(Update update) throws IOException, SQLException {
        if(update.hasMessage()){
            Message message = update.getMessage();
            String chatId = message.getChatId().toString()+"attend";
            if(message.hasText()){
                String text = message.getText();
                if(text.equals("/postattendance")){
                    l.info("@posting attendance initialized, chatId = "+chatId);
                    map.put(chatId, 1);
                    sendMsg(message, "Отправьте нам Excel файл в нужном формате. Для того, чтобы узнать, в каком формате нужно отправлять Excel файл, введите /postattendancehelp");
                } else if(text.equals("/postattendancehelp")){
                    l.info("@sending Excel attendance example to client, chatId = "+chatId);
                    download(message, getDirPath()+"examples\\attendance.xlsx");
                }
            } else if(message.hasDocument()){
                if(map.containsKey(chatId)){
                    if(map.get(chatId) == 1){
                        l.info("@getting file from client, chatId = "+chatId);
                        uploadFile("controls\\"+message.getDocument().getFileName(), message.getDocument().getFileId());
                        File file = new File(getDirPath()+"controls\\"+message.getDocument().getFileName());
                        try{
                            attendanceDao.create(parceAttendanceExcel(file));
                            sendMsg(message, "Файл был успешно загружен и прочитан");
                        } catch (IndexOutOfBoundsException | NullPointerException e){
                            e.printStackTrace();
                            sendMsg(message,"Ошибка при считывании файла, повторите попытку");
                        } finally {
                            map.remove(chatId);
                        }
                    }
                }
            }
        }
    }

    public void sendAttendance(Update update) throws SQLException {
        if(update.hasMessage()){
            Message message = update.getMessage();
            String chatId = message.getChatId().toString()+"sndatt";
            if(message.hasText()){
                String text = message.getText();
                if(text.equals("/attendance")){
                    l.info("@getting attendance initialized, chatId = "+chatId);
                    map.put(chatId, 1);
                    sendMsg(message, "Отправьте нам дату, на которую хотите получить посещаемость, в формате ГГГГ ММ ДД");
                } else if(map.containsKey(chatId)){
                    if(map.get(chatId) == 1){
                        l.info("@getting date from client, chatId = "+chatId);
                        isValidDate(text,update.getMessage().getChatId());
                        l.info("@setting date");
                        dates.put(chatId, text);
                        map.put(chatId, 2);
                        sendMsg(message, "Отлично, теперь укажите класс, по которому хотите получить посещаемость");
                        l.info("@error whlie receiving date");
                        sendMsg(message, "Неправильный формат даты, повторите попытку");
                    } else if(map.get(chatId) == 2){
                        l.info("@getting group from client, chatId = "+chatId);
                        Group group = GroupService.getGroupByName(text);
                        if(group != null){
                            l.info("@getting attendances");
                            sendMsg(message, "Отлично, отправляем вам посещаемость учеников");
                            for(Attendance a:attendanceDao.queryForAll()){
                                if(a.getDate().equals(dates.get(chatId)) && UserService.isUserGroup(group, a.getUser())){
                                    if(a.isAttends()){
                                        sendMsg(message, a.getUser().getLname()+": присутствовал");
                                    } else {
                                        sendMsg(message, a.getUser().getLname()+": отсутствовал");
                                    }
                                }
                            }
                            map.remove(chatId);
                            dates.remove(chatId);
                            l.info("@ending attendance cycle");
                        } else {
                            sendMsg(message, "Группы не существует, повторите попытку");
                        }
                    }
                }
            }
        }
    }
    public void setGroupButtons(SendMessage message,long chat_id) throws SQLException {
        message.setChatId(chat_id)
                .setText("Выберете группу:");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        for(Subject subject: subjectDao.queryForAll()){
            row = new KeyboardRow();
            row.add(subject.getName());
            keyboard.add(row);
        }

        row = new KeyboardRow();
        row.add("❌ Закрыть ❌");
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);
    }

    public ArrayList<Attendance> parceAttendanceExcel(File excel) throws IOException, SQLException {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> date = new ArrayList<>();
        ArrayList<String> status = new ArrayList<>();
        ArrayList<Attendance> attendances = new ArrayList<>();
        FileInputStream fileInputStream = new FileInputStream(excel);
        XSSFWorkbook workbook = new XSSFWorkbook(fileInputStream);
        XSSFSheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.iterator();
        int position, rowCounter = 1;
        while(rowIterator.hasNext()){
            Row row = rowIterator.next();
            Iterator<Cell> cellIterator = row.iterator();
            position = 1;
            while(cellIterator.hasNext()){
                Cell cell = cellIterator.next();
                if(rowCounter == 1){
                    if(cell.getCellType() == CellType.STRING){
                        l.info("dates "+cell.getStringCellValue());
                        if(!cell.getStringCellValue().equals("name")){
                            date.add(cell.getStringCellValue());
                        }
                    }
                } else {
                    if(position == 1){
                        l.info("names "+cell.getStringCellValue());
                        names.add(cell.getStringCellValue());
                    } else {
                        l.info("isAttend "+cell.getStringCellValue());
                        status.add(cell.getStringCellValue());
                    }
                }
                position ++;
            }
            rowCounter++;
        }
        UserDb u;
        boolean isAttend;
        for(int i=0;i<names.size();i++){
            for(int j=0;j<date.size();j++){
                u = UserService.getByLName(names.get(i));
                isAttend = status.get(j).equals("-");
                attendances.add(new Attendance(u, date.get(j), isAttend));
            }
        }
        l.info("@@@parcing done");
        return attendances;
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


}
