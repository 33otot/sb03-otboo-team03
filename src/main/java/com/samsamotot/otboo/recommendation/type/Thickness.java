package com.samsamotot.otboo.recommendation.type;

public enum Thickness {
    LIGHT("얇음"),
    MEDIUM("보통"),
    HEAVY("두꺼움");

    private final String name;

    Thickness(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Thickness fromName(String name) {
        for (Thickness thickness : Thickness.values()) {
            if (thickness.name().equalsIgnoreCase(name) || thickness.getName().equalsIgnoreCase(name)) {
                return thickness;
            }
        }
        throw new IllegalArgumentException("Invalid thickness name: " + name);
    }
}
