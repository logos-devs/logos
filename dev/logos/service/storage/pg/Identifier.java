package dev.logos.service.storage.pg;

public abstract class Identifier {

    static String delimiter = "\"";
    public String identifier;
    public String quotedIdentifier;

    public Identifier(String identifier,
                      String quotedIdentifier) {
        this.identifier = identifier;
        this.quotedIdentifier = quotedIdentifier;
    }

    public static String quoteIdentifier(String identifier) {
        return delimiter + identifier.replace(delimiter, delimiter + delimiter) + delimiter;
    }

    public static String snakeToCamelCase(String input) {
        StringBuilder sb = new StringBuilder();
        for (String word : input.split("_")) {
            if (!word.isEmpty()) {
                sb.append(word.substring(0, 1).toUpperCase());
                sb.append(word.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return this.quotedIdentifier;
    }
}
