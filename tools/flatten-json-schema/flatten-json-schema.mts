import * as fs from 'fs';

// Load schema and dereference library (if needed, otherwise, manual dereference as shown)
if (process.argv.length !== 4) {
    console.error('Usage: flatten-json-schema.ts <input-schema-file.json> <output-schema-file.json>');
    process.exit(1);
}

const inputFile = process.argv[2];
const outputFile = process.argv[3];

// Helper function to recursively embed references
function embedRefs(schema: any, defs: any): any {
    if (schema && typeof schema === 'object') {
        if (schema.$ref) {
            // Resolve $ref from definitions
            const refKey = schema.$ref.replace(/^#\/\$defs\//, '');
            if (defs[refKey]) {
                // Recursively embed the definition into the field
                return embedRefs(defs[refKey], defs);
            }
            // If reference not found, return the schema as-is (or throw an error)
            console.warn(`Warning: Reference ${schema.$ref} not found in definitions.`);
            return schema;
        } else if (schema.properties) {
            // Recursively apply embedding to each property
            for (const key in schema.properties) {
                schema.properties[key] = embedRefs(schema.properties[key], defs);
            }
        }
    }
    return schema;
}

// Read the schema file
fs.readFile(inputFile, 'utf8', async (err, data) => {
    if (err) {
        console.error(`Error reading file: ${err.message}`);
        process.exit(1);
    }

    let schema: any;
    try {
        schema = JSON.parse(data.replace(/\./g, "_"));
    } catch (parseErr) {
        console.error(`Error parsing JSON: ${parseErr.message}`);
        process.exit(1);
    }

    try {
        // Ensure definitions are provided in the schema
        const defs = schema.$defs || {};
        const flattenedSchema = embedRefs(schema, defs);

        // Write the fully dereferenced and flattened schema to the output file
        fs.writeFile(outputFile, JSON.stringify(flattenedSchema, null, 2), (writeErr) => {
            if (writeErr) {
                console.error(`Error writing output file: ${writeErr.message}`);
                process.exit(1);
            }
            console.log(`Successfully flattened ${inputFile} to ${outputFile}`);
        });
    } catch (dereferenceErr) {
        console.error(`Error dereferencing schema: ${dereferenceErr.message}`);
        process.exit(1);
    }
});
