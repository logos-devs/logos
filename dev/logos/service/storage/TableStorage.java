package dev.logos.service.storage;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessage;
import dev.logos.service.storage.exceptions.EntityReadException;
import dev.logos.service.storage.exceptions.EntityWriteException;
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
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.System.err;


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
        Map<String, Object> fields = ((GeneratedMessage) entity).getAllFields()
                                                                .entrySet()
                                                                .stream()
                                                                .collect(Collectors.toMap(
                                                                        e -> e.getKey().getName(),
                                                                        Map.Entry::getValue));
        Set<String> fieldNames = fields.keySet();

        String queryStr =
                String.format("insert into %s (%s) values (%s) returning id",
                              relation.quotedIdentifier,
                              String.join(",", fieldNames),
                              String.join(",", fieldNames.stream().map(s -> ":" + s).toList()));

        try (Handle handle = jdbi.open()) {
            Query query = handle.createQuery(queryStr);
            relation.bindFields(fields, query);
            return query.mapTo(this.storageIdentifierClass).first();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create entity %s".formatted(relation.quotedIdentifier), e);
            throw new EntityWriteException();
        }
    }

    public StorageIdentifier update(StorageIdentifier id, Entity entity) throws EntityWriteException {
        Map<String, Object> fields = ((GeneratedMessage) entity).getAllFields()
                                                                .entrySet()
                                                                .stream()
                                                                .collect(Collectors.toMap(
                                                                        e -> e.getKey().getName(),
                                                                        Map.Entry::getValue));
        Set<String> fieldNames = fields.keySet();

        String queryStr =
                String.format("update %s set %s where id = :id returning id",
                              relation.quotedIdentifier,
                              String.join(",", fieldNames.stream().map(s -> s + " = :" + s).toList()));

        try (Handle handle = jdbi.open()) {
            Query query = handle.createQuery(queryStr);
            relation.bindFields(fields, query);
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
}