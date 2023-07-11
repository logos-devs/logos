package dev.logos.stack.service.storage;

import dev.logos.stack.service.storage.exceptions.EntityReadException;
import dev.logos.stack.service.storage.exceptions.EntityWriteException;
import java.util.stream.Stream;

public interface EntityStorage<Entity, StorageIdentifier, Filter> {

    Entity get(StorageIdentifier id) throws EntityReadException;

    Stream<Entity> list() throws EntityReadException;

    Stream<Entity> list(Filter filter) throws EntityReadException;

    StorageIdentifier create(Entity entity) throws EntityWriteException;
}