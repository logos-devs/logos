package dev.logos.service.storage;

import dev.logos.service.storage.exceptions.EntityReadException;
import dev.logos.service.storage.exceptions.EntityWriteException;
import dev.logos.service.storage.pg.Select;

import java.util.stream.Stream;

public interface EntityStorage<Entity, StorageIdentifier> {
    Stream<Entity> query(Select.Builder selectBuilder);

    Stream<Entity> query(StorageIdentifier id, Select.Builder selectBuilder);

    StorageIdentifier create(Entity entity) throws EntityWriteException;

    StorageIdentifier update(StorageIdentifier id, Entity entity) throws EntityWriteException;

    StorageIdentifier delete(StorageIdentifier id) throws EntityWriteException;
}
