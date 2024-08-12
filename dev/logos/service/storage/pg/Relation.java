package dev.logos.service.storage.pg;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public abstract class Relation extends Identifier {

    public Relation(String identifier,
                    String quotedIdentifier) {
        super(identifier, quotedIdentifier);
    }

    public abstract Map<String, Column> getColumns();

    public abstract <Entity> Entity toProtobuf(ResultSet resultSet) throws SQLException;
}
