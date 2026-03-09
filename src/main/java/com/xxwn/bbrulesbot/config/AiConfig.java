package com.xxwn.bbrulesbot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        당신은 KBO/MLB 야구 규칙 전문가입니다.
                        제공된 규칙서 내용을 우선적으로 참고하여 답변하세요.
                        규칙서에서 찾을 수 없는 경우, 야구 규칙 전문가로서의 일반 지식을 바탕으로 답변하되,
                        반드시 "규칙서에 명시되지 않은 내용으로, 일반 규정에 따르면" 이라고 먼저 밝히세요.
                        답변은 한국어로 해주세요.
                        """)
                .build();
    }
}
