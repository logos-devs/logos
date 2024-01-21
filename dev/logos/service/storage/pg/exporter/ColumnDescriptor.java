package dev.logos.service.storage.pg.exporter;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.squareup.javapoet.CodeBlock;

import java.util.Arrays;

import static dev.logos.service.storage.pg.Identifier.snakeToCamelCase;

public record ColumnDescriptor(String name, String type) implements ExportedIdentifier {
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
            default -> "";
        };
    }

    public Boolean isArray() {
        return this.type.endsWith("[]");
    }

    public String getResultSetMethod() {
        DescriptorProtos.FieldDescriptorProto.Type protobufType = getProtobufType();
        if (isArray()) {
            return "getArray";
        } else {
            return switch (protobufType) {
                case TYPE_BOOL -> "getBoolean";
                case TYPE_BYTES -> "getBytes";
                case TYPE_DOUBLE,
                        TYPE_FIXED64 -> "getDouble";
                case TYPE_FLOAT -> "getFloat";
                case TYPE_SINT32 -> "getInt";
                case TYPE_SINT64 -> "getLong";
                case TYPE_STRING -> "getString";
                default -> throw new RuntimeException("Unknown type: " + protobufType);
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

    CodeBlock convertType(CodeBlock innerCall) {
        DescriptorProtos.FieldDescriptorProto.Type protobufType = getProtobufType();
        if (isArray()) {
            return CodeBlock.of("$T.asList((String[])$L.getArray())", Arrays.class, innerCall);
        } else {
            return switch (protobufType) {
                case TYPE_BOOL, TYPE_DOUBLE, TYPE_FIXED64, TYPE_SINT32, TYPE_SINT64, TYPE_STRING -> innerCall;
                case TYPE_BYTES -> CodeBlock.of("$T.copyFrom($L)", ByteString.class, innerCall);
                case TYPE_FLOAT -> CodeBlock.of("%L.floatValue()", innerCall);
                default -> throw new RuntimeException("Unknown type: " + protobufType);
            };
        }
    }
}
