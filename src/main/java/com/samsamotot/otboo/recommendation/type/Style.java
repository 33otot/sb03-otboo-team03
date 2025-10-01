package com.samsamotot.otboo.recommendation.type;

public enum Style {

    CASUAL("캐주얼"),
    FORMAL("포멀"),
    SPORTY("스포티"),
    CHIC("시크"),
    VINTAGE("빈티지"),
    CLASSIC("클래식"),
    MINIMAL("미니멀");

    private final String name;

    Style(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Style fromName(String name) {
        for (Style style : Style.values()) {
            if (style.name().equalsIgnoreCase(name) || style.getName().equalsIgnoreCase(name)) {
                return style;
            }
        }
        throw new IllegalArgumentException("Invalid style name: " + name);
    }

    // 유사 스타일 정의
    public boolean isSimilar(Style other) {
        if (other == null) return false;
        return switch (this) {
            case CASUAL -> other == SPORTY || other == MINIMAL;
            case SPORTY -> other == CASUAL || other == MINIMAL;
            case FORMAL -> other == CLASSIC || other == CHIC;
            case CLASSIC -> other == FORMAL || other == CHIC;
            case CHIC -> other == FORMAL || other == CLASSIC;
            case VINTAGE -> other == MINIMAL;
            case MINIMAL -> other == CASUAL || other == SPORTY || other == VINTAGE;
        };
    }
}
