package dev.logos.service.storage.pg;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Select {
    private final Column[] columns;
    private final Relation from;
    private final List<Filter> where;
    private final List<QualifierFunctionCall> qualifiers;
    private final Long limit;
    private final Long offset;
    private final List<OrderBy> orderBy;
    private final HashMap<String, Object> parameters = new HashMap<>();

    public static Builder select(Column... columns) {
        Builder builder = builder();
        builder.columns = columns;
        return builder;
    }

    public static class Builder {
        private Column[] columns = new Column[0];
        private Relation from;
        private List<Filter> where;
        private final List<QualifierFunctionCall> qualifiers;
        private Long limit;
        private Long offset;
        private final List<OrderBy> orderBy;
        private final HashMap<String, Object> parameters = new HashMap<>();
        private final AtomicInteger placeholderIndex = new AtomicInteger(0);

        public Builder() {
            this.offset = 0L;
            this.where = new ArrayList<>();
            this.qualifiers = new ArrayList<>();
            this.orderBy = new ArrayList<>();
        }

        public Builder columns(Column... columns) {
            this.columns = columns;
            return this;
        }

        public Builder from(Relation relation) {
            this.from = relation;
            return this;
        }

        public Builder limit(Long limit) {
            this.limit = limit;
            return this;
        }

        public Builder offset(Long offset) {
            this.offset = offset;
            return this;
        }

        public Builder where(Filter... where) {
            Collections.addAll(this.where, where);
            return this;
        }

        public Builder where(Column column, Filter.Op op) {
            where(Filter.builder().column(column).op(op).build());
            return this;
        }

        public Builder orderBy(OrderBy.Builder orderBy) {
            return orderBy(orderBy.build());
        }

        public Builder orderBy(OrderBy orderBy) {
            this.orderBy.add(orderBy);
            return this;
        }

        public Builder orderBy(Column column, SortOrder sortOrder) {
            this.orderBy
                    .add(OrderBy.builder()
                                .column(column)
                                .direction(sortOrder)
                                .build());
            return this;
        }

        /**
         * Adds a qualifier function call to filter results.
         *
         * @param qualifier  The qualifier function to call
         * @param parameters Named parameters to pass to the qualifier
         * @return this builder
         */
        public Builder qualifier(QualifierFunction qualifier, LinkedHashMap<String, Object> parameters) {
            if (this.from == null) {
                throw new IllegalStateException("Cannot add qualifier function call without a FROM clause");
            }
            this.qualifiers.add(new QualifierFunctionCall(this.from, qualifier, parameters));
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
        this.columns = builder.columns;
        this.from = builder.from;
        this.where = builder.where;
        this.qualifiers = builder.qualifiers;
        this.limit = builder.limit;
        this.offset = builder.offset;
        this.orderBy = builder.orderBy;
    }

    /**
     * Gets the qualifier functions added to this select.
     */
    public List<QualifierFunctionCall> getQualifiers() {
        return qualifiers;
    }

    /**
     * Get the parameters bound to this select.
     */
    public HashMap<String, Object> getParameters() {
        return parameters;
    }

    public String toString() {
        List<String> queryParts = new ArrayList<>();
        queryParts.add("select");

        if (this.columns.length > 0) {
            queryParts.add(Arrays.stream(this.columns).map(column -> column.quotedIdentifier).collect(Collectors.joining(", ")));
        }

        if (this.from != null) {
            queryParts.add(String.format("from %s", this.from.quotedIdentifier));
        }

        List<String> whereClauses = new ArrayList<>();
        this.where.stream()
                  .map(where -> where.toQuery(parameters))
                  .forEach(whereClauses::add);

        this.qualifiers.stream()
                       .map(qualifier -> qualifier.toQuery(parameters))
                       .forEach(whereClauses::add);

        if (!whereClauses.isEmpty()) {
            queryParts.add("where " + String.join(" and ", whereClauses));
        }

        if (!this.orderBy.isEmpty()) {
            queryParts.add("order by " + this.orderBy.stream()
                                                     .map(OrderBy::toString)
                                                     .collect(Collectors.joining(", ")));
        }
        if (this.limit != null) {
            queryParts.add(String.format("limit %d", this.limit));
        }
        if (this.offset != null && this.offset > 0) {
            queryParts.add(String.format("offset %d", this.offset));
        }
        return String.join(" ", queryParts);
    }
}