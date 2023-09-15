package dev.logos.stack.service.storage;

import dev.logos.stack.service.storage.exceptions.EntityReadException;
import dev.logos.stack.service.storage.pg.Select;

import java.util.stream.Stream;

public interface EntityStorage<Entity, StorageIdentifier, Filter> {
    Stream<Entity> query(Select.Builder selectBuilder) throws EntityReadException;
}