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

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {
    private final static ConcurrentHashMap<Long, Long> chosenStudent = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<Long, Student> userStudent = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<Long, Coach> userCoach = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<Long, Training> coachTraining = new ConcurrentHashMap<>();

    private final static ConcurrentHashMap<Long, String> coachActivity = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<Long, Student> adminStudent = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<Long, Coach> adminCoach = new ConcurrentHashMap<>();

    private final static ConcurrentHashMap<Long, UserCondition> userCondition = new ConcurrentHashMap<>();

    private final TelegramConfig telegramConfig;
    private final StudentController studentController;
    private final StudentRepository studentRepository;
    private final CoachController coachController;
    private final MarkService markService;
    private final CoachRepository coachRepository;
    private final MarkRepository markRepository;
    private final TrainingService trainingService;
    private final TrainingRepository trainingRepository;

    public TelegramBot(TelegramConfig telegramConfig, StudentController studentController, StudentRepository studentRepository, CoachController coachController, MarkService markService, CoachRepository coachRepository, MarkRepository markRepository, TrainingService trainingService, TrainingRepository trainingRepository) {
        this.telegramConfig = telegramConfig;
        this.studentController = studentController;
        this.studentRepository = studentRepository;
        this.coachController = coachController;
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
                            case WAITING_FOR_PASSWORD_ADMIN -> checkPasswordAdmin(messageText, chatId);
                            case WAITING_FOR_NAME_STUDENT_ADMIN -> addNameStudentAdmin(messageText, chatId);
                            case WAITING_FOR_SURNAME_STUDENT_ADMIN -> addSurnameStudentAdmin(messageText, chatId);
                            case WAITING_FOR_AGE_STUDENT_ADMIN -> addAgeStudentAdmin(messageText, chatId);
                            case WAITING_FOR_PASSWORD_STUDENT_ADMIN -> addPasswordStudentAdmin(messageText, chatId);
                            case WAITING_FOR_NAME_COACH_ADMIN -> addNameCoachAdmin(messageText, chatId);
                            case WAITING_FOR_SURNAME_COACH_ADMIN -> addSurnameCoachAdmin(messageText, chatId);
                            case WAITING_FOR_AGE_COACH_ADMIN -> addAgeCoachAdmin(messageText, chatId);
                            case WAITING_FOR_EXPERIENCE_COACH_ADMIN -> addExperienceCoachAdmin(messageText, chatId);
                            case WAITING_FOR_PASSWORD_COACH_ADMIN -> addPasswordCoachAdmin(messageText, chatId);
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

                сhooseActivity(chatId, "CHOOSEMARK_");
                executeEditMessageText("Выберите активность и студента", chatId, messageId);

            } else if (callbackData.startsWith("CHOOSEMARK_")) {
                String activity = extractCallBackData(callbackData);
                Student student = studentRepository.findById(chosenStudent.get(chatId)).orElseThrow();
                if (activity.equals("назад")) {
                    displayCommandsForCoach(chatId, String.valueOf(student.getId()));
                } else {
                    coachActivity.put(chatId, activity);

                    String text = "Выбор оценки (" + student.getName() + " " + student.getSurname() + "):";

                    executeEditMessageText(text, chatId, messageId);

                    chooseMark(chatId);
                }


            } else if (callbackData.startsWith("MARK_")) {
                try {
                    Integer mark = Integer.valueOf(extractCallBackData(callbackData));
                    Long studentId = chosenStudent.get(chatId);

                    Coach coach = coachRepository.coachByChatId(chatId).get(0);

                    String activity = coachActivity.get(chatId);

                    addMark(mark, studentId, coach, activity);
                    String text = "Оценка сохранена";
                    executeEditMessageText(text, chatId, messageId);
                    displayCoachMenu(chatId);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else if (callbackData.startsWith("STUDENTGROUP_")) {

                if (!extractCallBackData(callbackData).equals("назад")) {
                    String studentId = extractCallBackData(callbackData);
                    Student student = studentRepository.findById(Long.valueOf(studentId)).orElseThrow();
                    String text = "Выбран студент: " + student.getName() + " " + student.getSurname();
                    executeEditMessageText(text, chatId, messageId);

                    userStudent.put(chatId, student);
                    userCondition.put(chatId, UserCondition.WAITING_FOR_PASSWORD);

                    sendMessage(chatId, "Введите пароль для входа в аккаунт:");
                } else {
                    getGroups(chatId, "GROUP_"); ////!!!!!!!!!
                }
            } else if (callbackData.startsWith("REGCOACH_")) {
                String text = "Выбрана роль: тренер";
                executeEditMessageText(text, chatId, messageId);
                getCoaches(chatId, "COACH_");

            } else if (callbackData.startsWith("COACH_")) {
                if (!extractCallBackData(callbackData).equals("назад")) {
                    String coachId = extractCallBackData(callbackData);
                    Coach coach = coachRepository.findById(Long.valueOf(coachId)).orElseThrow();
                    String text = "Выбран тренер: " + coach.getName() + " " + coach.getSurname();
                    executeEditMessageText(text, chatId, messageId);

                    userCoach.put(chatId, coach);
                    userCondition.put(chatId, UserCondition.WAITING_FOR_PASSWORD_COACH);

                    sendMessage(chatId, "Введите пароль для входа в аккаунт:");
                } else {
                    displayMainMenu(chatId);
                }

            } else if (callbackData.startsWith("MYMARKS_")) {
                Long studentId = Long.valueOf(extractCallBackData(callbackData));
                List<Mark> marks = markRepository.getMarksByStudentId(studentId);

                HashMap<String, List<Integer>> activityMark = new HashMap<>();
                for (Mark mark : marks) {
                    if (activityMark.containsKey(mark.getActivity())) {
                        activityMark.get(mark.getActivity()).add(mark.getMark());
                    } else {
                        ArrayList<Integer> listOfMarks = new ArrayList<>();
                        listOfMarks.add(mark.getMark());

                        activityMark.put(mark.getActivity(), listOfMarks);
                    }
                }

                String message = "Мои оценки:" + '\n' + '\n';
                for (Map.Entry<String, List<Integer>> entry : activityMark.entrySet()) {
                    message += entry.getKey() + ":" + '\n';
                    float sum = 0;
                    for (int i = 0; i < entry.getValue().size(); i++) {
                        message += entry.getValue().get(i) + " ";
                        sum += entry.getValue().get(i);
                    }
                    message += '\n';
                    String formattedDouble = new DecimalFormat("#0.00").format(sum / entry.getValue().size());
                    message += "Средний балл: " + formattedDouble;
                    message += '\n';
                    message += '\n';

                }

                executeEditMessageText(message, chatId, messageId);
                displayStudentMenu(chatId);

            } else if (callbackData.startsWith("REGSTUDENT_")) {
                String text = "Выбрана роль: студент";
                executeEditMessageText(text, chatId, messageId);
                getGroups(chatId, "REGGROUP_");

            } else if (callbackData.startsWith("REGGROUP_")) {

                if (!extractCallBackData(callbackData).equals("назад")) {
                    String group = extractCallBackData(callbackData);
                    String text = "Выбрана группа: " + group;

                    executeEditMessageText(text, chatId, messageId);
                    getStudentsInGroup(chatId, group);
                } else {
                    displayMainMenu(chatId);
                }

            } else if (callbackData.startsWith("COACHGROUPS_")) {
                if (!extractCallBackData(callbackData).equals("назад")) {
                    Long coachId = Long.valueOf(extractCallBackData(callbackData));
                    Coach coach = coachRepository.findById(coachId).orElseThrow();
                    executeEditMessageText("Отображаем группы", chatId, messageId);
                    displayCoachGroups(chatId, coach, "COACHGROUPSNUMS_");///////

                } else {
                    displayCoachMenu(chatId);
                }
            } else if (callbackData.startsWith("ADDTRAINING_")) {
                Long coachId = Long.valueOf(extractCallBackData(callbackData));

                coachTraining.put(chatId, new Training());

                Coach coach = coachRepository.findById(coachId).orElseThrow();
                chooseActivity(chatId, coach, "CHOOSEACTIVITY_");
                executeEditMessageText("Выберите тренировку", chatId, messageId);


            } else if (callbackData.startsWith("CHOOSEACTIVITY_")) {
                if (!extractCallBackData(callbackData).equals("назад")) {
                    String activity = extractCallBackData(callbackData);
                    coachTraining.get(chatId).setActivity(activity);
                    String text = "Выбрана тренировка: " + activity;
                    executeEditMessageText(text, chatId, messageId);

                    Coach coach = coachRepository.coachByChatId(chatId).get(0);

                    displayCoachGroups(chatId, coach, "TRAININGGROUP_");
                } else {
                    displayCoachMenu(chatId);
                }


            } else if (callbackData.startsWith("TRAININGGROUP_")) {
                if (!extractCallBackData(callbackData).equals("назад")) {
                    String group = extractCallBackData(callbackData);
                    coachTraining.get(chatId).setClassGroup(group);

                    displayDates(chatId, group, "TRAININGDATE_");

                    String text = "Выбрана группа: " + group;
                    executeEditMessageText(text, chatId, messageId);
                } else {
                    Coach coach = coachRepository.coachByChatId(chatId).get(0);
                    chooseActivity(chatId, coach, "CHOOSEACTIVITY_");
                }
            } else if (callbackData.startsWith("COACHGROUPSNUMS_")) {
                if (!extractCallBackData(callbackData).equals("назад")) {
                    String group = extractCallBackData(callbackData);
                    log.info("Нажали на кнопку с номером группы " + group);
                    executeEditMessageText("Выбрана группа: " + group, chatId, messageId);
                    getStudentsInGroupCoach(chatId, group, "STUDENTGROUPCOACH_");
                } else {
                    displayCoachMenu(chatId);
                }
            } else if (callbackData.startsWith("GROUP_")) {
                if (!extractCallBackData(callbackData).equals("назад")) {
                    String group = extractCallBackData(callbackData);
                    executeEditMessageText("Выбрана группа: " + group, chatId, messageId);
                    getStudentsInGroupCoach(chatId, group, "STUDENTGROUP_");
                } else {
                    String nazad;
                    nazad = "Вы вернулись в назад";
                    displayMainMenu(chatId);
                    executeEditMessageText(nazad, chatId, messageId);
                }

            } else if (callbackData.startsWith("STUDENTGROUPCOACH_")) {

                if (!extractCallBackData(callbackData).equals("назад")) {
                    String studentId = extractCallBackData(callbackData);
                    Student student = studentRepository.findById(Long.valueOf(studentId)).orElseThrow();
                    String text = "Выбран студент: " + student.getName() + " " + student.getSurname();
                    executeEditMessageText(text, chatId, messageId);
                    displayCommandsForCoach(chatId, studentId);
                } else {
                    Coach coach = coachRepository.coachByChatId(chatId).get(0);
                    displayCoachGroups(chatId, coach, "COACHGROUPSNUMS_");
                }
            } else if (callbackData.startsWith("TRAININGDATE_")) {

                if (!extractCallBackData(callbackData).equals("назад")) {
                    String date = extractCallBackData(callbackData);
                    LocalDate trainingDate = parseDate(date);
                    coachTraining.get(chatId).setDate(trainingDate);

                    displayTime(chatId, "STARTTIME_");

                    String text = "Выбрана дата: " + date;
                    executeEditMessageText(text, chatId, messageId);
                } else {
                    Coach coach = coachRepository.coachByChatId(chatId).get(0);
                    displayCoachGroups(chatId, coach, "TRAININGGROUP_");
                }
            } else if (callbackData.startsWith("STARTTIME_")) {

                if (!extractCallBackData(callbackData).equals("назад")) {
                    String timeOfTraining = extractCallBackData(callbackData);

                    LocalTime startTime = parseTime(timeOfTraining);
                    coachTraining.get(chatId).setStartTime(startTime);

                    String text = "Время начала тренировки: " + timeOfTraining;
                    executeEditMessageText(text, chatId, messageId);
                    displayTimeEnd(chatId, timeOfTraining, "ENDTIME_");
                } else {
                    String group = String.valueOf(extractCallBackData(callbackData));
                    displayDates(chatId, group, "TRAININGDATE_");
                }

            } else if (callbackData.startsWith("ENDTIME_")) {
                if (!extractCallBackData(callbackData).equals("назад")) {
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
                } else {
                    displayTime(chatId, "STARTTIME_");
                }
            } else if (callbackData.startsWith("BACKTOMAINMENUCOACH_")) {
                displayCoachMenu(chatId);
                executeEditMessageText("Вы вернулись в главное меню", chatId, messageId);
            } else if (callbackData.startsWith("SCHEDULECOACH_")) {
                //trainingRepository.trainingByCoachId(chatId);
                Long coachId = Long.valueOf(extractCallBackData(callbackData));
                List<Training> trainings = trainingRepository.trainingByCoachId(coachId);
                String message = "Мои тренировки:  \n";
                for (Training training : trainings) {
                    message += "\uD83C\uDD98" + "Группа: " + String.valueOf(training.getClassGroup()) + "\n" + "тренировка: " + training.getActivity() + "\n" + "дата: " + training.getDate().getDayOfMonth() + "." + String.valueOf(training.getDate().getMonth().ordinal() + 1)
                            + " время: " + String.valueOf(training.getStartTime()) + "-" + String.valueOf(training.getEndTime()) + "\n" + "\n";
                }
                executeEditMessageText(message, chatId, messageId);
                displayCoachMenu(chatId);
            } else if (callbackData.startsWith("SCHEDULESTUDENT_")) {
                String group = String.valueOf(extractCallBackData(callbackData));
                List<Training> trainings = trainingRepository.trainingByClassGroup(group);
                String message = "Мои тренировки:  \n";
                for (Training training : trainings) {
                    message += "\uD83C\uDD98" + "Тренировка: " + training.getActivity() + "\n" + "дата: " + training.getDate().getDayOfMonth() + "." + String.valueOf(training.getDate().getMonth().ordinal() + 1) + " время: " + String.valueOf(training.getStartTime()) + "-" + String.valueOf(training.getEndTime()) + "\n" + "\n";
                }
                sendMessage(chatId, message);
                executeEditMessageText("Отображаем тренировки", chatId, messageId);
                displayStudentMenu(chatId);
            } else if (callbackData.startsWith("EXITCOACH_")) {
                Long coachId = Long.valueOf(extractCallBackData(callbackData));
                Coach coach = coachRepository.findById(coachId).orElseThrow();
                coach.setChatId(null);
                coachRepository.save(coach);
                executeEditMessageText("Вы вышли из аккаунта", chatId, messageId);
                displayMainMenu(chatId);
            } else if (callbackData.startsWith("EXITSTUDENT_")) {
                Long studentId = Long.valueOf(extractCallBackData(callbackData));
                Student student = studentRepository.findById(studentId).orElseThrow();
                student.setChatId(null);
                studentRepository.save(student);
                executeEditMessageText("Вы вышли из аккаунта", chatId, messageId);
                displayMainMenu(chatId);
            } else if (callbackData.startsWith("REGADMIN_")) {
                userCondition.put(chatId, UserCondition.WAITING_FOR_PASSWORD_ADMIN);
                executeEditMessageText("Выбрана роль: админ", chatId, messageId);
                sendMessage(chatId, "Введите пароль:");
            } else if (callbackData.startsWith("ADDSTUDENTADMIN_")) {
                getGroups(chatId, "VVODNAMESTUDENT_");
                executeEditMessageText("Выбрана команда: добавление ученика", chatId, messageId);

            } else if (callbackData.startsWith("VVODNAMESTUDENT_")) {

                if (extractCallBackData(callbackData).equals("назад")) {
                    displayAdminMenu(chatId);
                } else {
                    sendMessage(chatId, "Введите имя нового ученика: ");
                    executeEditMessageText("Выбрана группа: " + extractCallBackData(callbackData), chatId, messageId);
                    Student student = new Student();
                    String callback = extractCallBackData(callbackData);
                    student.setClassGroup(callback);
                    adminStudent.put(chatId, student);
                    userCondition.put(chatId, UserCondition.WAITING_FOR_NAME_STUDENT_ADMIN);
                }
            } else if (callbackData.startsWith("ADDCOACHADMIN_")) {
                executeEditMessageText("Введите имя нового тренера:", chatId, messageId);
                Coach coach = new Coach();
                adminCoach.put(chatId, coach);
                userCondition.put(chatId, UserCondition.WAITING_FOR_NAME_COACH_ADMIN);

            } else if (callbackData.startsWith("CHOOSEGROUPSFORCOACH_")) {
                executeEditMessageText("Выбраны группы: " + extractCallBackData(callbackData), chatId, messageId);
                Coach coach = adminCoach.get(chatId);
                String callback = extractCallBackData(callbackData);

                String currentClassGroups = coach.getClassGroups() != null ? " " + coach.getClassGroups() : "";
                coach.setClassGroups(callback + currentClassGroups);

                displayActivitiesMenu(chatId, "SETPASSWORDCOACH_");
            } else if (callbackData.startsWith("SETPASSWORDCOACH_")) {
                if (!extractCallBackData(callbackData).equals("назад")) {
                    Coach coach = adminCoach.get(chatId);

                    if (!extractCallBackData(callbackData).equals("next")) {
                        String currentActivities = coach.getActivity() != null ? " " + coach.getActivity() : "";
                        coach.setActivity(extractCallBackData(callbackData) + currentActivities);
                    }

                    adminCoach.put(chatId, coach);
                    executeEditMessageText("Тренер преподает: " + extractCallBackData(callbackData), chatId, messageId);
                    sendMessage(chatId, "Введите пароль для тренера" + '\n' +
                            "Если хотите добавить активность введите 1");
                    userCondition.put(chatId, UserCondition.WAITING_FOR_PASSWORD_COACH_ADMIN);

                } else {
                    getGroupsForAdmin(chatId, "CHOOSEGROUPSFORCOACH_");
                }
            } else if (callbackData.startsWith("EXITADMIN_")) {
                executeEditMessageText("Вы вышли из аккаунта админа", chatId, messageId);
                displayMainMenu(chatId);
            }
        }
    }


    private void addPasswordCoachAdmin(String messageText, Long chatId) {
        if (messageText.equals("1")) {
            displayActivitiesMenu(chatId, "SETPASSWORDCOACH_");
        } else {
            Coach coach = adminCoach.get(chatId);
            coach.setPasswordCoach(messageText);
            adminCoach.put(chatId, coach);
            coachRepository.save(coach);
            sendMessage(chatId, "Тренер сохранен");
            displayAdminMenu(chatId);
        }
    }

    private void displayActivitiesMenu(Long chatId, String callbackData) {//222222222222222
        Coach coach = adminCoach.get(chatId);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выбор активности, которую преподает тренер:");
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        if (coach.getActivity() != null) {
            if (!coach.getActivity().contains("АФК")) {
                InlineKeyboardButton afk = new InlineKeyboardButton();
                afk.setText("АФК\uD83D\uDEFC");
                afk.setCallbackData(callbackData + afk.getText());
                rows.add(List.of(afk));
            }
            if (!coach.getActivity().contains("Футбол")) {
                InlineKeyboardButton football = new InlineKeyboardButton();
                football.setText("Футбол⚽");
                football.setCallbackData(callbackData + football.getText());
                rows.add(List.of(football));
            }
            if (!coach.getActivity().contains("Кикбоксинг")) {
                InlineKeyboardButton kik = new InlineKeyboardButton();
                kik.setText("Кикбоксинг\uD83E\uDD4A");
                kik.setCallbackData(callbackData + kik.getText());
                rows.add(List.of(kik));
            }
            if (!coach.getActivity().contains("Плавание")) {
                InlineKeyboardButton swim = new InlineKeyboardButton();
                swim.setText("Плавание\uD83D\uDC33");
                swim.setCallbackData(callbackData + swim.getText());
                rows.add(List.of(swim));
            }
            if (!coach.getActivity().contains("Баскетбол")) {
                InlineKeyboardButton basket = new InlineKeyboardButton();
                basket.setText("Баскетбол\uD83C\uDFC0");
                basket.setCallbackData(callbackData + basket.getText());
                rows.add(List.of(basket));
            }
            if (!coach.getActivity().contains("Самбо")) {
                InlineKeyboardButton sam = new InlineKeyboardButton();
                sam.setText("Самбо\uD83E\uDD3C");
                sam.setCallbackData(callbackData + sam.getText());
                rows.add(List.of(sam));
            }
            if (!coach.getActivity().contains("Тренажерный-зал")) {
                InlineKeyboardButton gym = new InlineKeyboardButton();
                gym.setText("Тренажерный-зал\uD83C\uDFCB️");
                gym.setCallbackData(callbackData + gym.getText());
                rows.add(List.of(gym));
            }
            if (!coach.getActivity().contains("Вольная-борьба")) {
                InlineKeyboardButton fight = new InlineKeyboardButton();
                fight.setText("Вольная-борьба\uD83E\uDD1C");
                fight.setCallbackData(callbackData + fight.getText());
                rows.add(List.of(fight));
            }
        } else {
            addDefaultActivities(rows, coach, callbackData);
        }
        if (rows.isEmpty()) {
            InlineKeyboardButton next = new InlineKeyboardButton();
            next.setText("Next step");
            next.setCallbackData(callbackData + "next");
            rows.add(List.of(next));
        }
        InlineKeyboardButton nazad = new InlineKeyboardButton();
        nazad.setText("↩️");
        nazad.setCallbackData(callbackData + "назад");
        rows.add(List.of(nazad));
        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("ошибка");
        }
    }

    private void addDefaultActivities(List<List<InlineKeyboardButton>> rows, Coach coach, String callbackData) {
        InlineKeyboardButton afk = new InlineKeyboardButton();
        afk.setText("АФК\uD83D\uDEFC");
        afk.setCallbackData(callbackData + afk.getText());
        rows.add(List.of(afk));

        InlineKeyboardButton football = new InlineKeyboardButton();
        football.setText("Футбол⚽");
        football.setCallbackData(callbackData + football.getText());
        rows.add(List.of(football));

        InlineKeyboardButton kik = new InlineKeyboardButton();
        kik.setText("Кикбоксинг\uD83E\uDD4A");
        kik.setCallbackData(callbackData + kik.getText());
        rows.add(List.of(kik));

        InlineKeyboardButton swim = new InlineKeyboardButton();
        swim.setText("Плавание\uD83D\uDC33");
        swim.setCallbackData(callbackData + swim.getText());
        rows.add(List.of(swim));
        InlineKeyboardButton basket = new InlineKeyboardButton();
        basket.setText("Баскетбол\uD83C\uDFC0");
        basket.setCallbackData(callbackData + basket.getText());
        rows.add(List.of(basket));

        InlineKeyboardButton sam = new InlineKeyboardButton();
        sam.setText("Самбо\uD83E\uDD3C");
        sam.setCallbackData(callbackData + sam.getText());
        rows.add(List.of(sam));

        InlineKeyboardButton gym = new InlineKeyboardButton();
        gym.setText("Тренажерный-зал\uD83C\uDFCB️");
        gym.setCallbackData(callbackData + gym.getText());
        rows.add(List.of(gym));
        InlineKeyboardButton fight = new InlineKeyboardButton();
        fight.setText("Вольная-борьба\uD83E\uDD1C");
        fight.setCallbackData(callbackData + fight.getText());
        rows.add(List.of(fight));

    }

    private void addExperienceCoachAdmin(String messageText, Long chatId) {
        Coach coach = adminCoach.get(chatId);
        coach.setExperience(Long.valueOf(messageText));
        adminCoach.put(chatId, coach);
        getGroupsForAdmin(chatId, "CHOOSEGROUPSFORCOACH_");
    }

    private void addAgeCoachAdmin(String messageText, Long chatId) {
        Coach coach = adminCoach.get(chatId);
        coach.setAge(Long.valueOf(messageText));
        adminCoach.put(chatId, coach);
        sendMessage(chatId, "Введите опыт работы тренера:");
        userCondition.put(chatId, UserCondition.WAITING_FOR_EXPERIENCE_COACH_ADMIN);
    }

    private void addSurnameCoachAdmin(String messageText, Long chatId) {
        Coach coach = adminCoach.get(chatId);
        coach.setSurname(messageText);
        adminCoach.put(chatId, coach);
        sendMessage(chatId, "Введите возраст:");
        userCondition.put(chatId, UserCondition.WAITING_FOR_AGE_COACH_ADMIN);
    }

    private void addNameCoachAdmin(String messageText, Long chatId) {
        Coach coach = adminCoach.get(chatId);
        coach.setName(messageText);
        adminCoach.put(chatId, coach);
        sendMessage(chatId, "Введите фамилию:");
        userCondition.put(chatId, UserCondition.WAITING_FOR_SURNAME_COACH_ADMIN);
    }

    private void addPasswordStudentAdmin(String messageText, Long chatId) {
        Student student = adminStudent.get(chatId);
        student.setPassword(messageText);
        adminStudent.put(chatId, student);
        studentRepository.save(student);
        sendMessage(chatId, "Студент сохранен");
        displayAdminMenu(chatId);
    }

    private void addAgeStudentAdmin(String messageText, Long chatId) {
        Student student = adminStudent.get(chatId);
        student.setAge(Integer.parseInt(messageText));
        adminStudent.put(chatId, student);
        sendMessage(chatId, "Введите пароль для студента:");
        userCondition.put(chatId, UserCondition.WAITING_FOR_PASSWORD_STUDENT_ADMIN);
    }

    private void addSurnameStudentAdmin(String messageText, Long chatId) {
        Student student = adminStudent.get(chatId);
        student.setSurname(messageText);
        adminStudent.put(chatId, student);
        sendMessage(chatId, "Введите возраст:");
        userCondition.put(chatId, UserCondition.WAITING_FOR_AGE_STUDENT_ADMIN);
    }

    private void addNameStudentAdmin(String message, Long chatId) {
        Student student = adminStudent.get(chatId);
        student.setName(message);
        adminStudent.put(chatId, student);
        sendMessage(chatId, "Введите фамилию:");
        userCondition.put(chatId, UserCondition.WAITING_FOR_SURNAME_STUDENT_ADMIN);
    }

    private void checkPasswordAdmin(String messageText, Long chatId) {
        if (messageText.equals("capibara")) {
            sendMessage(chatId, "Пароль верный!");
            displayAdminMenu(chatId);
        } else  sendMessage(chatId, "Пароль неверный! Попробуйте снова...");
    }

    private void getGroupsForAdmin(Long chatId, String callbackData) {
        List<String> groups = studentRepository.findDifferentGroups();
        Coach coach = adminCoach.get(chatId);

        if (coach.getClassGroups() != null) {
            log.info("Уже выбраны некоторые группы: " + coach.getClassGroups());
            log.info("Стартовый массив групп " + groups);

            int i = 0;
            while (i < groups.size()) {
                if (coach.getClassGroups().contains(groups.get(i))) {
                    groups.remove(i);
                } else {
                    i++;
                }
            }

            log.info("Итоговый массив групп " + groups);

            if (groups.isEmpty()) {
                SendMessage message = new SendMessage();
                message.setChatId(String.valueOf(chatId));
                message.setText("Все группы уже выбраны");
                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
                List<InlineKeyboardButton> buttonsInLine = new ArrayList<>();
                InlineKeyboardButton group = new InlineKeyboardButton();
                group.setText("Next step");
                group.setCallbackData(callbackData + coach.getClassGroups());
                buttonsInLine.add(group);

                rowsInLine.add(buttonsInLine);
                markup.setKeyboard(rowsInLine);
                message.setReplyMarkup(markup);
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    log.error("ошибка");
                }
            }
        }

        if (!groups.isEmpty()) {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("Выбор группы:");
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
            List<InlineKeyboardButton> buttonsInLine = new ArrayList<>();
            for (int i = 0; i < groups.size(); i++) {
                if (groups.get(i) != null) {
                    InlineKeyboardButton group = new InlineKeyboardButton();
                    group.setText(groups.get(i));
                    group.setCallbackData(callbackData + groups.get(i));
                    buttonsInLine.add(group);
                }
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
    }

    private void displayAdminMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите действие:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton addStudent = new InlineKeyboardButton();
        addStudent.setText("Добавить ученика в группу \uD83E\uDE84\n");
        addStudent.setCallbackData("ADDSTUDENTADMIN_");
        rows.add(List.of(addStudent));

        InlineKeyboardButton addCoach = new InlineKeyboardButton();
        addCoach.setText("Добавить тренера \uD83E\uDE84\n");
        addCoach.setCallbackData("ADDCOACHADMIN_");
        rows.add(List.of(addCoach));

        InlineKeyboardButton exitAdmin = new InlineKeyboardButton();
        exitAdmin.setText("Выйти из аккаунта \uD83E\uDEE1\n");
        exitAdmin.setCallbackData("EXITADMIN_");
        rows.add(List.of(exitAdmin));

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при выводе меню студента:" + e.getMessage());
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

    private void chooseActivity(Long chatId, Coach coach, String callbackData) {
        String[] activities = coach.getActivity().split(" ");
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выбор активности:");
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> buttonsInLines = new ArrayList<>();
        for (int i = 0; i < activities.length; i++) {
            List<InlineKeyboardButton> buttonsInLine = new ArrayList<>();
            InlineKeyboardButton activity = new InlineKeyboardButton();
            activity.setText(activities[i]);
            activity.setCallbackData(callbackData + activities[i]);
            buttonsInLine.add(activity);
            rowsInLine.add(buttonsInLine);
        }
        InlineKeyboardButton nazad = new InlineKeyboardButton();
        nazad.setText("↩️");
        nazad.setCallbackData(callbackData + "назад");
        buttonsInLines.add(nazad);
        rowsInLine.add(buttonsInLines);
        markup.setKeyboard(rowsInLine);
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("ошибка");
        }

    }

    private void displayTimeEnd(Long chatId, String timeOfEnd, String callbackData) {
        LocalTime timeOfTraining = LocalTime.parse(timeOfEnd);

        String group = coachTraining.get(chatId).getClassGroup();
        LocalDate chosenDate = coachTraining.get(chatId).getDate();

        List<Training> groupTrainings = trainingRepository.trainingByClassGroup(group);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> buttonsInLine = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            timeOfTraining = timeOfTraining.plusHours(1L);
            if (canEnd(timeOfTraining, chosenDate, groupTrainings)) {
                InlineKeyboardButton time = new InlineKeyboardButton();
                time.setText(timeOfTraining.toString());
                time.setCallbackData(callbackData + timeOfTraining);
                buttonsInLine.add(time);
            }
        }
        InlineKeyboardButton nazad = new InlineKeyboardButton();
        nazad.setText("↩️");
        nazad.setCallbackData(callbackData + "назад");
        buttonsInLine.add(nazad);
        rowsInLine.add(buttonsInLine);
        if (buttonsInLine.isEmpty()) {
            sendMessage(chatId, "Все время занято");
            displayCoachMenu(chatId);
        } else {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("Выбор конца тренировки:");
            markup.setKeyboard(rowsInLine);
            message.setReplyMarkup(markup);
            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error("Ошибка при выводе возможных дат тренировки");
            }
        }
    }

    private void displayTime(Long chatId, String callbackData) {
        LocalTime timeOfTraining = LocalTime.parse("10:00");

        String group = coachTraining.get(chatId).getClassGroup();
        LocalDate chosenDate = coachTraining.get(chatId).getDate();

        List<Training> groupTrainings = trainingRepository.trainingByClassGroup(group);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> buttonsInLine = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            if (canStart(timeOfTraining, chosenDate, groupTrainings)) {
                InlineKeyboardButton time = new InlineKeyboardButton();
                time.setText(timeOfTraining.toString());
                time.setCallbackData(callbackData + timeOfTraining);
                buttonsInLine.add(time);
            }
            timeOfTraining = timeOfTraining.plusHours(1L);
        }

        InlineKeyboardButton nazad = new InlineKeyboardButton();
        nazad.setText("↩️");
        nazad.setCallbackData(callbackData + "назад");
        buttonsInLine.add(nazad);
        rowsInLine.add(buttonsInLine);
        if (buttonsInLine.isEmpty()) {
            sendMessage(chatId, "Все время занято");
            displayCoachMenu(chatId);
        } else {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("Выбор начала тренировки:");
            markup.setKeyboard(rowsInLine);
            message.setReplyMarkup(markup);
            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error("Ошибка при выводе возможных дат тренировки");
            }
        }
    }

    private boolean canStart(LocalTime timeOfTraining, LocalDate chosenDate, List<Training> groupTrainings) {
        for (Training training : groupTrainings) {
            if (training.getDate().equals(chosenDate)) {
                log.info("Даты совпали!");
                if (timeOfTraining.isAfter(training.getStartTime()) && timeOfTraining.isBefore(training.getEndTime())) {
                    return false;
                }
                if (timeOfTraining.equals(training.getStartTime())) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean canEnd(LocalTime timeOfTraining, LocalDate chosenDate, List<Training> groupTrainings) {
        for (Training training : groupTrainings) {
            if (training.getDate().equals(chosenDate)) {
                log.info("Даты совпали!");
                if (timeOfTraining.isAfter(training.getStartTime())) {
                    return false;
                }
            }
        }
        return true;
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
        for (int i = 0; i < 7; i++) {
            currentDate = currentDate.plusDays(1L);
            InlineKeyboardButton date = new InlineKeyboardButton();
            date.setText(currentDate.getDayOfMonth() + "." + String.valueOf(currentDate.getMonth().ordinal() + 1));
            date.setCallbackData(callbackData + currentDate.getDayOfMonth() + "." + String.valueOf(currentDate.getMonth().ordinal() + 1));
            buttonsInLine.add(date);
        }
        InlineKeyboardButton nazad = new InlineKeyboardButton();
        nazad.setText("↩️");
        nazad.setCallbackData(callbackData + "назад");
        buttonsInLine.add(nazad);
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
        InlineKeyboardButton nazad = new InlineKeyboardButton();
        nazad.setText("↩️");
        nazad.setCallbackData(callbackData + "назад");
        buttonsInLine.add(nazad);
        rowsInLine.add(buttonsInLine);
        markup.setKeyboard(rowsInLine);
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("ошибка в выборе группы");
        }
    }

    private void getStudentsInGroupCoach(Long chatId, String group, String callbackData) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Список студентов в группе " + group);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> buttonsInLine = new ArrayList<>();
        List<Student> students = studentController.getStudentsByGroup(group);
        for (int i = 0; i < students.size(); i++) {
            InlineKeyboardButton student = new InlineKeyboardButton();
            student.setText(students.get(i).getName() + " " + students.get(i).getSurname());
            student.setCallbackData(callbackData + students.get(i).getId());
            rows.add(List.of(student));

        }
        rows.add(buttonsInLine);
        InlineKeyboardButton nazad = new InlineKeyboardButton();
        nazad.setText("↩️");
        nazad.setCallbackData(callbackData + "назад");
        buttonsInLine.add(nazad);
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
        List<InlineKeyboardButton> buttonsInLine = new ArrayList<>();

        List<Coach> coaches = coachController.getAllCoaches();
        for (int i = 0; i < coaches.size(); i++) {
            InlineKeyboardButton coach = new InlineKeyboardButton();
            coach.setText(coaches.get(i).getName() + " " + coaches.get(i).getSurname());
            coach.setCallbackData(callbackData + coaches.get(i).getId());
            rows.add(List.of(coach));
        }
        InlineKeyboardButton nazad = new InlineKeyboardButton();
        nazad.setText("↩️");
        nazad.setCallbackData(callbackData + "назад");
        buttonsInLine.add(nazad);
        rows.add(buttonsInLine);
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
            if (groups.get(i) != null) {
                InlineKeyboardButton group = new InlineKeyboardButton();
                group.setText(groups.get(i));
                group.setCallbackData(callbackData + groups.get(i));
                buttonsInLine.add(group);
            }
        }
        InlineKeyboardButton nazad = new InlineKeyboardButton();
        nazad.setText("↩️");
        nazad.setCallbackData(callbackData + "назад");
        buttonsInLine.add(nazad);
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
        regStudent.setText("Зарегистрироваться как студент \uD83E\uDD20\n");
        regStudent.setCallbackData("REGSTUDENT_");
        rows.add(List.of(regStudent));

        InlineKeyboardButton regCoach = new InlineKeyboardButton();
        regCoach.setText("Зарегистрироваться как тренер \uD83D\uDDFF\n");
        regCoach.setCallbackData("REGCOACH_");
        rows.add(List.of(regCoach));
        InlineKeyboardButton regAdmin = new InlineKeyboardButton();
        regAdmin.setText("Зарегистрироваться как админ \uD83D\uDE0E\n");
        regAdmin.setCallbackData("REGADMIN_");
        rows.add(List.of(regAdmin));

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
        exit.setText("Выйти из аккаунта \uD83D\uDEAA\n");
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
        log.info(coach.toString());
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
        InlineKeyboardButton nazad = new InlineKeyboardButton();
        nazad.setText("↩️");
        nazad.setCallbackData("STUDENTGROUP_" + "назад");
        buttonsInLine.add(nazad);
        rows.add(buttonsInLine);
        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при выводе списка студентов:" + e.getMessage());
        }
    }

    private void сhooseActivity(Long chatId, String callbackData) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите активность:");

        Coach coach = coachRepository.coachByChatId(chatId).get(0);
        String[] activities = coach.getActivity().split(" ");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> buttonsInLine = new ArrayList<>();

        for (int i = 0; i < activities.length; i++) {
            InlineKeyboardButton activity = new InlineKeyboardButton();
            activity.setText(activities[i]);
            activity.setCallbackData(callbackData + activities[i]);
            rows.add(List.of(activity));
        }
        InlineKeyboardButton nazad = new InlineKeyboardButton();
        nazad.setText("↩️");
        nazad.setCallbackData(callbackData + "назад");
        buttonsInLine.add(nazad);
        rows.add(buttonsInLine);
        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при выводе активностей тренера" + e.getMessage());
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
                "\uD83D\uDC94получать уведомления (напоминания) о тренировках (за день до тренировки) \n" +
                "\uD83D\uDC94просматривать свои группы \n" +
                "\uD83D\uDC94ставить оценки за тренировку \n\n" +
                "Для ученика:  \n" +
                "\uD83D\uDC94получать уведомления (за день до тренировки) \n" +
                "\uD83D\uDC94получать уведомления о добавлении оценки \n" +
                "\uD83D\uDC94просматривать расписание \n" +
                "\uD83D\uDC94просматривать свои оценки\n\n" +
                "Если вы админ: \n" +
                "\uD83D\uDC94добавлять нового тренера \n" +
                "\uD83D\uDC94добавлять нового ученика в конкретную группу \n";

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

    private void addMark(int mark, Long studentId, Coach coach, String activity) throws Exception {
        Mark mark1 = markService.addMark(mark, coach, studentId, activity);

        if (mark1.getStudent().getChatId() != null) {
            String text =
                    "Тренер: " + mark1.getCoach().getName() + " " + mark1.getCoach().getSurname() + "\n"+"поставил вам оценку: " + mark + "\n"+ "по предмету: " + activity;
            sendMessage(mark1.getStudent().getChatId(), text);
        }
    }

    private void addTraining(Training training) {
        trainingService.addTraining(training);

        List<Student> students = studentRepository.findByGroup(training.getClassGroup());

        String text = "Вам добавили тренировку:\n" +
                "Вид тренировки: " + training.getActivity() + "\n" +
                "Дата: " + training.getDate() + "\n" +
                "Время: " + training.getStartTime() + " " + training.getEndTime() + "\n" +
                "Тренер: " + training.getCoach().getName() + " " + training.getCoach().getSurname();

        for (int i = 0; i < students.size(); i++) {
            if (students.get(i).getChatId() != null) {
                sendMessage(students.get(i).getChatId(), text);
            }
        }
    }

    @Scheduled(fixedDelay = 86400000)
    private void sendNotificationDayBeforeTraining() {
        List<Training> trainings = trainingRepository.findAll();
        for (int i = 0; i < trainings.size(); i++) {
            // проверяем, что тренировка будет завтра
            if (trainings.get(i).getDate().equals(LocalDate.now().plusDays(1))) {
                if (trainings.get(i).getCoach().getChatId() != null) {
                    String textForCoach = "Завтра у вас тренировка:\n" +
                            "Вид тренировки: " + trainings.get(i).getActivity() + "\n" +
                            "Дата: " + trainings.get(i).getDate() + "\n" +
                            "Время: " + trainings.get(i).getStartTime() + " " + trainings.get(i).getEndTime() + "\n" +
                            "Группа: " + trainings.get(i).getClassGroup();
                    sendMessage(trainings.get(i).getCoach().getChatId(), textForCoach);
                }
                String text = "Завтра у вас тренировка:\n" +
                        "Вид тренировки: " + trainings.get(i).getActivity() + "\n" +
                        "Дата: " + trainings.get(i).getDate() + "\n" +
                        "Время: " + trainings.get(i).getStartTime() + " " + trainings.get(i).getEndTime() + "\n" +
                        "Тренер: " + trainings.get(i).getCoach().getName() + " " + trainings.get(i).getCoach().getSurname();

                List<Student> studentsInGroup = studentRepository.findByGroup(trainings.get(i).getClassGroup());
                for (int j = 0; j < studentsInGroup.size(); j++) {
                    log.info("Sending message for student " + studentsInGroup.get(j).getSurname());
                    sendMessage(studentsInGroup.get(j).getChatId(), text);
                }
            }
        }
    }

    /*
    @Scheduled(fixedDelay = 3600000)
    private void sendNotificationHourBeforeTraining() {
        List<Training> trainings = trainingRepository.findAll();

        for (int i = 0; i < trainings.size(); i++) {
            // проверяем, что тренировка будет сегодня
            if (trainings.get(i).getDate().equals(LocalDate.now())) {
                // проверяем, что тренировка в течение часа будет тренировка
                if (trainings.get(i).getStartTime().isBefore(LocalTime.now().plusHours(1))) {
                    if (trainings.get(i).getCoach().getChatId() != null) {
                        String textForCoach = "В течение часа у вас будет тренировка:\n" +
                                "Вид тренировки: " + trainings.get(i).getActivity() + "\n" +
                                "Дата: " + trainings.get(i).getDate() + "\n" +
                                "Время: " + trainings.get(i).getStartTime() + " " + trainings.get(i).getEndTime() + "\n" +
                                "Группа: " + trainings.get(i).getClassGroup();
                        sendMessage(trainings.get(i).getCoach().getChatId(), textForCoach);
                    }
                    String text = "В течение часа у вас будет тренировка:\n" +
                            "Вид тренировки: " + trainings.get(i).getActivity() + "\n" +
                            "Дата: " + trainings.get(i).getDate() + "\n" +
                            "Время: " + trainings.get(i).getStartTime() + " " + trainings.get(i).getEndTime() + "\n" +
                            "Тренер: " + trainings.get(i).getCoach().getName() + " " + trainings.get(i).getCoach().getSurname();

                    List<Student> studentsInGroup = studentRepository.findByGroup(trainings.get(i).getClassGroup());
                    for (int j = 0; j < studentsInGroup.size(); j++) {
                        log.info("Sending message for student " + studentsInGroup.get(j).getSurname());
                        sendMessage(studentsInGroup.get(j).getChatId(), text);
                    }
                }
            }
        }
    }*/
}
