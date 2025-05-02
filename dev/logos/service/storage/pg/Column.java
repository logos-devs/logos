package dev.logos.service.storage.pg;


import static dev.logos.service.storage.pg.Filter.filter;

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

    /*
    EQ("="),
    GT(">"),
    LT("<"),
    GTE(">="),
    LTE("<="),
    NE("<>"),
    IS_NULL("is null"),
    CONTAINS("@>");
     */

    public Filter eq(Object value) {
        return filter(this).op(Filter.Op.EQ).value(value).build();
    }
}
