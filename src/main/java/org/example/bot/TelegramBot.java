package org.example.bot;

import lombok.extern.slf4j.Slf4j;
import org.example.controllers.CoachController;
import org.example.controllers.MarkController;
import org.example.controllers.StudentController;
import org.example.models.Coach;
import org.example.models.Mark;
import org.example.models.Student;
import org.example.services.MarkService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
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

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {
    private final TelegramConfig telegramConfig;
    private final StudentController studentController;
    private final CoachController coachController;
    private final MarkController markController;
    private final MarkService markService;

    public TelegramBot(TelegramConfig telegramConfig, StudentController studentController, CoachController coachController, MarkController markController, MarkService markService) {
        this.telegramConfig = telegramConfig;
        this.studentController = studentController;
        this.coachController = coachController;
        this.markController = markController;
        this.markService = markService;
        log.info("Начинаем добавлять меню");
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Start work"));
        listOfCommands.add(new BotCommand("/add_student", "Add student"));
        listOfCommands.add(new BotCommand("/add_coach", "Add coach"));
        listOfCommands.add(new BotCommand("/get_students", "Show students list"));
        listOfCommands.add(new BotCommand("/get_coaches", "Show coaches list"));
        listOfCommands.add(new BotCommand("/get_marks", "Show marks list"));
        listOfCommands.add(new BotCommand("/add_marks", "Add mark"));
        listOfCommands.add(new BotCommand("/key_buttons", "Buttons!"));
        listOfCommands.add(new BotCommand("/put_mark", "Put mark"));
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
                    break;
                case "/add_student":
                    addStudent(chatId);
                    break;
                case "/add_coach":
                    addCoach(chatId);
                    break;
                case "/get_coaches":
                    getAllCoaches(chatId);
                    break;
                case "/get_students":
                    getAllStudents(chatId);
                    break;
                case "/add_mark":
                    try {
                        addMark(chatId);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "/get_marks":
                    getAllMarks(chatId);
                    break;
                case "/key_buttons":
                    buttons(chatId);
                    break;
                case "/put_mark":
                    putMark(chatId);
                    break;
                default:
                    sendMessage(chatId, "То что вы присали не соответствует ни одной команде");
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            int messageId = update.getCallbackQuery().getMessage().getMessageId();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            /*if(callbackData.equals("YES")) {
                String text = "You pressed YES";
                executeEditMessageText(text, chatId, messageId);
            }
            else if(callbackData.equals("NO"))
            {
                String text = "You pressed NO";
                executeEditMessageText(text, chatId, messageId);
            }*/
            if(callbackData.equals("STUDENT")) {
                List<Student> students = studentController.getAllStudents();
                String text = "Выбор оценки:";
                    executeEditMessageText(text, chatId, messageId);
            }
        }
    }

    private void putMark(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Кому вы хотите добавить оценку:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> buttonsInLine = new ArrayList<>();

        List<Student> students = studentController.getAllStudents();
        for (int i = 0; i < students.size(); i++){
            InlineKeyboardButton student = new InlineKeyboardButton();
            student.setText(students.get(i).getName() + " " + students.get(i).getSurname());
            student.setCallbackData("STUDENT");
            buttonsInLine.add(student);
        }
        rowsInLine.add(buttonsInLine);
        markup.setKeyboard(rowsInLine);
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
        }
    }

    private void executeEditMessageText(String text, Long chatId, int messageId) {
        EditMessageText messageText = new EditMessageText();
        messageText.setText(text);
        messageText.setMessageId(messageId);
        messageText.setChatId(String.valueOf(chatId));

        try
        {
            execute(messageText);
        }
        catch (TelegramApiException e)
        {

        }
    }

    private void buttons(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Test buttons");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> buttonsInLine = new ArrayList<>();

        InlineKeyboardButton yes = new InlineKeyboardButton();
        yes.setText("yes");
        yes.setCallbackData("YES");
        buttonsInLine.add(yes);

        InlineKeyboardButton no = new InlineKeyboardButton();
        no.setText("no");
        no.setCallbackData("NO");
        buttonsInLine.add(no);

        rowsInLine.add(buttonsInLine);
        markup.setKeyboard(rowsInLine);

        message.setReplyMarkup(markup);
        try {
            execute(message);
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

    private void addStudent(Long chatId) {
        Student student = new Student("Антон", "Кисляков", 21, "10-3");
        studentController.addStudent(student);
        sendMessage(chatId, "Студент сохранен");
    }

    private void getAllStudents(Long chatId) {
        List<Student> students = studentController.getAllStudents();
        String text = "";
        for (int i = 0; i < students.size(); i++) {
            text += students.get(i).getName() + " " + students.get(i).getSurname() + " " + students.get(i).getAge() + " " + students.get(i).getClassGroup() + '\n';
        }
        sendMessage(chatId, text);
    }

    private void addCoach(Long chatId) {
        Coach coach = new Coach("Антон", "Кисляков", 21L, 2L);
        coachController.addCoach(coach);
        sendMessage(chatId, "Тренер сохранен");
    }

    private void getAllCoaches(Long chatId) {
        List<Coach> coaches = coachController.getAllCoaches();
        String text = "";
        for (int i = 0; i < coaches.size(); i++) {
            text += coaches.get(i).getName() + " " + coaches.get(i).getSurname() + " " + coaches.get(i).getAge() + " " + coaches.get(i).getExperience() + '\n';
        }
        sendMessage(chatId, text);
    }

    private void addMark(Long chatId) throws Exception {
        markService.addMark(5, "Александр", "Антон");
        sendMessage(chatId, "Оценка сохранена");
    }

    private void getAllMarks(Long chatId) {
        List<Mark> marks = markController.getAllMarks();
        String text = "";
        for (int i = 0; i < marks.size(); i++) {
            text += marks.get(i).getMark() + " " + marks.get(i).getStudent() + " " + marks.get(i).getCoach() + '\n';
        }
        sendMessage(chatId, text);
    }
}
