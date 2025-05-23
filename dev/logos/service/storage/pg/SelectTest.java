package dev.logos.service.storage.pg;

import dev.logos.service.storage.exceptions.EntityReadException;
import org.jdbi.v3.core.statement.Query;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class SelectTest {
    private final Relation testRelation = new Relation("schema", "table", "\"schema\".\"table\"") {
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
    };


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
        assertEquals("select from \"schema\".\"table\" as \"schema_table\"",
                Select.select().from(this.testRelation).build().toString());
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

        assertEquals("select where \"col1\" = :col1 and \"col2\" = :col2",
                Select.select().where(filter1, filter2).build().toString());
    }

    @Test
    public void test_selectWithWhereFilters_storesParameterValues() {
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

        Select query = Select.select().where(filter1, filter2).build();
        query.toString();

        HashMap<String, Object> params = query.getParameters();
        assertEquals(2, params.size());
        assertEquals("val1", params.get("col1"));
        assertEquals("val2", params.get("col2"));
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
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("param1", "value1");
        params.put("param2", 42);
        String queryStr = Select.select()
                                .from(testRelation)
                                .qualifier(new QualifierFunction("my_qualifier", new LinkedHashMap<>(
                                        Map.of("param1", new QualifierFunctionParameter("param1", "string"),
                                                "param2", new QualifierFunctionParameter("param2", "integer"))
                                )) {
                                }, params).build().toString();

        assertEquals("""
                select from "schema"."table" as "schema_table" where "schema"."my_qualifier"("schema_table", :param1::string, :param2::integer)""", queryStr);
    }

    @Test
    public void test_selectWithQualifier_storesParameterValues() {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("param1", "value1");
        params.put("param2", 42);
        Select query = Select.select()
                             .from(testRelation)
                             .qualifier(new QualifierFunction("my_qualifier", new LinkedHashMap<>(
                                     Map.of("param1", new QualifierFunctionParameter("param1", "string"),
                                             "param2", new QualifierFunctionParameter("param2", "integer"))
                             )) {
                             }, params).build();

        query.toString(); // Force parameter evaluation
        
        // Confirm parameter values were passed
        assertEquals("value1", query.getParameters().get("param1"));
        assertEquals(42, query.getParameters().get("param2"));
    }

    @Test
    public void test_selectWithMultipleConditions_producesValidSql() {
        Column col1 = new Column("col1", "\"col1\"", "string") {
        };
        Column col2 = new Column("col2", "\"col2\"", "string") {
        };

        assertEquals("select from \"schema\".\"table\" as \"schema_table\" where \"col1\" = :col1 order by \"col2\" asc limit 10 offset 5",
                Select.select()
                      .from(testRelation)
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
    
    // New derived field tests
    
    @Test
    public void test_selectWithDerivedField_producesValidSql() {
        DerivedFieldFunction derivedField = new DerivedFieldFunction("get_count");
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        
        String queryStr = Select.select()
                                .from(testRelation)
                                .derivedField(derivedField, params)
                                .build()
                                .toString();
        
        assertEquals("select schema.get_count(\"schema_table\") as \"get_count\" " +
                     "from \"schema\".\"table\" as \"schema_table\"", queryStr);
    }
    
    @Test
    public void test_selectWithDerivedFieldAndParameters_producesValidSql() {
        DerivedFieldFunction derivedField = new DerivedFieldFunction("calculate_score");
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("threshold", 75);
        params.put("factor", 1.5);
        
        // We need to force a consistent parameter naming for this test
        Select query = Select.select()
                             .from(testRelation)
                             .derivedField(derivedField, params)
                             .build();
        
        String sql = query.toString();
        
        // This test is a bit tricky because parameter names are generated with a counter
        // Let's make sure the SQL contains function call and has from clause for schema.table
        assertTrue("SQL should contain derived field function call", 
                  sql.contains("schema.calculate_score(\"schema_table\""));
        assertTrue("SQL should have from clause", 
                  sql.contains("from \"schema\".\"table\" as \"schema_table\""));
        
        // And verify the parameter values were saved
        HashMap<String, Object> storedParams = query.getParameters();
        assertEquals("Should have 2 parameters", 2, storedParams.size());
        
        // Check that our parameter values exist in the stored parameters, regardless of key names
        boolean foundThreshold = false;
        boolean foundFactor = false;
        for (Object value : storedParams.values()) {
            if (value instanceof Integer && (Integer)value == 75) {
                foundThreshold = true;
            } else if (value instanceof Double && (Double)value == 1.5) {
                foundFactor = true;
            }
        }
        assertTrue("Should store threshold parameter", foundThreshold);
        assertTrue("Should store factor parameter", foundFactor);
    }
    
    @Test
    public void test_selectWithMultipleDerivedFields_producesValidSql() {
        DerivedFieldFunction field1 = new DerivedFieldFunction("get_count");
        DerivedFieldFunction field2 = new DerivedFieldFunction("get_sum");
        LinkedHashMap<String, Object> params1 = new LinkedHashMap<>();
        LinkedHashMap<String, Object> params2 = new LinkedHashMap<>();
        params2.put("min_value", 10);
        
        // Test individual derived field function calls
        Map<String, Object> testParams = new HashMap<>();
        AtomicInteger counter = new AtomicInteger(0);
        
        String sqlExpression1 = new DerivedFieldFunctionCall(testRelation, field1, params1)
                .toSelectExpression(testParams, counter);
        assertEquals("schema.get_count(\"schema_table\") as \"get_count\"", sqlExpression1);
        
        String sqlExpression2 = new DerivedFieldFunctionCall(testRelation, field2, params2)
                .toSelectExpression(testParams, counter);
        // The parameter name will be dynamic, so we check parts of the expression
        assertTrue(sqlExpression2.startsWith("schema.get_sum(\"schema_table\", $"));
        assertTrue(sqlExpression2.endsWith(") as \"get_sum\""));
        
        // Now test the full SQL when combining both derived fields
        Select query = Select.select()
                           .from(testRelation)
                           .derivedField(field1, params1)
                           .derivedField(field2, params2)
                           .build();
        
        String sqlQuery = query.toString();
        
        // Check that SQL contains both function calls
        assertTrue("SQL should contain first derived field", 
                  sqlQuery.contains("schema.get_count(\"schema_table\")"));
        assertTrue("SQL should contain second derived field", 
                  sqlQuery.contains("schema.get_sum(\"schema_table\""));
        assertTrue("SQL should contain from clause", 
                  sqlQuery.contains("from \"schema\".\"table\" as \"schema_table\""));
        
        // Parameter should be stored
        assertEquals("Should have 1 parameter", 1, query.getParameters().size());
        assertTrue("Parameters should contain min_value", 
                 query.getParameters().containsValue(10));
    }
    
    @Test
    public void test_selectWithColumnsAndDerivedFields_producesValidSql() {
        Column col1 = new Column("col1", "\"col1\"", "string") {};
        DerivedFieldFunction derivedField = new DerivedFieldFunction("calculate_total");
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        
        String sql = Select.select(col1)
                           .from(testRelation)
                           .derivedField(derivedField, params)
                           .build()
                           .toString();
        
        assertEquals("select \"col1\", schema.calculate_total(\"schema_table\") as \"calculate_total\" " + 
                     "from \"schema\".\"table\" as \"schema_table\"", sql);
    }
    
    @Test
    public void test_selectWithDerivedField_failsWithoutFromClause() {
        DerivedFieldFunction derivedField = new DerivedFieldFunction("get_count");
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        
        try {
            Select.select()
                  .derivedField(derivedField, params)
                  .build();
            fail("Expected IllegalStateException was not thrown");
        } catch (IllegalStateException e) {
            assertEquals("Cannot add derived field function call without a FROM clause", e.getMessage());
        }
    }
}