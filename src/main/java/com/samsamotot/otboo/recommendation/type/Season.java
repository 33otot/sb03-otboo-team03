package com.samsamotot.otboo.recommendation.type;

import java.time.Month;

public enum Season {

    SPRING("봄"),
    SUMMER("여름"),
    FALL("가을"),
    WINTER("겨울");

    private final String name;

    Season(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Season fromMonth(Month month) {
        return switch (month) {
            case MARCH, APRIL, MAY -> SPRING;
            case JUNE, JULY, AUGUST -> SUMMER;
            case SEPTEMBER, OCTOBER  -> FALL;
            case NOVEMBER, DECEMBER, JANUARY, FEBRUARY -> WINTER;
        };
    }

    public static Season fromName(String name) {
        for (Season season : Season.values()) {
            if (season.name().equalsIgnoreCase(name) || season.getName().equalsIgnoreCase(name)) {
                return season;
            }
        }
        throw new IllegalArgumentException("Invalid season name: " + name);
    }

    /**
     * 두 계절이 완전히 다른지 여부를 판단합니다. (여름과 겨울)
     */
    public static boolean isCompletelyDifferent(Season s1, Season s2) {
        return (s1 == SUMMER && s2 == WINTER) || (s1 == WINTER && s2 == SUMMER);
    }
}
