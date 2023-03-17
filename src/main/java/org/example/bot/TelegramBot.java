package org.example.bot;

import org.example.controllers.CoachController;
import org.example.controllers.MarkController;
import org.example.controllers.StudentController;
import org.example.models.Coach;
import org.example.models.Mark;
import org.example.models.Student;
import org.example.services.MarkService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Component
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
        String messageText = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();

        if (update.hasMessage() && update.getMessage().hasText()) {
            switch (messageText) {
                case "/start":
                    startCommandRecieved(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case "/addStudent":
                    addStudent(chatId);
                    break;
                case "/addCoach":
                    addCoach(chatId);
                    break;
                case "/getCoaches":
                    getAllCoaches(chatId);
                    break;
                case "/getStudents":
                    getAllStudents(chatId);
                    break;
                case "/addMark":
                    try {
                        addMark(chatId);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "/getMarks":
                    getAllMarks(chatId);
                default:
                    sendMessage(chatId, "То что вы присали не соответствует ни одной команде");
            }
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
    private void addStudent(Long chatId){
        Student student = new Student("Антон", "Кисляков", 21, "10-3");
        studentController.addStudent(student);
        sendMessage(chatId, "Студент сохранен");
    }
    private void getAllStudents(Long chatId){
        List<Student> students = studentController.getAllStudents();
        String text = "";
        for(int i = 0; i<students.size(); i++){
            text += students.get(i).getName() + " " + students.get(i).getSurname() + " " + students.get(i).getAge() + " " + students.get(i).getClassGroup() + '\n';
        }
        sendMessage(chatId, text);
    }
    private void addCoach(Long chatId){
        Coach coach = new Coach("Антон", "Кисляков", 21L, 2L);
        coachController.addCoach(coach);
        sendMessage(chatId, "Тренер сохранен");
    }
    private void getAllCoaches(Long chatId){
        List<Coach> coaches = coachController.getAllCoaches();
        String text = "";
        for(int i = 0; i<coaches.size(); i++){
            text += coaches.get(i).getName() + " " + coaches.get(i).getSurname() + " " + coaches.get(i).getAge()  + " " + coaches.get(i).getExperience() + '\n';
        }
        sendMessage(chatId, text);
    }
    private void addMark(Long chatId) throws Exception {
        markService.addMark(5, "Александр","Антон");
        sendMessage(chatId, "Оценка сохранена");
    }
    private void getAllMarks(Long chatId){
        List<Mark> marks = markController.getAllMarks();
        String text = "";
        for(int i = 0; i<marks.size(); i++){
            text += marks.get(i).getMark() + " " + marks.get(i).getStudent() + " " +  marks.get(i).getCoach() + '\n';
        }
        sendMessage(chatId, text);
    }
}
