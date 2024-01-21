package dev.logos.service.storage;

import dev.logos.service.storage.exceptions.EntityReadException;
import dev.logos.service.storage.pg.Select;

import java.util.stream.Stream;

public interface EntityStorage<Entity> {
    Stream<Entity> query(Select.Builder selectBuilder) throws EntityReadException;
}
