package com.xxwn.bbrulesbot.bot;

import com.xxwn.bbrulesbot.rag.RulesQAService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageListener extends ListenerAdapter {

    private final RulesQAService rulesQAService;

    @Async
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!isMentioningBot(event)) return;

        String question = extractQuestion(event.getMessage());
        if (question.isBlank()) {
            event.getMessage().reply("질문을 입력해주세요.").queue();
            return;
        }

        log.info("Discord 질문 수신 - user: {}, question: {}",
                event.getAuthor().getName(), question);

        event.getChannel().sendTyping().queue();

        try {
            String answer = rulesQAService.ask(question);
            event.getMessage().reply(answer).queue();
        } catch (Exception e) {
            log.error("답변 생성 중 오류 발생 - user: {}, question: {}",
                    event.getAuthor().getName(), question, e);
            event.getMessage().reply("죄송합니다. 답변 생성 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.").queue();
        }
    }

    private boolean isMentioningBot(MessageReceivedEvent event) {
        return event.getMessage().getMentions()
                .isMentioned(event.getJDA().getSelfUser());
    }

    private String extractQuestion(Message message) {
        return message.getContentStripped()
                .replaceAll("@\\S+\\s*", "").trim();
    }
}
