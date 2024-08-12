package dev.logos.service.storage.pg;

public abstract class Schema extends Identifier {

    public Schema(String identifier,
                  String quotedIdentifier) {
        super(identifier, quotedIdentifier);
    }
}
