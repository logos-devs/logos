package dev.logos.stack.service.storage;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessageV3;
import com.querydsl.core.types.dsl.BooleanTemplate;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.PostgreSQLTemplates;
import com.querydsl.sql.SQLQueryFactory;
import dev.logos.stack.service.storage.exceptions.EntityReadException;
import dev.logos.stack.service.storage.exceptions.EntityWriteException;
import dev.logos.stack.service.storage.pg.Filter;
import dev.logos.stack.service.storage.pg.Relation;
import dev.logos.stack.service.storage.pg.Select;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.mapper.reflect.FieldMapper;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.postgres.PostgresPlugin;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


public abstract class TableStorage<Entity, StorageIdentifier> implements
                                                              EntityStorage<Entity, StorageIdentifier, BooleanTemplate> {

    @Inject
    protected DataSource dataSource;

    Relation relation;
    Class<Entity> entityClass;
    Class<StorageIdentifier> storageIdentifierClass;

    public TableStorage(
        Relation relation,
        Class<Entity> entityClass,
        Class<StorageIdentifier> storageIdentifierClass
    ) {
        this.relation = relation;
        this.entityClass = entityClass;
        this.storageIdentifierClass = storageIdentifierClass;
    }

    protected abstract Entity storageToEntity(ResultSet resultSet) throws SQLException;

    protected Jdbi getJdbi() throws SQLException {
        return Jdbi.create(dataSource).installPlugin(new PostgresPlugin());
    }

    protected SQLQueryFactory getQueryFactory() {
        return new SQLQueryFactory(
            new Configuration(
                PostgreSQLTemplates.builder()
                                   .printSchema()
                                   .build()),
            dataSource);
    }

    // TODO : write a mapper which uses the proto reflection API. The members
    // of this class are not the ones which correspond to field names.
    protected RowMapperFactory getMapper() {
        return FieldMapper.factory(entityClass);
    }

    private List<String> getFieldNames(Map<FieldDescriptor, Object> fields) {
        return fields.keySet().stream().map(FieldDescriptor::getName).toList();
    }

    private void bindEntityUpdateFields(Map<FieldDescriptor, Object> fields,
                                        Query updateStmt) {
        fields.forEach((fieldDescriptor, o) -> {
            String fieldName = fieldDescriptor.getName();
            switch (fieldDescriptor.getJavaType()) {
                case INT -> {
                    updateStmt.bind(fieldName, (Integer) o);
                }
                case LONG -> {
                    updateStmt.bind(fieldName, (Long) o);
                }
                case FLOAT -> {
                    updateStmt.bind(fieldName, (Float) o);
                }
                case DOUBLE -> {
                    updateStmt.bind(fieldName, (Double) o);
                }
                case BOOLEAN -> {
                    updateStmt.bind(fieldName, (Boolean) o);
                }
                case STRING -> {
                    updateStmt.bind(fieldName, (String) o);
                }
                case BYTE_STRING -> {
                    updateStmt.bind(fieldName, ((ByteString) o).toByteArray());
                }
                case ENUM -> {
                    throw new UnsupportedOperationException("ENUM is not supported yet.");
                }
                case MESSAGE -> {
                    throw new UnsupportedOperationException(
                        "MESSAGE (sub-message field) is not supported yet.");
                }
            }
        });
    }

    // FIXME does not respect the argument
    @Override
    public Entity get(StorageIdentifier id) throws EntityReadException {
        try (Query selectQuery = getJdbi().withHandle(handle -> {
            handle.registerRowMapper(getMapper());
            return handle.select(
                Select.builder()
                      .from(relation)
                      .where(Collections.singletonList(
                          Filter.build().column("id").op(Filter.Op.EQ)))
                      .build()
                      .toString(),
                id);
        })) {
            return selectQuery.mapTo(entityClass).first();
        } catch (SQLException e) {
            throw new EntityReadException();
        }
    }

    @Override
    public Stream<Entity> list() throws EntityReadException {
        return list(null);
    }

    @Override
    public Stream<Entity> list(BooleanTemplate filter) throws EntityReadException {
        try {
            Handle handle = getJdbi().open();
            handle.registerRowMapper(getMapper());
            return handle.createQuery(
                             Select.builder()
                                   .from(relation)
                                   .build()
                                   .toString())
                         .map((rs, ctx) -> storageToEntity(rs))
                         .stream()
                         .onClose(handle::close);
        } catch (SQLException e) {
            throw new EntityReadException("Database error in list query", e);
        }
    }

    @Override
    public StorageIdentifier create(Entity entity) throws EntityWriteException {
        try (Query insertQuery = getJdbi().withHandle(handle -> {
            Map<FieldDescriptor, Object> fields = ((GeneratedMessageV3) entity).getAllFields();

            List<String> fieldNames = getFieldNames(fields);
            String placeholders = String.join(
                ",",
                fieldNames.stream().map(s -> ":" + s).toList());

            Query stmt = handle.createQuery(
                String.format("insert into %s (%s) values (%s) returning id",
                              relation.quotedIdentifier,
                              String.join(",", fieldNames),
                              placeholders));

            bindEntityUpdateFields(fields, stmt);
            return stmt;
        })) {
            return insertQuery.mapTo(this.storageIdentifierClass).first();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

