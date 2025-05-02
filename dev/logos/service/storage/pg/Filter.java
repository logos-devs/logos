package dev.logos.service.storage.pg;

import java.util.HashMap;

public class Filter {
    Column column;
    Op operation;
    Object value;

    public static Filter.Builder filter() {
        return builder();
    }

    public static Filter.Builder filter(Column column) {
        return builder().column(column);
    }

    public enum Op {
        EQ("="),
        GT(">"),
        LT("<"),
        GTE(">="),
        LTE("<="),
        NE("<>"),
        IS_NULL("is null"),
        CONTAINS("@>");

        private final String operator;

        Op(String operator) {
            this.operator = operator;
        }

        @Override
        public String toString() {
            return this.operator;
        }
    }

    public static class Builder {
        private final Filter filter;

        public Builder() {
            this.filter = new Filter();
        }

        public Builder column(Column column) {
            this.filter.column = column;
            return this;
        }

        public Builder op(Op operation) {
            this.filter.operation = operation;
            return this;
        }

        public Builder value(Object value) {
            this.filter.value = value;
            return this;
        }

        public Filter build() {
            return this.filter;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public String toQuery(HashMap<String, Object> parameters) {
        String placeholder = "";

        if (this.value != null) {
            placeholder = ":%s".formatted(this.column.identifier);
            parameters.put(this.column.identifier, value);
        }

        return String.format(
                "%s %s%s",
                this.column,
                this.operation,
                this.value != null ? " " + placeholder : ""
        );
    }
}
