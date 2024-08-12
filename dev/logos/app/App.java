package dev.logos.app;

public class App {
    final String name;
    final String domain;

    public App(String name, String domain) {
        super();
        this.name = name;
        this.domain = domain;
    }

    public static Builder builder () {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String domain;

        public Builder name (String name) {
            this.name = name;
            return this;
        }

        public Builder domain (String domain) {
            this.domain = domain;
            return this;
        }

        public App build () {
            return new App(name, domain);
        }
    }
}
