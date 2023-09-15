package dev.logos.stack.service.storage.pg;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Select {

    private final String from;
    private final List<Filter> where;
    private final Integer limit;
    private final Integer offset;
    private final List<OrderBy> orderBy;

    public static class Builder {

        private String from;
        private List<Filter> where;
        private Integer limit;
        private Integer offset;
        private List<OrderBy> orderBy;

        public Builder() {
            this.offset = 0;
            this.where = new ArrayList<>();
            this.orderBy = new ArrayList<>();
        }

        public Builder from(Relation relation) {
            this.from = relation.quotedIdentifier;
            return this;
        }

        public Builder from(String schema,
                            String table) {
            this.from = Identifier.quoteIdentifier(schema) + "." + Identifier.quoteIdentifier(table);
            return this;
        }

        public Builder from(String table) {
            this.from = Identifier.quoteIdentifier(table);
            return this;
        }

        public Builder limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        public Builder offset(Integer offset) {
            this.offset = offset;
            return this;
        }

        public Builder where(List<Filter> where) {
            this.where = where;
            return this;
        }

        public Builder orderBy(OrderBy.Builder orderBy) {
            return orderBy(orderBy.build());
        }

        public Builder orderBy(OrderBy orderBy) {
            this.orderBy.add(orderBy);
            return this;
        }

        public Select build() {
            return new Select(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public Select(Builder builder) {
        this.from = builder.from;
        this.where = builder.where;
        this.limit = builder.limit;
        this.offset = builder.offset;
        this.orderBy = builder.orderBy;
    }

    @Override
    public String toString() {
        List<String> queryParts = new ArrayList<>();
        queryParts.add("select");
        queryParts.add("*");
        queryParts.add(String.format("from %s", this.from));
        if (!this.where.isEmpty()) {
            queryParts.add("where " + this.where.stream().map(Filter::toString).collect(Collectors.joining(" and ")));
        }
        if (!this.orderBy.isEmpty()) {
            queryParts.add("order by " + this.orderBy.stream().map(OrderBy::toString).collect(Collectors.joining(", ")));
        }
        if (this.limit != null) {
            queryParts.add(String.format("limit %d", this.limit));
        }
        if (this.offset != null) {
            queryParts.add(String.format("offset %d", this.offset));
        }
        return String.join(" ", queryParts);
    }
}
