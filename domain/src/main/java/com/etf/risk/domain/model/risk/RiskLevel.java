package com.etf.risk.domain.model.risk;

public enum RiskLevel {
    LOW("안정", "정상 범위 내"),
    MEDIUM("주의", "주의가 필요한 범위"),
    HIGH("경고", "위험 범위"),
    CRITICAL("위급", "즉시 조치 필요");

    private final String displayName;
    private final String description;

    RiskLevel(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isHigherThan(RiskLevel other) {
        return this.ordinal() > other.ordinal();
    }

    public boolean isLowerThan(RiskLevel other) {
        return this.ordinal() < other.ordinal();
    }

    public static RiskLevel max(RiskLevel level1, RiskLevel level2) {
        return level1.ordinal() >= level2.ordinal() ? level1 : level2;
    }
}
