package com.samsamotot.otboo.recommendation.type;

public enum Waterproof {
    TRUE("가능"),
    FALSE("불가능");

    private final String name;

    Waterproof(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Waterproof fromName(String name) {
        for (Waterproof waterproof : Waterproof.values()) {
            if (waterproof.name().equalsIgnoreCase(name) || waterproof.getName().equalsIgnoreCase(name)) {
                return waterproof;
            }
        }
        throw new IllegalArgumentException("Invalid waterproof name: " + name);
    }
}
