package dev.logos.stack.service.storage.pg;


public abstract class Column extends Identifier {
    public static final Column STAR = new Column("*", "*", null){};

    String storageType;

    public Column(String identifier,
                  String quotedIdentifier,
                  String storageType) {

        super(identifier, quotedIdentifier);
        this.storageType = storageType;
    }
}
