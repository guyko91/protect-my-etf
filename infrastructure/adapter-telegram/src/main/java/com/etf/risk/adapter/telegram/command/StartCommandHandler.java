package com.etf.risk.adapter.telegram.command;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class StartCommandHandler implements CommandHandler {

    @Override
    public String getCommand() {
        return "/start";
    }

    @Override
    public String getDescription() {
        return "봇 시작 및 안내";
    }

    @Override
    public String handle(Update update) {
        String firstName = update.getMessage().getFrom().getFirstName();
        return String.format("""
            안녕하세요, %s님!

            ETF 리스크 분석 봇입니다.
            GOF와 QQQI ETF의 리스크를 분석하고 배당 알림을 제공합니다.

            사용 가능한 명령어:
            /register - 사용자 등록
            /portfolio - 포트폴리오 조회
            /add [심볼] [수량] [평단가] - 포지션 추가
            /remove [심볼] - 포지션 제거
            /risk - 리스크 분석
            /help - 도움말

            먼저 /register 명령어로 등록해주세요.""", firstName);
    }
}
