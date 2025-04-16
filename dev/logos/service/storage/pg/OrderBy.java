package dev.logos.service.storage.pg;

public class OrderBy {
    public final Column column;
    public final SortOrder direction;

    public OrderBy(Column column,
                   SortOrder direction) {
        this.column = column;
        this.direction = direction;
    }

    public String toString() {
        return column.quotedIdentifier + " " + direction;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Column column;
        private SortOrder direction;

        public Builder column(Column column) {
            this.column = column;
            return this;
        }

        public Builder direction(SortOrder direction) {
            this.direction = direction;
            return this;
        }

        public OrderBy build() {
            return new OrderBy(column, direction);
        }
    }
}
