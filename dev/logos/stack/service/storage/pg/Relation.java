package dev.logos.stack.service.storage.pg;

public abstract class Relation extends Identifier {

    public Relation(String identifier,
                    String quotedIdentifier) {
        super(identifier, quotedIdentifier);
    }
}
