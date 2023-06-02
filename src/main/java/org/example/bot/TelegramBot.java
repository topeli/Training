package org.example.bot;

import lombok.extern.slf4j.Slf4j;
import org.example.controllers.CoachController;
import org.example.controllers.MarkController;
import org.example.controllers.StudentController;
import org.example.models.Coach;
import org.example.models.Mark;
import org.example.models.Student;
import org.example.models.Training;
import org.example.repositories.CoachRepository;
import org.example.repositories.MarkRepository;
import org.example.repositories.StudentRepository;
import org.example.repositories.TrainingRepository;
import org.example.services.MarkService;
import org.example.services.TrainingService;
import org.springframework.scheduling.annotation.Scheduled;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {
    private final static ConcurrentHashMap<Long, Long> chosenStudent = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<Long, Student> userStudent = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<Long, Coach> userCoach = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<Long, Training> coachTraining = new ConcurrentHashMap<>();


    private final static ConcurrentHashMap<Long, UserCondition> userCondition = new ConcurrentHashMap<>();

    private final TelegramConfig telegramConfig;
    private final StudentController studentController;
    private final StudentRepository studentRepository;
    private final CoachController coachController;
    private final MarkController markController;
    private final MarkService markService;
    private final CoachRepository coachRepository;
    private final MarkRepository markRepository;
    private final TrainingService trainingService;
    private final TrainingRepository trainingRepository;

    public TelegramBot(TelegramConfig telegramConfig, StudentController studentController, StudentRepository studentRepository, CoachController coachController, MarkController markController, MarkService markService, CoachRepository coachRepository, MarkRepository markRepository, TrainingService trainingService, TrainingRepository trainingRepository) {
        this.telegramConfig = telegramConfig;
        this.studentController = studentController;
        this.studentRepository = studentRepository;
        this.coachController = coachController;
        this.markController = markController;
        this.markService = markService;
        this.coachRepository = coachRepository;
        this.markRepository = markRepository;
        this.trainingService = trainingService;
        this.trainingRepository = trainingRepository;
        log.info("Начинаем добавлять меню");
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Start work"));
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
                    if (studentRepository.studentByChatId(chatId).isEmpty() && coachRepository.coachByChatId(chatId).isEmpty()) {
                        startCommandRecieved(chatId, update.getMessage().getChat().getFirstName());
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

                chosenStudent.put(chatId, studentId);

                Student student = studentRepository.findById(studentId).orElseThrow();
                String text = "Выбор оценки (" + student.getName() + " " + student.getSurname() + "):";

                executeEditMessageText(text, chatId, messageId);

                chooseMark(chatId);

            } else if (callbackData.startsWith("MARK_")) {
                try {
                    Integer mark = Integer.valueOf(extractCallBackData(callbackData));
                    Long studentId = chosenStudent.get(chatId);

                    Coach coach = coachRepository.coachByChatId(chatId).get(0);

                    addMark(mark, studentId, coach);
                    String text = "Оценка сохранена";
                    executeEditMessageText(text, chatId, messageId);
                    displayCoachMenu(chatId);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else if (callbackData.startsWith("STUDENTGROUP_")) {
                if(!extractCallBackData(callbackData).equals("назад")){
                String studentId = extractCallBackData(callbackData);
                Student student = studentRepository.findById(Long.valueOf(studentId)).orElseThrow();
                String text = "Выбран студент: " + student.getName() + " " + student.getSurname();
                executeEditMessageText(text, chatId, messageId);

                userStudent.put(chatId, student);
                userCondition.put(chatId, UserCondition.WAITING_FOR_PASSWORD);

                sendMessage(chatId, "Введите пароль:");}
                else {
                    String nazad = extractCallBackData(callbackData);
                    nazad = "Выбрана команда: " + nazad;

                    executeEditMessageText(nazad, chatId, messageId);
                    //displayMainMenu(chatId);
                    getGroups(chatId, "REGGROUP_");
                }
            } else if (callbackData.startsWith("REGCOACH_")) {
                String text = "Выбрана роль: тренер";
                executeEditMessageText(text, chatId, messageId);
                getCoaches(chatId, "COACH_");

            } else if (callbackData.startsWith("COACH_")) {
                String coachId = extractCallBackData(callbackData);
                Coach coach = coachRepository.findById(Long.valueOf(coachId)).orElseThrow();
                String text = "Выбран тренер: " + coach.getName() + " " + coach.getSurname();
                executeEditMessageText(text, chatId, messageId);

                userCoach.put(chatId, coach);
                userCondition.put(chatId, UserCondition.WAITING_FOR_PASSWORD_COACH);

                sendMessage(chatId, "Введите пароль:");
            } else if (callbackData.startsWith("MYMARKS_")) {
                Long studentId = Long.valueOf(extractCallBackData(callbackData));
                List<Mark> marks = markRepository.getMarksByStudentId(studentId);
                String message = "Мои оценки: ";
                for (Mark mark : marks) {
                    message += String.valueOf(mark.getMark());
                    message += " ";
                }
                sendMessage(chatId, message);
                displayStudentMenu(chatId);

            } else if (callbackData.startsWith("REGSTUDENT_")) {

                String text = "Выбрана роль: студент";
                executeEditMessageText(text, chatId, messageId);
                getGroups(chatId, "REGGROUP_");
            } else if (callbackData.startsWith("REGGROUP_")) {
                if(!extractCallBackData(callbackData).equals("назад")){
                    log.info(extractCallBackData(callbackData));
                    String group = extractCallBackData(callbackData);
                    String text = "Выбрана группа: " + group;

                    executeEditMessageText(text, chatId, messageId);
                    getStudentsInGroup(chatId, group);
                }
                else {
                    String nazad = extractCallBackData(callbackData);
                    nazad = "Выбрана команда: " + nazad;

                    executeEditMessageText(nazad, chatId, messageId);
                    displayMainMenu(chatId);
                }


            } else if (callbackData.startsWith("COACHGROUPS_")) {
                Long coachId = Long.valueOf(extractCallBackData(callbackData));
                Coach coach = coachRepository.findById(coachId).orElseThrow();
                displayCoachGroups(chatId, coach, "GROUP_");


            } else if (callbackData.startsWith("ADDTRAINING_")) {
                Long coachId = Long.valueOf(extractCallBackData(callbackData));

                coachTraining.put(chatId, new Training());

                Coach coach = coachRepository.findById(coachId).orElseThrow();
                chooseActivity(chatId, coach, "CHOOSEACTIVITY_");

            } else if (callbackData.startsWith("CHOOSEACTIVITY_")) {
                String activity = extractCallBackData(callbackData);
                coachTraining.get(chatId).setActivity(activity);
                String text = "Выбрана тренировка: " + activity;
                executeEditMessageText(text, chatId, messageId);

                Coach coach = coachRepository.coachByChatId(chatId).get(0);

                displayCoachGroups(chatId, coach, "TRAININGGROUP_");
            } else if (callbackData.startsWith("TRAININGGROUP_")) {
                String group = extractCallBackData(callbackData);
                coachTraining.get(chatId).setClassGroup(group);

                displayDates(chatId, group, "TRAININGDATE_");

                String text = "Выбрана группа: " + group;
                executeEditMessageText(text, chatId, messageId);
            } else if (callbackData.startsWith("GROUP_")) {
                String group = extractCallBackData(callbackData);
                getStudentsInGroupCoach(chatId, group);
                executeEditMessageText("Выбрана группа: " + group, chatId, messageId);

            } else if (callbackData.startsWith("STUDENTGROUPCOACH_")) {
                String studentId = extractCallBackData(callbackData);
                displayCommandsForCoach(chatId, studentId);
                Student student = studentRepository.findById(Long.valueOf(studentId)).orElseThrow();
                String text = "Выбран студент: " + student.getName() + " " + student.getSurname();
                executeEditMessageText(text, chatId, messageId);
            } else if (callbackData.startsWith("TRAININGDATE_")) {
                displayTime(chatId, "STARTTIME_");
                String date = extractCallBackData(callbackData);

                LocalDate trainingDate = parseDate(date);
                coachTraining.get(chatId).setDate(trainingDate);

                String text = "Выбрана дата: " + date;
                executeEditMessageText(text, chatId, messageId);
            } else if (callbackData.startsWith("STARTTIME_")) {
                String timeOfTraining = extractCallBackData(callbackData);

                LocalTime startTime = parseTime(timeOfTraining);
                coachTraining.get(chatId).setStartTime(startTime);

                displayTimeEnd(chatId, timeOfTraining, "ENDTIME_");
                String text = "Время начала тренировки: " + timeOfTraining;
                executeEditMessageText(text, chatId, messageId);

            } else if (callbackData.startsWith("ENDTIME_")) {
                String timeOfTraining = extractCallBackData(callbackData);

                String text = "Время конца тренировки: " + timeOfTraining;
                executeEditMessageText(text, chatId, messageId);

                LocalTime endTime = parseTime(timeOfTraining);
                coachTraining.get(chatId).setEndTime(endTime);

                Coach coach = coachRepository.coachByChatId(chatId).get(0);
                coachTraining.get(chatId).setCoach(coach);

                addTraining(coachTraining.get(chatId));
                sendMessage(chatId, "Тренировка сохранена");

                displayCoachMenu(chatId);
            } else if (callbackData.startsWith("BACKTOMAINMENUCOACH_")) {
                displayCoachMenu(chatId);
                executeEditMessageText("Вы вернулись в главное меню", chatId, messageId);
            } else if (callbackData.startsWith("SCHEDULECOACH_")) {
                //trainingRepository.trainingByCoachId(chatId);
                Long coachId = Long.valueOf(extractCallBackData(callbackData));
                List<Training> trainings = trainingRepository.trainingByCoachId(coachId);
                String message = "Мои тренировки:  \n";
                for (Training training : trainings) {
                    message += "\uD83C\uDD98" + "Группа: " + String.valueOf(training.getClassGroup()) + "\n" + "дата: " + String.valueOf(training.getDate()) + " время: " + String.valueOf(training.getStartTime()) + "-" + String.valueOf(training.getEndTime()) + "\n";
                }
                sendMessage(chatId, message);
                displayCoachMenu(chatId);
            } else if (callbackData.startsWith("SCHEDULESTUDENT_")) {
                String group = String.valueOf(extractCallBackData(callbackData));
                List<Training> trainings = trainingRepository.trainingByStudent(group);
                String message = "Мои тренировки:  \n";
                for (Training training : trainings) {
                    message += "\uD83C\uDD98" + "Дата: " + String.valueOf(training.getDate()) + " время: " + String.valueOf(training.getStartTime()) + "-" + String.valueOf(training.getEndTime()) + "\n";
                }
                sendMessage(chatId, message);
                displayStudentMenu(chatId);
            } else if (callbackData.startsWith("EXITCOACH_")) {
                Long coachId = Long.valueOf(extractCallBackData(callbackData));
                Coach coach = coachRepository.findById(coachId).orElseThrow();
                coach.setChatId(null);
                coachRepository.save(coach);
                sendMessage(chatId, "Вы вышли из аккаунта");
                displayMainMenu(chatId);
            } else if (callbackData.startsWith("EXITSTUDENT_")) {
                Long studentId = Long.valueOf(extractCallBackData(callbackData));
                Student student = studentRepository.findById(studentId).orElseThrow();
                student.setChatId(null);
                studentRepository.save(student);
                sendMessage(chatId, "Вы вышли из аккаунта");
                displayMainMenu(chatId);
            }
        }
    }

    private void chooseActivity(Long chatId, Coach coach, String callbackData) {
        String[] activities = coach.getActivity().split(" ");
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выбор активности:");
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        for (int i = 0; i < activities.length; i++) {
            List<InlineKeyboardButton> buttonsInLine = new ArrayList<>();
            InlineKeyboardButton activity = new InlineKeyboardButton();
            activity.setText(activities[i]);
            activity.setCallbackData(callbackData + activities[i]);
            buttonsInLine.add(activity);
            rowsInLine.add(buttonsInLine);
        }
        markup.setKeyboard(rowsInLine);
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("ошибка");
        }

    }

    private LocalDate parseDate(String date) {
        String[] dayMonth = date.split("\\.");
        return LocalDate.of(2023, Integer.parseInt(dayMonth[1]), Integer.parseInt(dayMonth[0]));
    }

    private LocalTime parseTime(String time) {
        String[] hourMinute = time.split(":");
        return LocalTime.of(Integer.parseInt(hourMinute[0]), Integer.parseInt(hourMinute[1]));
    }

    private void displayTimeEnd(Long chatId, String timeOfEnd, String callbackData) {
        LocalTime timeOfTraining = LocalTime.parse(timeOfEnd);
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выбор конца тренировки:");
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> buttonsInLine = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            timeOfTraining = timeOfTraining.plusHours(1L);
            InlineKeyboardButton time = new InlineKeyboardButton();
            time.setText(timeOfTraining.toString());
            time.setCallbackData(callbackData + timeOfTraining);
            buttonsInLine.add(time);
        }
        rowsInLine.add(buttonsInLine);
        markup.setKeyboard(rowsInLine);
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при выводе возможных дат тренировки");
        }
    }

    private void displayTime(Long chatId, String callbackData) {
        //еще раз достать тренера из бд и посмотреть его тренировки именно в этот день
        LocalTime timeOfTraining = LocalTime.parse("10:00");
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выбор начала тренировки:");
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> buttonsInLine = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Long coachId = Long.valueOf(extractCallBackData(callbackData));
            Coach coach = coachRepository.findById(Long.valueOf(coachId)).orElseThrow();
            Training training = trainingRepository.findById(coachId).orElseThrow();
            if(training.getStartTime()!=timeOfTraining)
            {
                InlineKeyboardButton time = new InlineKeyboardButton();
                time.setText(timeOfTraining.toString());
                time.setCallbackData(callbackData + timeOfTraining);
                timeOfTraining = timeOfTraining.plusHours(2L);
                buttonsInLine.add(time);
            }
        }
        rowsInLine.add(buttonsInLine);
        markup.setKeyboard(rowsInLine);
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при выводе возможных дат тренировки");
        }
    }

    private void displayCommandsForCoach(Long chatId, String studentId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите действие:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton putMark = new InlineKeyboardButton();
        putMark.setText("Поставить оценку\uD83E\uDE84\n");
        putMark.setCallbackData("STUDENT_" + studentId);
        rows.add(List.of(putMark));

        InlineKeyboardButton backToMainMenuCoach = new InlineKeyboardButton();
        backToMainMenuCoach.setText("Вернуться в главное меню\uD83E\uDEE1\n");
        backToMainMenuCoach.setCallbackData("BACKTOMAINMENUCOACH_" + studentId);
        rows.add(List.of(backToMainMenuCoach));

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при выводе меню студента:" + e.getMessage());
        }
    }

    private void displayDates(Long chatId, String group, String callbackData) {
        LocalDateTime currentDate = LocalDateTime.now();

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выбор даты:");
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> buttonsInLine = new ArrayList<>();
        //достать тренера из бд и его тренировки
        Long coachId = Long.valueOf(extractCallBackData(callbackData));
        Coach coach = coachRepository.findById(Long.valueOf(coachId)).orElseThrow();
        Training training = trainingRepository.findById(coachId).orElseThrow();
        for (int i = 0; i < 8; i++) {
            InlineKeyboardButton date = new InlineKeyboardButton();
            date.setText(currentDate.getDayOfMonth() + "." + String.valueOf(currentDate.getMonth().ordinal() + 1));
            date.setCallbackData(callbackData + currentDate.getDayOfMonth() + "." + String.valueOf(currentDate.getMonth().ordinal() + 1));
            buttonsInLine.add(date);
            currentDate = currentDate.plusDays(1L);
            //проверить что есть хотя бы одно свободное окно у тренера в этот день
            //если нет - скип, дату не добавляем

        }

        rowsInLine.add(buttonsInLine);
        markup.setKeyboard(rowsInLine);
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при выводе возможных дат тренировки");
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
            log.error("ошибка в выборе группы");
        }
    }

    private void getStudentsInGroupCoach(Long chatId, String group) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Список студентов в группе " + group);

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
        InlineKeyboardButton nazad = new InlineKeyboardButton();
        nazad.setText("↩️");
        nazad.setCallbackData(callbackData + "назад");
        buttonsInLine.add(nazad);
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
        regStudent.setText("Зарегистрироваться как студент \uD83E\uDD20\n");
        regStudent.setCallbackData("REGSTUDENT_");
        rows.add(List.of(regStudent));

        InlineKeyboardButton regCoach = new InlineKeyboardButton();
        regCoach.setText("Зарегистрироваться как тренер \uD83D\uDDFF\n");
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
        schedule.setCallbackData("SCHEDULESTUDENT_" + student.getClassGroup());
        rows.add(List.of(schedule));

        InlineKeyboardButton marks = new InlineKeyboardButton();
        marks.setText("Мои оценки \uD83D\uDD1D\n");
        marks.setCallbackData("MYMARKS_" + student.getId());
        InlineKeyboardButton exit = new InlineKeyboardButton();
        exit.setText("Выйти из аккаунта \uD83E\uDD7A\n");
        exit.setCallbackData("EXITSTUDENT_" + student.getId());

        rows.add(List.of(marks));
        rows.add(List.of(exit));
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
        schedule.setCallbackData("SCHEDULECOACH_" + coach.getId());
        rows.add(List.of(schedule));

        InlineKeyboardButton groups = new InlineKeyboardButton();
        groups.setText("Мои группы \uD83D\uDC65\n");
        groups.setCallbackData("COACHGROUPS_" + coach.getId());
        rows.add(List.of(groups));

        InlineKeyboardButton addTraining = new InlineKeyboardButton();
        addTraining.setText("Добавить тренировку \uD83D\uDE35\n");
        addTraining.setCallbackData("ADDTRAINING_" + coach.getId());
        rows.add(List.of(addTraining));

        InlineKeyboardButton exitCoach = new InlineKeyboardButton();
        exitCoach.setText("Выйти из аккаунта \uD83D\uDEAA\n");
        exitCoach.setCallbackData("EXITCOACH_" + coach.getId());
        rows.add(List.of(exitCoach));

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
        List<InlineKeyboardButton> buttonsInLine = new ArrayList<>();
        List<Student> students = studentController.getStudentsByGroup(group);
        for (int i = 0; i < students.size(); i++) {
            InlineKeyboardButton student = new InlineKeyboardButton();
            student.setText(students.get(i).getName() + " " + students.get(i).getSurname());
            student.setCallbackData("STUDENTGROUP_" + students.get(i).getId());
            rows.add(List.of(student));
        }
        rows.add(buttonsInLine);
        InlineKeyboardButton nazad = new InlineKeyboardButton();
        nazad.setText("↩️");
        nazad.setCallbackData("STUDENTGROUP_" + "назад");
        buttonsInLine.add(nazad);
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
        String answer = "Привет, " + firstName + "! Это бот для ДЮСШ-2. У него есть разные возможности как для тренера, так и для спортсменов. Можете ознакомиться с ними:\n\n Для тренера:\n" +
                "\uD83D\uDC94добавлять тренировки  \n" +
                "\uD83D\uDC94смотреть свои тренировки  \n" +
                "\uD83D\uDC94отмечать список детей, пришедших на тренировку  \n" +
                "\uD83D\uDC94получать уведомления (за день или за час до тренировки) \n" +
                "\uD83D\uDC94ставить оценки за тренировку \n\n" +
                "Для ученика:  \n" +
                "\uD83D\uDC94получать уведомления (за день или за час до тренировки) \n" +
                "\uD83D\uDC94просматривать расписание \n" +
                "\uD83D\uDC94просматривать свои оценки";

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

    private void addMark(int mark, Long studentId, Coach coach) throws Exception {
        Mark mark1 = markService.addMark(mark, coach, studentId);

        if (mark1.getStudent().getChatId() != null) {
            String text =
                    "Тренер " + mark1.getCoach().getName() + " " + mark1.getCoach().getSurname() + " поставил вам оценку " + mark;
            sendMessage(mark1.getStudent().getChatId(), text);
        }
    }

    private void addTraining(Training training) {
        trainingService.addTraining(training);

        List<Student> students = studentRepository.findByGroup(training.getClassGroup());

        String text = "Вам добавили тренировку:\n" +
                "Дата: " + training.getDate() + "\n" +
                "Время: " + training.getStartTime() + " " + training.getEndTime() + "\n" +
                "Тренер: " + training.getCoach().getName() + " " + training.getCoach().getSurname();

        for (int i = 0; i < students.size(); i++) {
            if (students.get(i).getChatId() != null) {
                sendMessage(students.get(i).getChatId(), text);
            }
        }
    }

    @Scheduled
    private void sendNotification() {
        trainingRepository.allTrainings();
        List<Student> students = studentRepository.findAll();

        String text = "У вас скоро тренировка";

        for (int i = 0; i < students.size(); i++) {
            if (students.get(i).getChatId() != null) {
                sendMessage(students.get(i).getChatId(), text);
            }
        }
    }

}
