package com.xxwn.bbrulesbot.bot;

import com.xxwn.bbrulesbot.rag.RulesQAService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.entities.Mentions;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageListenerTest {

    @Mock
    private RulesQAService rulesQAService;

    @Mock
    private MessageReceivedEvent event;

    @Mock
    private Message message;

    @Mock
    private User author;

    @Mock
    private SelfUser selfUser;

    @Mock
    private JDA jda;

    @Mock
    private Mentions mentions;

    @Mock
    private MessageChannelUnion channel;

    @Mock
    private MessageCreateAction messageCreateAction;

    private MessageListener messageListener;

    @BeforeEach
    void setUp() {
        messageListener = new MessageListener(rulesQAService);
    }

    @Test
    @DisplayName("봇 멘션 포함 메시지가 오면 RulesQAService.ask() 호출")
    void onMessageReceived_withBotMention_callsRulesQAService() {
        // given
        when(event.getAuthor()).thenReturn(author);
        when(author.isBot()).thenReturn(false);
        when(event.getMessage()).thenReturn(message);
        when(message.getMentions()).thenReturn(mentions);
        when(event.getJDA()).thenReturn(jda);
        when(jda.getSelfUser()).thenReturn(selfUser);
        when(mentions.isMentioned(selfUser)).thenReturn(true);
        when(message.getContentStripped()).thenReturn("인필드플라이란 무엇인가요?");
        when(rulesQAService.ask(anyString())).thenReturn("답변입니다");
        when(message.reply(anyString())).thenReturn(messageCreateAction);
        when(event.getChannel()).thenReturn(channel);
        when(channel.sendTyping()).thenReturn(mock());

        // when
        messageListener.onMessageReceived(event);

        // then
        verify(rulesQAService).ask("인필드플라이란 무엇인가요?");
    }

    @Test
    @DisplayName("멘션 없으면 서비스 호출 안 함")
    void onMessageReceived_withoutMention_doesNotCallService() {
        // given
        when(event.getAuthor()).thenReturn(author);
        when(author.isBot()).thenReturn(false);
        when(event.getMessage()).thenReturn(message);
        when(message.getMentions()).thenReturn(mentions);
        when(event.getJDA()).thenReturn(jda);
        when(jda.getSelfUser()).thenReturn(selfUser);
        when(mentions.isMentioned(selfUser)).thenReturn(false);

        // when
        messageListener.onMessageReceived(event);

        // then
        verify(rulesQAService, never()).ask(anyString());
    }

    @Test
    @DisplayName("멘션 부분을 제거한 질문 텍스트만 서비스에 전달")
    void onMessageReceived_withBotMention_extractsQuestion() {
        // given
        when(event.getAuthor()).thenReturn(author);
        when(author.isBot()).thenReturn(false);
        when(event.getMessage()).thenReturn(message);
        when(message.getMentions()).thenReturn(mentions);
        when(event.getJDA()).thenReturn(jda);
        when(jda.getSelfUser()).thenReturn(selfUser);
        when(mentions.isMentioned(selfUser)).thenReturn(true);
        // getContentStripped() removes mentions - should pass clean text
        when(message.getContentStripped()).thenReturn("@룰봇 보크란 무엇인가요?");
        when(rulesQAService.ask(anyString())).thenReturn("답변입니다");
        when(message.reply(anyString())).thenReturn(messageCreateAction);
        when(event.getChannel()).thenReturn(channel);
        when(channel.sendTyping()).thenReturn(mock());

        // when
        messageListener.onMessageReceived(event);

        // then — 현재 코드는 getContentStripped()를 그대로 전달하므로 멘션 텍스트가 포함됨
        // 멘션 부분을 제거한 순수 질문만 전달해야 함
        verify(rulesQAService).ask("보크란 무엇인가요?");
    }

    @Test
    @DisplayName("봇 자신의 메시지는 무시")
    void onMessageReceived_fromBot_doesNotCallService() {
        // given
        when(event.getAuthor()).thenReturn(author);
        when(author.isBot()).thenReturn(true);

        // when
        messageListener.onMessageReceived(event);

        // then
        verify(rulesQAService, never()).ask(anyString());
    }
}
