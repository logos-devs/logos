package dev.logos.service.storage;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessage;
import dev.logos.service.storage.exceptions.EntityReadException;
import dev.logos.service.storage.exceptions.EntityWriteException;
import dev.logos.service.storage.pg.Column;
import dev.logos.service.storage.pg.Relation;
import dev.logos.service.storage.pg.Select;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.FieldMapper;
import org.jdbi.v3.core.statement.Query;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;


public class TableStorage<Entity, StorageIdentifier> implements EntityStorage<Entity, StorageIdentifier> {

    @Inject
    private Logger logger;

    @Inject
    private DataSource dataSource;

    @Inject
    private Jdbi jdbi;

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
        Map<FieldDescriptor, Object> fields = ((GeneratedMessage) entity).getAllFields();
        List<String> fieldNames = fields.keySet().stream().map(FieldDescriptor::getName).toList();

        String queryStr =
            String.format("insert into %s (%s) values (%s) returning id",
                          relation.quotedIdentifier,
                          String.join(",", fieldNames),
                          String.join(",", fieldNames.stream().map(s -> ":" + s).toList()));

        try (Handle handle = jdbi.open()) {
            Query query = handle.createQuery(queryStr);
            bindFields(fields, query);
            return query.mapTo(this.storageIdentifierClass).first();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create entity %s".formatted(relation.quotedIdentifier), e);
            throw new EntityWriteException();
        }
    }

    public StorageIdentifier update(StorageIdentifier id, Entity entity) throws EntityWriteException {
        Map<FieldDescriptor, Object> fields = ((GeneratedMessage) entity).getAllFields();
        List<String> fieldNames = fields.keySet().stream().map(FieldDescriptor::getName).toList();

        String queryStr =
            String.format("update %s set %s where id = :id returning id",
                          relation.quotedIdentifier,
                          String.join(",", fieldNames.stream().map(s -> s + " = :" + s).toList()));

        try (Handle handle = jdbi.open()) {
            Query query = handle.createQuery(queryStr);
            bindFields(fields, query);
            query.bind("id", id);
            return query.mapTo(this.storageIdentifierClass).first();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to update entity %s".formatted(relation.quotedIdentifier), e);
            throw new EntityWriteException();
        }
    }

    public StorageIdentifier delete(StorageIdentifier id) throws EntityWriteException {
        String queryStr = String.format("delete from %s where id = :id returning id", relation.quotedIdentifier);

        try (Handle handle = jdbi.open()) {
            Query query = handle.createQuery(queryStr);
            query.bind("id", id);
            return query.mapTo(this.storageIdentifierClass).first();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to delete entity %s".formatted(relation.quotedIdentifier), e);
            throw new EntityWriteException();
        }
    }

    private void bindFields(Map<FieldDescriptor, Object> protoFields, Query query) {
        Map<String, Column> columns = this.relation.getColumns();

        protoFields.forEach((fieldDescriptor, o) -> {
            String fieldName = fieldDescriptor.getName();
            Column column = columns.get(fieldName);
            String storageType = column.getStorageType();

            switch (storageType) {
                case "smallint", "integer" -> query.bind(fieldName, (Integer) o);
                case "bigint" -> query.bind(fieldName, (BigInteger) o);
                case "real" -> query.bind(fieldName, (Float) o);
                case "double precision" -> query.bind(fieldName, (Double) o);
                case "numeric", "decimal" -> query.bind(fieldName, (BigDecimal) o);
                case "char",
                    "varchar",
                    "character varying",
                    "text" -> query.bind(fieldName, (String) o);
                case "text[]" -> query.bind(fieldName, (String[]) o);
                case "timestamp",
                    "timestamp with time zone" -> query.bind(fieldName, OffsetDateTime.parse((String) o,
                                                                                             DateTimeFormatter.ofPattern(

                                                                                                 "yyyy-MM-dd HH:mm:ss.SSSSSSX")));
                case "date" -> query.bind(fieldName, LocalDate.parse((String) o));
                case "bytea", "uuid" -> query.bind(fieldName, ((ByteString) o).toByteArray());
                case "boolean" -> query.bind(fieldName, (Boolean) o);
                default -> throw new UnsupportedOperationException(
                    "Unsupported field type %s".formatted(fieldDescriptor.getJavaType()));
            }
        });
    }
}

