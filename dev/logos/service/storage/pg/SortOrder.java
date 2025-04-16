package dev.logos.service.storage.pg;

public enum SortOrder {
    ASC,
    DESC;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
