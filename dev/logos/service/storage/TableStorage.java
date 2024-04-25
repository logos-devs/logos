package dev.logos.service.storage;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessageV3;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.PostgreSQLTemplates;
import com.querydsl.sql.SQLQueryFactory;
import dev.logos.service.storage.exceptions.EntityReadException;
import dev.logos.service.storage.exceptions.EntityWriteException;
import dev.logos.service.storage.pg.Relation;
import dev.logos.service.storage.pg.Select;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.FieldMapper;
import org.jdbi.v3.core.statement.Query;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


public class TableStorage<Entity, StorageIdentifier> implements EntityStorage<Entity, StorageIdentifier> {

    @Inject private DataSource dataSource;
    @Inject private Jdbi jdbi;

    protected Relation relation;
    Class<Entity> entityClass;
    Class<StorageIdentifier> storageIdentifierClass;

    @Inject
    public TableStorage(
            Relation relation,
            Class<Entity> entityClass1,
            Class<StorageIdentifier> storageIdentifierClass
    ) {
        this.relation = relation;
        this.entityClass = entityClass1;
        this.storageIdentifierClass = storageIdentifierClass;
    }

    @Deprecated
    protected SQLQueryFactory getQueryFactory() {
        return new SQLQueryFactory(
                new Configuration(
                        PostgreSQLTemplates.builder()
                                           .printSchema()
                                           .build()),
                dataSource);
    }

    // TODO : write a mapper which uses the proto reflection API. The members
    //  of this class are not the ones which correspond to field names.
    public Stream<Entity> query(Select.Builder selectBuilder) throws EntityReadException {
        Handle handle = jdbi.open();
        handle.registerRowMapper(FieldMapper.factory(entityClass));
        return handle.createQuery(selectBuilder.build().toString())
                     .map((rs, ctx) -> relation.<Entity>toProtobuf(rs))
                     .stream()
                     .onClose(handle::close);
    }

    public StorageIdentifier create(Entity entity) throws EntityWriteException {
        try (Query insertQuery = jdbi.withHandle(handle -> {
            Map<FieldDescriptor, Object> fields = ((GeneratedMessageV3) entity).getAllFields();
            List<String> fieldNames = fields.keySet().stream().map(FieldDescriptor::getName).toList();

            Query query = handle.createQuery(
                    String.format("insert into %s (%s) values (%s) returning id",
                                  relation.quotedIdentifier,
                                  String.join(",", fieldNames),
                                  String.join(",", fieldNames.stream().map(s -> ":" + s).toList())));

            bindFields(fields, query);

            return query;
        })) {
            return insertQuery.mapTo(this.storageIdentifierClass).first();
        }
    }

    public StorageIdentifier update(StorageIdentifier id, Entity entity) throws EntityWriteException {
        try (Query updateQuery = jdbi.withHandle(handle -> {
            Map<FieldDescriptor, Object> fields = ((GeneratedMessageV3) entity).getAllFields();
            List<String> fieldNames = fields.keySet().stream().map(FieldDescriptor::getName).toList();

            Query query = handle.createQuery(
                    String.format("update %s set %s where id = :id returning id",
                                  relation.quotedIdentifier,
                                  String.join(",", fieldNames.stream().map(s -> s + " = :" + s).toList())));

            bindFields(fields, query);
            query.bind("id", id);

            return query;
        })) {
            return updateQuery.mapTo(this.storageIdentifierClass).first();
        }
    }

    public StorageIdentifier delete(StorageIdentifier id) {
        try (Query deleteQuery = jdbi.withHandle(
                handle -> handle.createQuery(
                                        String.format("delete from %s where id = :id returning id", relation.quotedIdentifier))
                                .bind("id", id))) {
            return deleteQuery.mapTo(this.storageIdentifierClass).first();
        }
    }

    private static void bindFields(Map<FieldDescriptor, Object> fields, Query query) {
        fields.forEach((fieldDescriptor, o) -> {
            String fieldName = fieldDescriptor.getName();
            switch (fieldDescriptor.getJavaType()) {
                case INT -> query.bind(fieldName, (Integer) o);
                case LONG -> query.bind(fieldName, (Long) o);
                case FLOAT -> query.bind(fieldName, (Float) o);
                case DOUBLE -> query.bind(fieldName, (Double) o);
                case BOOLEAN -> query.bind(fieldName, (Boolean) o);
                case STRING -> query.bind(fieldName, (String) o);
                case BYTE_STRING -> query.bind(fieldName, ((ByteString) o).toByteArray());
                case ENUM -> throw new UnsupportedOperationException("ENUM is not supported yet.");
                case MESSAGE -> throw new UnsupportedOperationException(
                        "MESSAGE (sub-message field) is not supported yet.");
            }
        });
    }
}

