package dev.logos.service.storage.pg.exporter;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import dev.logos.service.storage.pg.Converter;
import dev.logos.service.storage.pg.Identifier;

import java.util.Arrays;
import java.util.Objects;

import static dev.logos.service.storage.pg.Identifier.snakeToCamelCase;

public final class ColumnDescriptor implements ExportedIdentifier {
    private final String name;
    private final String type;

    public ColumnDescriptor(String name, String type) {
        this.name = name;
        this.type = type;
    }

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
            case "smallint", "integer" -> DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT32;
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

    public String getResultSetMethod() {
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

    public String getProtobufFieldSetter() {
        String setterName = snakeToCamelCase(this.name);
        String setterPrefix = isArray() ? "addAll" : "set";
        return "%s%s%s".formatted(
                setterPrefix,
                setterName.substring(0, 1).toUpperCase(),
                setterName.substring(1)
        );
    }

    /* from db to protobuf type */
    CodeBlock convertType(CodeBlock innerCall) {
        if (isArray()) {
            return CodeBlock.of("$T.asList((String[])$L.getArray())", Arrays.class, innerCall);
        } else {
            return switch (this.type) {
                case "bytea" -> CodeBlock.of("$T.copyFrom($L)", ByteString.class, innerCall);
                case "real" -> CodeBlock.of("%L.floatValue()", innerCall);
                case "uuid" -> CodeBlock.of("$T.uuidToBytestring($L)", Converter.class, innerCall);
                default -> innerCall;
            };
        }
    }

    @Override
    public String name() {
        return name;
    }

    public String type() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ColumnDescriptor) obj;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public String toString() {
        return "ColumnDescriptor[" +
                "name=" + name + ", " +
                "type=" + type + ']';
    }

}
