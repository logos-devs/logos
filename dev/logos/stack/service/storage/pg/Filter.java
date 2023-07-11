package dev.logos.stack.service.storage.pg;

public class Filter {

    public enum Op {
        EQ("="),
        GT(">"),
        LT("<"),
        GTE(">="),
        LTE("<="),
        NE("<>");

        private final String operator;

        Op(String operator) {
            this.operator = operator;
        }

        public String getOperator() {
            return operator;
        }

        @Override
        public String toString() {
            return this.operator;
        }
    }

    public static Filter build() {
        return new Filter();
    }

    String column;
    Op operation;

    public Filter column(String column) {
        this.column = column;
        return this;
    }

    public Filter op(Op operation) {
        this.operation = operation;
        return this;
    }

    public String toString() {
        return String.format("%s %s", this.column, this.operation);
    }
}
