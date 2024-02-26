package dev.logos.service.backend.secrets;

public enum App {
    Digits("digits"),
    Review("review");

    public final String name;

    App(String name) {
        this.name = name;
    }
}
