package dev.logos.service.storage.pg;

/**
 * Represents a derived field function in PostgreSQL.
 * A derived field function will be added to the SELECT clause to compute a value for each row.
 */
public class DerivedFieldFunction {
    /**
     * The name of this derived field function
     */
    public final String name;
    
    /**
     * Creates a new derived field function.
     * 
     * @param name the name of the derived field function
     */
    public DerivedFieldFunction(String name) {
        this.name = name;
    }
}