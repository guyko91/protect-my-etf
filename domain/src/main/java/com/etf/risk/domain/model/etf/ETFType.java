package com.etf.risk.domain.model.etf;

public enum ETFType {
    INDEX("지수형", "시장 전체 또는 대표 지수 추종"),
    SECTOR("섹터형", "산업군, 업종별"),
    THEMATIC("테마형", "특정 트렌드, 미래산업군"),
    BOND("채권형", "국채, 회사채 등"),
    COMMODITY("원자재", "금, 원유 등 실물자산 추종"),
    REIT("리츠", "상장리츠, 부동산 직접 투자"),
    LEVERAGED("레버리지형", "지수의 2~3배 수익률 추종"),
    INVERSE("인버스형", "지수 반대방향 추종"),
    DIVIDEND("배당형", "고배당주"),
    GLOBAL("글로벌", "해외 시장, 국가별"),
    SMART_BETA("스마트베타", "요인별 맞춤형 전략"),
    CEF("폐쇄형펀드", "Closed-End Fund"),
    COVERED_CALL("커버드콜", "옵션 프리미엄 수익 추구");

    private final String displayName;
    private final String description;

    ETFType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }
}
