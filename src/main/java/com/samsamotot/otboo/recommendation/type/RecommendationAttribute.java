package com.samsamotot.otboo.recommendation.type;

public enum RecommendationAttribute {

    THICKNESS("두께"),
    WATERPROOF("방수"),
    SEASON("계절"),
    STYLE("스타일");

    private final String name;

    RecommendationAttribute(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static RecommendationAttribute fromName(String name) {
        for (RecommendationAttribute attr : values()) {
            if (attr.getName().equals(name)) {
                return attr;
            }
        }
        throw new IllegalArgumentException("Invalid recommendationAttribute name: " + name);
    }
}
