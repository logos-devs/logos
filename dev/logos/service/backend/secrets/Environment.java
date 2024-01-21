package dev.logos.stack.backend.secrets;

public enum Environment {
    Dev("dev"),
    Test("test"),
    Prod("prod");

    public final String name;

    Environment(String name) {
        this.name = name;
    }
}