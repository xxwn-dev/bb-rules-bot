package com.xxwn.bbrulesbot.bot;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class DiscordBotConfig {

    @Bean
    public JDA jda(@Value("${discord.bot-token}") String token,
                   EventListener messageListener) throws InterruptedException {
        JDA jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(messageListener)
                .build()
                .awaitReady();
        log.info("Discord 봇 연결 완료: {}", jda.getSelfUser().getName());
        return jda;
    }
}
