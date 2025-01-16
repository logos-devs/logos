package dev.logos.service.storage.pg;

import com.google.protobuf.Descriptors.FieldDescriptor;
import dev.logos.service.storage.exceptions.EntityReadException;
import org.jdbi.v3.core.statement.Query;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public abstract class Relation extends Identifier {

    public Relation(String identifier,
                    String quotedIdentifier) {
        super(identifier, quotedIdentifier);
    }

    public abstract Map<String, Column> getColumns();

    public abstract <Entity> Entity toProtobuf(ResultSet resultSet) throws EntityReadException;

    // Adjust method signature to accept Map<FieldDescriptor, Object>
    public abstract void bindFields(Map<String, Object> fields, Query query);
}
