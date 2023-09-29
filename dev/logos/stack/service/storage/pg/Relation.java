package dev.logos.stack.service.storage.pg;

import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class Relation extends Identifier {

    public Relation(String identifier,
                    String quotedIdentifier) {
        super(identifier, quotedIdentifier);
    }

    public abstract <Entity> Entity toProtobuf(ResultSet resultSet) throws SQLException;
}
