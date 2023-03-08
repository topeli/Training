package org.example.bot;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@Getter
@PropertySource("application.properties")
public class TelegramConfig {
    @Value("${bot.name}")
    private String botName;

    @Value("${bot.token}")
    private String botToken;
}