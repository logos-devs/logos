package dev.logos.service.storage.pg;


public abstract class Column extends Identifier {
    public static final Column STAR = new Column("*", "*", null) {
    };

    private final String storageType;

    public Column(String identifier,
                  String quotedIdentifier,
                  String storageType) {

        super(identifier, quotedIdentifier);
        this.storageType = storageType;
    }

    public String getStorageType() {
        return storageType;
    }
}
