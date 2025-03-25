package dev.logos.service.storage.pg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Select {

    private final Column[] columns;
    private final String from;
    private final List<Filter> where;
    private final List<QualifierFunctionCall> qualifiers;
    private final Long limit;
    private final Long offset;
    private final List<OrderBy> orderBy;

    public static Builder select(Column... columns) {
        Builder builder = builder();
        builder.columns = columns;
        return builder;
    }

    public static class Builder {

        private Column[] columns = new Column[0];
        private String from;
        private List<Filter> where;
        private List<QualifierFunctionCall> qualifiers;
        private Long limit;
        private Long offset;
        private List<OrderBy> orderBy;

        public Builder() {
            this.offset = 0L;
            this.where = new ArrayList<>();
            this.qualifiers = new ArrayList<>();
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

        public Builder limit(Long limit) {
            this.limit = limit;
            return this;
        }

        public Builder offset(Long offset) {
            this.offset = offset;
            return this;
        }

        public Builder where(Filter... where) {
            this.where = List.of(where);
            return this;
        }

        public Builder where(Column column, Filter.Op op) {
            this.where = List.of(
                    Filter.builder()
                          .column(column)
                          .op(op)
                          .build());
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
         * @param qualifierName Name of the qualifier function
         * @param parameters Named parameters to pass to the qualifier
         * @return this builder
         */
        public Builder qualifier(String qualifierName, Map<String, Object> parameters) {
            this.qualifiers.add(new QualifierFunctionCall(qualifierName, parameters));
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
     * Gets all parameter values from qualifier functions that need to be bound.
     * @return Map of parameter names to values
     */
    public Map<String, Object> getQualifierParameters() {
        return QualifierSupport.getParameters(qualifiers);
    }

    /**
     * Gets the qualifier functions added to this select.
     */
    public List<QualifierFunctionCall> getQualifiers() {
        return qualifiers;
    }

    @Override
    public String toString() {
        List<String> queryParts = new ArrayList<>();
        queryParts.add("select");

        if (this.columns.length > 0) {
            queryParts.add(Arrays.stream(this.columns).map(column -> column.quotedIdentifier).collect(Collectors.joining(", ")));
        }

        queryParts.add(String.format("from %s as t", this.from));
        
        // Combine standard WHERE clauses and qualifier function calls
        List<String> whereClauses = new ArrayList<>();
        if (!this.where.isEmpty()) {
            whereClauses.addAll(this.where.stream()
                .map(Filter::toString)
                .collect(Collectors.toList()));
        }
        
        if (!this.qualifiers.isEmpty()) {
            whereClauses.addAll(this.qualifiers.stream()
                .map(QualifierFunctionCall::toString)
                .collect(Collectors.toList()));
        }
        
        if (!whereClauses.isEmpty()) {
            queryParts.add("where " + String.join(" and ", whereClauses));
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