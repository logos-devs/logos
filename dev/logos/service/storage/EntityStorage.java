package dev.logos.service.storage;

import dev.logos.service.storage.exceptions.EntityReadException;
import dev.logos.service.storage.exceptions.EntityWriteException;
import dev.logos.service.storage.pg.Select;

import java.sql.SQLException;
import java.util.stream.Stream;

public interface EntityStorage<Entity, StorageIdentifier> {
    Stream<Entity> query(Select.Builder selectBuilder) throws EntityReadException;

    Stream<Entity> query(Object id, Select.Builder selectBuilder) throws EntityReadException;

    StorageIdentifier create(Entity entity) throws EntityWriteException;

    StorageIdentifier update(Object id, Entity entity) throws EntityWriteException;

    StorageIdentifier delete(Object id) throws EntityWriteException;
}
