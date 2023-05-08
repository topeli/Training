package org.example.bot;

import lombok.extern.slf4j.Slf4j;
import org.example.controllers.CoachController;
import org.example.controllers.MarkController;
import org.example.controllers.StudentController;
import org.example.models.Coach;
import org.example.models.Mark;
import org.example.models.Student;
import org.example.repositories.CoachRepository;
import org.example.repositories.MarkRepository;
import org.example.repositories.StudentRepository;
import org.example.services.MarkService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {
    private final static ConcurrentHashMap<Long, Long> chosenStudent = new ConcurrentHashMap<>();

    private final static ConcurrentHashMap<Long, String> chosenGroup = new ConcurrentHashMap<>();

    private final static ConcurrentHashMap<Long, Student> userStudent = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<Long, Coach> userCoach = new ConcurrentHashMap<>();


    private final static ConcurrentHashMap<Long, UserCondition> userCondition = new ConcurrentHashMap<>();

    private final TelegramConfig telegramConfig;
    private final StudentController studentController;
    private final StudentRepository studentRepository;
    private final CoachController coachController;
    private final MarkController markController;
    private final MarkService markService;
    private final CoachRepository coachRepository;
    private final MarkRepository markRepository;

    public TelegramBot(TelegramConfig telegramConfig, StudentController studentController, StudentRepository studentRepository, CoachController coachController, MarkController markController, MarkService markService, CoachRepository coachRepository, MarkRepository markRepository) {
        this.telegramConfig = telegramConfig;
        this.studentController = studentController;
        this.studentRepository = studentRepository;
        this.coachController = coachController;
        this.markController = markController;
        this.markService = markService;
        this.coachRepository = coachRepository;
        this.markRepository = markRepository;
        log.info("Начинаем добавлять меню");
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Start work"));
        /*listOfCommands.add(new BotCommand("/get_students", "Show students list"));
        listOfCommands.add(new BotCommand("/get_coaches", "Show coaches list"));
        listOfCommands.add(new BotCommand("/get_marks", "Show marks list"));
        listOfCommands.add(new BotCommand("/put_mark", "Put mark"));
        listOfCommands.add(new BotCommand("/get_classes", "Show groups list"));
        listOfCommands.add(new BotCommand("/register_as_student", "Register as a student"));
        listOfCommands.add(new BotCommand("/register_as_coach", "Register as a coach"));*/
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
            log.info("Успешно добавлены команды");
        } catch (TelegramApiException e) {
            log.error("Ошибка при добавлении меню " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return telegramConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return telegramConfig.getBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            switch (messageText) {
                case "/start":
                    startCommandRecieved(chatId, update.getMessage().getChat().getFirstName());
                    if (studentRepository.studentByChatId(chatId).isEmpty() && coachRepository.coachByChatId(chatId).isEmpty()) {
                        displayMainMenu(chatId);
                    } else {
                        sendMessage(chatId, "Вы уже зарегистрированы");
                        log.info("Хотим отобразить главное меню...");
                        studentOrCoachMenu(chatId);

                    }

                    break;
                case "/get_coaches":
                    getAllCoaches(chatId);
                    break;
                case "/get_students":
                    getAllStudents(chatId);
                    break;
                /*case "/get_marks":
                    getAllMarks(chatId);
                    break;*/
                case "/put_mark":
                    putMark(chatId);
                    break;
                case "/get_classes":
                    getGroups(chatId, "GROUP_");
                    break;

                default:
                    if (userCondition.containsKey(chatId)) {
                        switch (userCondition.get(chatId)) {
                            case WAITING_FOR_PASSWORD -> checkPassword(messageText, chatId);
                            case WAITING_FOR_PASSWORD_COACH -> checkPasswordCoach(messageText, chatId);
                            default -> sendMessage(chatId, "То что вы прислали не соответствует ни одной команде");

                        }
                    }
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            int messageId = update.getCallbackQuery().getMessage().getMessageId();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.startsWith("STUDENT_")) {
                Long studentId = Long.valueOf(extractCallBackData(callbackData));

                chosenStudent.putIfAbsent(chatId, studentId);

                Student student = studentRepository.findById(studentId).orElseThrow();
                String text = "Выбор оценки (" + student.getName() + " " + student.getSurname() + "):";

                executeEditMessageText(text, chatId, messageId);

                chooseMark(chatId);

            } else if (callbackData.startsWith("MARK_")) {
                try {
                    Integer mark = Integer.valueOf(extractCallBackData(callbackData));
                    Long studentId = chosenStudent.get(chatId);
                    addMark(mark, studentId);
                    String text = "Оценка сохранена";
                    executeEditMessageText(text, chatId, messageId);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } /*else if (callbackData.startsWith("GROUP_")) {
                try {

                    String group = extractCallBackData(callbackData);



                    List<Student> students = studentRepository.findByGroup(group);
                    String text = "Студенты группы " + group + ":\n";
                    for (int i = 0; i < students.size(); i++) {
                        text += students.get(i).getName();
                        text += " ";
                        text += students.get(i).getSurname() + "\n";
                    }
                    executeEditMessageText(text, chatId, messageId);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }*/
        else if (callbackData.startsWith("STUDENTGROUP_")) {
                String studentId = extractCallBackData(callbackData);
                Student student = studentRepository.findById(Long.valueOf(studentId)).orElseThrow();
                String text = "Выбран студент: " + student.getName() + " " + student.getSurname();
                executeEditMessageText(text, chatId, messageId);

                userStudent.putIfAbsent(chatId, student);
                userCondition.putIfAbsent(chatId, UserCondition.WAITING_FOR_PASSWORD);

                sendMessage(chatId, "Введите пароль:");
            } else if (callbackData.startsWith("REGCOACH_")) {
                getCoaches(chatId, "COACH_");

            } else if (callbackData.startsWith("COACH_")) {
                String coachId = extractCallBackData(callbackData);
                Coach coach = coachRepository.findById(Long.valueOf(coachId)).orElseThrow();
                String text = "Выбран тренер: " + coach.getName() + " " + coach.getSurname();
                executeEditMessageText(text, chatId, messageId);

                userCoach.putIfAbsent(chatId, coach);
                userCondition.putIfAbsent(chatId, UserCondition.WAITING_FOR_PASSWORD_COACH);

                sendMessage(chatId, "Введите пароль:");
            } else if (callbackData.startsWith("MYMARKS_")) {
                Long studentId = Long.valueOf(extractCallBackData(callbackData));
                List<Mark> marks = markRepository.getMarksByStudentId(studentId);

                for (Mark mark : marks) {
                    sendMessage(chatId, String.valueOf(mark.getMark()));

                }

            } else if (callbackData.startsWith("REGSTUDENT_")) {
                getGroups(chatId, "REGGROUP_");
            } else if (callbackData.startsWith("REGGROUP_")) {
                String group = extractCallBackData(callbackData);
                String text = "Выбрана группа: " + group;

                executeEditMessageText(text, chatId, messageId);
                getStudentsInGroup(chatId, group);

            } else if (callbackData.startsWith("COACHGROUPS_")) {
                Long coachId = Long.valueOf(extractCallBackData(callbackData));
                Coach coach = coachRepository.findById(coachId).orElseThrow();
                displayCoachGroups(chatId, coach, "GROUP_");

            }
            else if (callbackData.startsWith("GROUP_")) {//не работает а почему
                String group = extractCallBackData(callbackData);
                getStudentsInGroupCoach(chatId, group);
            }
            else if(callbackData.startsWith("STUDENTGROUPCOACH_")) {
                displayCommandsForCoach(chatId);
            }
        }
    }

    private void displayCommandsForCoach(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton putMark = new InlineKeyboardButton();
        putMark.setText("Поставить оценку");
        putMark.setCallbackData("MARK_");
        rows.add(List.of(putMark));

        InlineKeyboardButton putAbsence = new InlineKeyboardButton();
        putAbsence.setText("Н");
        putAbsence.setCallbackData("PUTABSENCE_");
        rows.add(List.of(putAbsence));
        markup.setKeyboard(rows);
        InlineKeyboardButton putPresence = new InlineKeyboardButton();
        putPresence.setText("Не Н");
        putAbsence.setCallbackData("PUTPRESENCE_");
        rows.add(List.of(putPresence));
        markup.setKeyboard(rows);

        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при выводе меню студента:" + e.getMessage());
        }
    }

    private void displayCoachGroups(Long chatId, Coach coach, String callbackData) {
        String[] groups = coach.getClassGroups().split(" ");
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выбор группы:");
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> buttonsInLine = new ArrayList<>();
        for (int i = 0; i < groups.length; i++) {
            InlineKeyboardButton group = new InlineKeyboardButton();
            group.setText(groups[i]);
            group.setCallbackData(callbackData + groups[i]);
            buttonsInLine.add(group);
        }
        rowsInLine.add(buttonsInLine);
        markup.setKeyboard(rowsInLine);
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("ошибка");
        }


    }
    private void getStudentsInGroupCoach(Long chatId, String group) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<Student> students = studentController.getStudentsByGroup(group);
        for (int i = 0; i < students.size(); i++) {
            InlineKeyboardButton student = new InlineKeyboardButton();
            student.setText(students.get(i).getName() + " " + students.get(i).getSurname());
            student.setCallbackData("STUDENTGROUPCOACH_" + students.get(i).getId());
            rows.add(List.of(student));
        }
        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при выводе списка студентов:" + e.getMessage());
        }
    }

    private void studentOrCoachMenu(Long chatId) {
        if (studentRepository.studentByChatId(chatId).isEmpty()) {
            displayCoachMenu(chatId);
        } else {
            displayStudentMenu(chatId);
        }
    }

    private void getCoaches(Long chatId, String callbackData) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите свой профиль:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<Coach> coaches = coachController.getAllCoaches();
        for (int i = 0; i < coaches.size(); i++) {
            InlineKeyboardButton coach = new InlineKeyboardButton();
            coach.setText(coaches.get(i).getName() + " " + coaches.get(i).getSurname());
            coach.setCallbackData(callbackData + coaches.get(i).getId());
            rows.add(List.of(coach));
        }
        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при выводе списка тренеров:" + e.getMessage());
        }
    }

    private void checkPasswordCoach(String password, Long chatId) {
        Coach coach = userCoach.get(chatId);

        if (password.equals(coach.getPasswordCoach())) {
            sendMessage(chatId, "Пароль верный!");
            userCondition.put(chatId, UserCondition.REGISTERED);
            coach.setChatId(chatId);
            coachRepository.save(coach);
            sendMessage(chatId, "Регистрация успешно завершена!");
            displayCoachMenu(chatId);
        } else {
            sendMessage(chatId, "Пароль неверный! Попробуйте снова...");
        }
    }


    private void checkPassword(String password, Long chatId) {
        Student student = userStudent.get(chatId);

        if (password.equals(student.getPassword())) {
            sendMessage(chatId, "Пароль верный!");
            userCondition.put(chatId, UserCondition.REGISTERED);
            student.setChatId(chatId);
            studentRepository.save(student);
            sendMessage(chatId, "Регистрация успешно завершена!");
            displayStudentMenu(chatId);
        } else {
            sendMessage(chatId, "Пароль неверный! Попробуйте снова...");
        }
    }

    private void getGroups(Long chatId, String callbackData) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выбор группы:");
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> buttonsInLine = new ArrayList<>();
        List<String> groups = studentRepository.findDifferentGroups();
        for (int i = 0; i < groups.size(); i++) {
            InlineKeyboardButton group = new InlineKeyboardButton();
            group.setText(groups.get(i));
            group.setCallbackData(callbackData + groups.get(i));
            buttonsInLine.add(group);
        }
        rowsInLine.add(buttonsInLine);
        markup.setKeyboard(rowsInLine);
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("ошибка");
        }
    }

    private String extractCallBackData(String callbackData) {
        return callbackData.split("_")[1];
    }

    private void displayMainMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Главное меню");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton regStudent = new InlineKeyboardButton();
        regStudent.setText("\uD83C\uDFC3\n Зарегистрироваться как студент \uD83D\uDE35\n");
        regStudent.setCallbackData("REGSTUDENT_");
        rows.add(List.of(regStudent));

        InlineKeyboardButton regCoach = new InlineKeyboardButton();
        regCoach.setText("\uD83E\uDD20\n Зарегистрироваться как тренер \uD83D\uDDFF\n");
        regCoach.setCallbackData("REGCOACH_");
        rows.add(List.of(regCoach));
        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при выводе меню студента:" + e.getMessage());
        }
    }

    private void displayStudentMenu(Long chatId) {
        Student student = studentRepository.studentByChatId(chatId).get(0);
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        log.info("Отображаем главное меню студента...");
        message.setText("Главное меню");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton schedule = new InlineKeyboardButton();
        schedule.setText("Мое расписание \uD83D\uDDC2️\n");
        schedule.setCallbackData("SCHEDULE_" + student.getId());
        rows.add(List.of(schedule));

        InlineKeyboardButton marks = new InlineKeyboardButton();
        marks.setText("Мои оценки \uD83D\uDD1D\n");
        marks.setCallbackData("MYMARKS_" + student.getId());

        rows.add(List.of(marks));

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при выводе меню студента:" + e.getMessage());
        }
    }

    private void displayCoachMenu(Long chatId) {
        Coach coach = coachRepository.coachByChatId(chatId).get(0);
        log.info("Отображаем главное меню тренера...");
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Главное меню");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton schedule = new InlineKeyboardButton();
        schedule.setText("Мое расписание \uD83D\uDDC2️\n");
        schedule.setCallbackData("SCHEDULE_" + coach.getId());
        rows.add(List.of(schedule));

        InlineKeyboardButton groups = new InlineKeyboardButton();
        groups.setText("Мои группы \uD83D\uDC65\n");
        groups.setCallbackData("COACHGROUPS_" + coach.getId());
        rows.add(List.of(groups));

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при выводе меню студента:" + e.getMessage());
        }
    }

    private void getStudentsInGroup(Long chatId, String group) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите свой профиль:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<Student> students = studentController.getStudentsByGroup(group);
        for (int i = 0; i < students.size(); i++) {
            InlineKeyboardButton student = new InlineKeyboardButton();
            student.setText(students.get(i).getName() + " " + students.get(i).getSurname());
            student.setCallbackData("STUDENTGROUP_" + students.get(i).getId());
            rows.add(List.of(student));
        }
        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при выводе списка студентов:" + e.getMessage());
        }
    }

    private void chooseMark(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Оценки:");
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> buttonsInLine = new ArrayList<>();
        for (int i = 2; i < 6; i++) {
            InlineKeyboardButton mark = new InlineKeyboardButton();
            mark.setText(String.valueOf(i));
            mark.setCallbackData("MARK_" + i);
            buttonsInLine.add(mark);
        }
        rowsInLine.add(buttonsInLine);
        markup.setKeyboard(rowsInLine);
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("ошибка");
        }
    }

    private void putMark(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Кому вы хотите добавить оценку:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<Student> students = studentController.getAllStudents();
        for (int i = 0; i < students.size(); i++) {
            InlineKeyboardButton student = new InlineKeyboardButton();
            student.setText(students.get(i).getName() + " " + students.get(i).getSurname());
            student.setCallbackData("STUDENT_" + students.get(i).getId());
            rows.add(List.of(student));
        }
        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("ошибка");
        }
    }

    private void executeEditMessageText(String text, Long chatId, int messageId) {
        EditMessageText messageText = new EditMessageText();
        messageText.setText(text);
        messageText.setMessageId(messageId);
        messageText.setChatId(String.valueOf(chatId));

        try {
            execute(messageText);
        } catch (TelegramApiException e) {

        }
    }

    private void startCommandRecieved(Long chatId, String firstName) {
        String answer = "Привет, " + firstName + "!";

        sendMessage(chatId, answer);
    }

    private void sendMessage(Long chatId, String messageToSend) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(messageToSend);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
        }
    }

    private void getAllStudents(Long chatId) {
        List<Student> students = studentController.getAllStudents();
        String text = "";
        for (int i = 0; i < students.size(); i++) {
            text += students.get(i).getName() + " " + students.get(i).getSurname() + " " + students.get(i).getAge() + " " + students.get(i).getClassGroup() + '\n';
        }
        sendMessage(chatId, text);
    }


    private void getAllCoaches(Long chatId) {
        List<Coach> coaches = coachController.getAllCoaches();
        String text = "";
        for (int i = 0; i < coaches.size(); i++) {
            text += coaches.get(i).getName() + " " + coaches.get(i).getSurname() + " " + coaches.get(i).getAge() + " " + coaches.get(i).getExperience() + '\n';
        }
        sendMessage(chatId, text);
    }

    private void addMark(int mark, Long studentId) throws Exception {
        markService.addMark(mark, "Александр", studentId);
    }

    /*private void getAllMarks(Long chatId) {
        List<Mark> marks = markController.getAllMarks();
        String text = "";
        for (int i = 0; i < marks.size(); i++) {
            text += marks.get(i).getMark() + " " + marks.get(i).getStudent() + " " + marks.get(i).getCoach() + '\n';
        }
        sendMessage(chatId, text);
    }*/
}
