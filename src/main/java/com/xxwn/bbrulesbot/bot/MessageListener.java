package com.xxwn.bbrulesbot.bot;

import com.xxwn.bbrulesbot.rag.RulesQAService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageListener extends ListenerAdapter {

    private final RulesQAService rulesQAService;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // 봇 메시지 무시
        if (event.getAuthor().isBot()) return;

        // 봇 멘션(@룰봇)이 포함된 경우에만 응답
        boolean isMentioned = event.getMessage().getMentions()
                .isMentioned(event.getJDA().getSelfUser());
        if (!isMentioned) return;

        String question = event.getMessage().getContentStripped().trim();
        if (question.isBlank()) {
            event.getMessage().reply("질문을 입력해주세요.").queue();
            return;
        }

        log.info("Discord 질문 수신 - user: {}, question: {}",
                event.getAuthor().getName(), question);

        // 답변 생성 중 표시
        event.getChannel().sendTyping().queue();

        String answer = rulesQAService.ask(question);
        event.getMessage().reply(answer).queue();
    }
}
