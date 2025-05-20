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
    
    /**
     * Convert a camel case string to snake case.
     * Example: "ByEmbeddingDistance" -> "by_embedding_distance"
     */
    public static String camelToSnakeCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toLowerCase(input.charAt(0)));
        
        for (int i = 1; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append('_');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        
        return sb.toString();
    }

    @Override
    public String toString() {
        return this.quotedIdentifier;
    }
}