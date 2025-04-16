package dev.logos.service.storage.pg;

import dev.logos.service.storage.exceptions.EntityReadException;
import org.jdbi.v3.core.statement.Query;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.ResultSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class SelectTest {
    @Test
    public void test_selectWithNoArguments_producesValidSql() {
        assertEquals("select", Select.select().build().toString());
    }

    @Test
    public void test_selectWithColumns_producesValidSql() {
        assertEquals("select \"col1\", \"col2\", \"col3\"",
                Select.select(
                        new Column("col1", "\"col1\"", "string") {
                        },
                        new Column("col2", "\"col2\"", "string") {
                        },
                        new Column("col3", "\"col3\"", "string") {
                        }
                ).build().toString());
    }

    @Test
    public void test_selectWithStar_producesValidSql() {
        assertEquals("select *", Select.select(Column.STAR).build().toString());
    }

    @Test
    public void test_selectFromRelation_producesValidSql() {
        assertEquals("select from \"schema\".\"table\"",
                Select.select().from(new Relation("schema.table", "\"schema\".\"table\"") {
                    @Override
                    public Map<String, Column> getColumns() {
                        return Map.of();
                    }

                    @Override
                    public <Entity> Entity toProtobuf(ResultSet resultSet) throws EntityReadException {
                        return null;
                    }

                    @Override
                    public void bindFields(Map<String, Object> fields, Query query) {
                    }
                }).build().toString());
    }

    @Test
    public void test_selectFromSchemaAndTable_producesValidSql() {
        assertEquals("select from \"schema\".\"table\"",
                Select.select().from("schema", "table").build().toString());
    }

    @Test
    public void test_selectFromTable_producesValidSql() {
        assertEquals("select from \"table\"",
                Select.select().from("table").build().toString());
    }

    @Test
    public void test_selectWithWhereFilters_producesValidSql() {
        Filter filter1 = Filter.builder()
                               .column(new Column("col1", "\"col1\"", "string") {
                               })
                               .op(Filter.Op.EQ)
                               .value("val1")
                               .build();
        Filter filter2 = Filter.builder()
                               .column(new Column("col2", "\"col2\"", "string") {
                               })
                               .op(Filter.Op.EQ)
                               .value("val2")
                               .build();

        assertEquals("select where \"col1\" = 'val1' and \"col2\" = 'val2'",
                Select.select().where(filter1, filter2).build().toString());
    }

    @Test
    public void test_selectWhereColumnIsNull_producesValidSql() {
        Column col = new Column("col1", "\"col1\"", "string") {
        };
        assertEquals("select where \"col1\" is null",
                Select.select().where(col, Filter.Op.IS_NULL).build().toString());
    }

    @Test
    public void test_selectWithLimit_producesValidSql() {
        assertEquals("select limit 10",
                Select.select().limit(10L).build().toString());
    }

    @Test
    public void test_selectWithOffset_producesValidSql() {
        assertEquals("select offset 5",
                Select.select().offset(5L).build().toString());
    }

    @Test
    public void test_selectWithOrderByBuilder_producesValidSql() {
        Column col = new Column("col1", "\"col1\"", "string") {
        };
        assertEquals("select order by \"col1\" asc",
                Select.select().orderBy(OrderBy.builder().column(col).direction(SortOrder.ASC)).build().toString());
    }

    @Test
    public void test_selectWithOrderByObject_producesValidSql() {
        Column col = new Column("col1", "\"col1\"", "string") {
        };
        OrderBy orderBy = OrderBy.builder().column(col).direction(SortOrder.DESC).build();
        assertEquals("select order by \"col1\" desc",
                Select.select().orderBy(orderBy).build().toString());
    }

    @Test
    public void test_selectWithOrderByColumnSortOrder_producesValidSql() {
        Column col = new Column("col1", "\"col1\"", "string") {
        };
        assertEquals("select order by \"col1\" desc",
                Select.select().orderBy(col, SortOrder.DESC).build().toString());
    }

    @Test
    public void test_selectWithQualifier_producesValidSql() {
        String queryStr = Select.select()
                                .qualifier("my_qualifier", Map.of(
                                        "param1", "value1",
                                        "param2", 42
                                )).build().toString();

        assertEquals("select where my_qualifier(t, :param1, :param2)", queryStr);
    }

    @Test
    public void test_selectWithMultipleConditions_producesValidSql() {
        Column col1 = new Column("col1", "\"col1\"", "string") {
        };
        Column col2 = new Column("col2", "\"col2\"", "string") {
        };

        assertEquals("select from \"table\" where \"col1\" = 'val' order by \"col2\" asc limit 10 offset 5",
                Select.select()
                      .from("table")
                      .where(col1.eq("val"))
                      .orderBy(col2, SortOrder.ASC)
                      .limit(10L)
                      .offset(5L)
                      .build().toString());
    }

    @Test
    public void test_selectWithMultipleOrderBy_producesValidSql() {
        Column col1 = new Column("col1", "\"col1\"", "string") {
        };
        Column col2 = new Column("col2", "\"col2\"", "string") {
        };

        assertEquals("select order by \"col1\" asc, \"col2\" desc",
                Select.select()
                      .orderBy(col1, SortOrder.ASC)
                      .orderBy(col2, SortOrder.DESC)
                      .build().toString());
    }
}

