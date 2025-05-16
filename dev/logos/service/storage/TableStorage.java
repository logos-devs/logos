package dev.logos.service.storage;

import com.google.inject.Inject;
import com.google.protobuf.GeneratedMessage;
import dev.logos.service.storage.exceptions.EntityReadException;
import dev.logos.service.storage.exceptions.EntityWriteException;
import dev.logos.service.storage.pg.Relation;
import dev.logos.service.storage.pg.Select;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.mapper.reflect.FieldMapper;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.StatementContext;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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

    private Entity entityMapper(ResultSet rs, StatementContext ctx) {
        try {
            return relation.toProtobuf(rs);
        } catch (EntityReadException e) {
            throw new MappingException(e);
        }
    }

    public Stream<Entity> query(Object id, Select.Builder selectBuilder) throws EntityReadException {
        selectBuilder.where(relation.getColumns().get("id").eq(id));
        return query(selectBuilder);
    }

    public Stream<Entity> query(Select.Builder selectBuilder) throws EntityReadException {
        Select selectObj = selectBuilder.build();
        String sqlQuery = selectObj.toString();
        try {
            return query(sqlQuery, selectObj.getParameters());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to execute query %s".formatted(sqlQuery), e);
            throw new EntityReadException(e);
        }
    }

    public Stream<Entity> query(String sqlQuery) throws SQLException {
        return query(sqlQuery, (Map<String, Object>) null);
    }

    public Stream<Entity> query(String sqlQuery, Map<String, Object> parameters) throws SQLException {
        Handle handle = jdbi.open();
        handle.registerRowMapper(FieldMapper.factory(entityClass));
        Query queryObj = handle.createQuery(sqlQuery);

        if (parameters != null) {
            relation.bindFields(parameters, queryObj);
        }

        return queryObj
                .map(this::entityMapper)
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
            throw new EntityWriteException(e);
        }
    }

    public StorageIdentifier update(Object id, Entity entity) throws EntityWriteException {
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
            fields.put("id", id);
            relation.bindFields(fields, query);
            return query.mapTo(this.storageIdentifierClass).first();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to update entity %s".formatted(relation.quotedIdentifier), e);
            throw new EntityWriteException(e);
        }
    }

    public StorageIdentifier delete(Object id) throws EntityWriteException {
        String queryStr = String.format("delete from %s where id = :id returning id", relation.quotedIdentifier);

        try (Handle handle = jdbi.open()) {
            Query query = handle.createQuery(queryStr);
            relation.bindFields(Map.of("id", id), query);
            return query.mapTo(this.storageIdentifierClass).first();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to delete entity %s".formatted(relation.quotedIdentifier), e);
            throw new EntityWriteException(e);
        }
    }
}
