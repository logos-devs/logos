package dev.logos.service.storage.pg.exporter.mapper;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.squareup.javapoet.CodeBlock;

import java.util.List;

public abstract class PgTypeMapper {
    private final List<String> pgTypes;
    private final FieldDescriptorProto.Type protoFieldType;
    private final String resultSetFieldGetter;

    public PgTypeMapper(
            List<String> pgTypes,
            FieldDescriptorProto.Type protoFieldType,
            String resultSetFieldGetter
    ) {
        this.pgTypes = pgTypes;
        this.protoFieldType = protoFieldType;
        this.resultSetFieldGetter = resultSetFieldGetter;
    }

    public List<String> getPgTypes() {
        return pgTypes;
    }

    public FieldDescriptorProto.Type getProtoFieldType() {
        return protoFieldType;
    }

    public boolean protoFieldRepeated() {
        // Count how many pg types end with []
        long arrayCount = pgTypes.stream()
                                 .filter(type -> type.endsWith("[]"))
                                 .count();

        // If all types end with []
        if (arrayCount == pgTypes.size()) {
            return true;
        }

        // If no types end with []
        if (arrayCount == 0) {
            return false;
        }

        // If we get here, there's a mix of array and non-array types
        throw new IllegalStateException(
                String.format("Inconsistent array types detected in pgTypes %s. Some types are arrays while others are not. " +
                                      "Please override protoFieldRepeated() in the child class to handle this case.",
                              pgTypes)
        );
    }

    public String protoFieldTypeKeyword() {
        return getProtoFieldType().name().substring("TYPE_".length()).toLowerCase();
    }

    public String resultSetFieldGetter() {
        return resultSetFieldGetter;
    }

    public String resultSetFieldCast() {
        return "";
    }

    public CodeBlock pgToProto(CodeBlock innerCall) {
        return innerCall;
    }

    public abstract CodeBlock protoToPg(String queryVariable, String fieldVariable, String fieldName);
}