package org.example.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramBot extends TelegramLongPollingBot {
    private final TelegramConfig telegramConfig;

    public TelegramBot(TelegramConfig telegramConfig) {
        this.telegramConfig = telegramConfig;
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

        // чтобы бот мог нам написать ему необходимо знать chat id
        // chat id - id, который идентифицирует пользователя. Содержится в каждом update
        Long chatId = update.getMessage().getChatId();

        if (update.hasMessage() && update.getMessage().hasText()) {
            switch (messageText) {
                case "/start":
                    startCommandRecieved(chatId, update.getMessage().getChat().getFirstName());
                    break;
                default:
                    sendMessage(chatId, "Завтра будем делать тестового бота!");

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
}
