package dev.logos.service.storage.pg.exporter;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import dev.logos.service.storage.pg.Converter;

import java.util.Arrays;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT32;
import static dev.logos.service.storage.pg.Identifier.snakeToCamelCase;

public record ColumnDescriptor(String name, String type) implements ExportedIdentifier {

    public ClassName getClassName(String tableName) {

        String className = snakeToCamelCase(this.name());
        if (className.equals(tableName)) {
            className = className + "_";
        }

        return ClassName.bestGuess(className);
    }

    public String getProtobufTypeName() {
        return switch (this.type) {
            case "smallint", "integer" -> "int32";
            case "bigint" -> "int64";
            case "real" -> "float";
            case "double precision" -> "double";
            case "numeric", "decimal" -> "fixed64";
            case "char",
                 "varchar",
                 "character varying",
                 "text",
                 "text[]",
                 "timestamp",
                 "timestamp with time zone",
                 "date" -> "string";
            case "bytea", "uuid" -> "bytes";
            case "boolean" -> "bool";
            default -> throw new IllegalArgumentException("Unsupported type: " + this.type);
        };
    }

    public DescriptorProtos.FieldDescriptorProto.Type getProtobufType() {
        return switch (this.type) {
            case "smallint", "integer" -> TYPE_SINT32;
            case "bigint" -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT64;
            case "real" -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT;
            case "double precision" -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE;
            case "numeric", "decimal" -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED64;
            case "char",
                 "varchar",
                 "character varying",
                 "text",
                 "text[]",
                 "timestamp",
                 "timestamp with time zone",
                 "date" -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING;
            case "bytea", "uuid" -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES;
            case "boolean" -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL;
            default -> throw new IllegalArgumentException("Unsupported type: " + this.type);
        };
    }

    public String getJavaCast() {
        return switch (this.type) {
            case "text[]" -> "(String[])";
            case "uuid" -> "(java.util.UUID)";
            default -> "";
        };
    }

    public Boolean isArray() {
        return this.type.endsWith("[]");
    }

    public String resultSetFieldGetter() {
        if (isArray()) {
            return "getArray";
        } else {
            return switch (this.type) {
                case "smallint", "integer" -> "getInt";
                case "bigint" -> "getLong";
                case "real" -> "getFloat";
                case "double precision" -> "getDouble";
                case "numeric", "decimal" -> "getBigDecimal";
                case "char",
                     "varchar",
                     "character varying",
                     "text",
                     "text[]",
                     "timestamp",
                     "timestamp with time zone",
                     "date" -> "getString";
                case "bytea" -> "getBytes";
                case "uuid" -> "getObject";
                case "boolean" -> "getBoolean";
                default -> throw new IllegalArgumentException("Unsupported type: " + this.type);
            };
        }
    }

    public String protobufFieldSetter() {
        String setterName = snakeToCamelCase(this.name);
        String setterPrefix = isArray() ? "addAll" : "set";
        return "%s%s%s".formatted(
                setterPrefix,
                setterName.substring(0, 1).toUpperCase(),
                setterName.substring(1)
        );
    }

    /* from db to protobuf type */
    CodeBlock protobufTypeConverter(CodeBlock innerCall) {
        if (isArray()) {
            return CodeBlock.of("$T.asList((String[])$L.getArray())", Arrays.class, innerCall);
        } else {
            return switch (type) {
                case "bytea" -> CodeBlock.of("$T.copyFrom($L)", ByteString.class, innerCall);
                case "real" -> CodeBlock.of("%L.floatValue()", innerCall);
                case "uuid" -> CodeBlock.of("$T.uuidToBytestring($L)", Converter.class, innerCall);
                default -> innerCall;
            };
        }
    }

    public CodeBlock bindField(String fieldVariable, String queryVariable) {
        return switch (type) {
            case "smallint", "integer" ->
                    CodeBlock.of("$L.bind($S, (Integer) $L.get($S));\n", queryVariable, name, fieldVariable, name);
            case "bigint" ->
                    CodeBlock.of("$L.bind($S, (BigInteger) $L.get($S));\n", queryVariable, name, fieldVariable, name);
            case "real" -> CodeBlock.of("$L.bind($S, (Float) $L.get($S));\n", queryVariable, name, fieldVariable, name);
            case "double precision" ->
                    CodeBlock.of("$L.bind($S, (Double) $L.get($S));\n", queryVariable, name, fieldVariable, name);
            case "numeric", "decimal" ->
                    CodeBlock.of("$L.bind($S, (BigDecimal) $L.get($S));\n", queryVariable, name, fieldVariable, name);
            case "char", "varchar", "character varying", "text" ->
                    CodeBlock.of("$L.bind($S, (String) $L.get($S));\n", queryVariable, name, fieldVariable, name);
            case "text[]" ->
                    CodeBlock.of("$L.bind($S, (String[]) $L.get($S));\n", queryVariable, name, fieldVariable, name);
            case "timestamp", "timestamp with time zone" ->
                    CodeBlock.of("$L.bind($S, OffsetDateTime.parse((String) $L.get($S), DateTimeFormatter.ofPattern(\"yyyy-MM-dd HH:mm:ss.SSSSSSX\")));\n",
                                 queryVariable, name, fieldVariable, name);
            case "date" ->
                    CodeBlock.of("$L.bind($S, LocalDate.parse((String) $L.get($S)));\n", queryVariable, name, fieldVariable, name);
            case "bytea", "uuid" ->
                    CodeBlock.of("$L.bind($S, (($T) $L.get($S)).toByteArray());\n", queryVariable, name, ByteString.class, fieldVariable, name);
            case "boolean" ->
                    CodeBlock.of("$L.bind($S, (Boolean) $L.get($S));\n", queryVariable, name, fieldVariable, name);
            default -> throw new UnsupportedOperationException("Unsupported field type " + this.type());
        };
    }

    @Override
    public String toString() {
        return "ColumnDescriptor[" +
                "name=" + name + ", " +
                "type=" + type + ']';
    }

}
