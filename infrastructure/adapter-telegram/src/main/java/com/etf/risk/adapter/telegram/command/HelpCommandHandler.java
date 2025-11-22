package com.etf.risk.adapter.telegram.command;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class HelpCommandHandler implements CommandHandler {

    @Override
    public String getCommand() {
        return "/help";
    }

    @Override
    public String getDescription() {
        return "도움말 표시";
    }

    @Override
    public String handle(Update update) {
        return """
            ETF 리스크 분석 봇 도움말

            [명령어 목록]
            /start - 봇 시작 및 안내
            /register - 사용자 등록
            /portfolio - 내 포트폴리오 조회
            /add [심볼] [수량] [평단가] - 포지션 추가
              예: /add GOF 100 20.5
            /remove [심볼] - 포지션 전량 매도
              예: /remove GOF
            /risk - 포트폴리오 리스크 분석
            /help - 이 도움말 표시

            [지원 ETF]
            - GOF: Guggenheim Strategic Opportunities Fund (CEF)
            - QQQI: NEOS Nasdaq 100 High Income ETF

            [리스크 레벨]
            - LOW: 안정
            - MEDIUM: 주의 필요
            - HIGH: 위험
            - CRITICAL: 즉시 조치 필요""";
    }
}
