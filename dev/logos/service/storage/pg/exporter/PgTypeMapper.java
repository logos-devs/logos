package dev.logos.service.storage.pg.exporter;

import com.google.protobuf.DescriptorProtos;
import com.squareup.javapoet.CodeBlock;

import java.util.List;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT32;

public abstract class PgTypeMapper {
    public PgTypeMapper(
            List<String> pgTypes,
            DescriptorProtos.FieldDescriptorProto.Type fieldType,
            String resultSetMethod
    ) {
    }

    public CodeBlock pgToProto(CodeBlock innerCall) {
        return innerCall;
    }

    public abstract CodeBlock protoToPg(String fieldVariable, String queryVariable);

    public static PgTypeMapper PG_TYPE_MAPPER =
            new PgTypeMapper(List.of("smallint", "integer"), TYPE_SINT32, "getInt") {
                public CodeBlock protoToPg(String fieldVariable, String queryVariable) {
                    return CodeBlock.of("$L.bind($S, (Integer) $L.get($S));\n",
                                        queryVariable,
                                        fieldName,
                                        fieldVariable,
                                        fieldName);
                }
            };
}


//public static PgTypeMapper smallint =
//        PgTypeMapper.builder()
//                    .pgTypes("smallint", "integer")
//                    .protoType(TYPE_SINT32)
//                    .resultSetMethod("getInt")
//                    //.protobufTypeName("int32")
//                    .javaCast("") // default
//                    .pgToProto((CodeBlock innerCall) -> innerCall) // default
//                    .protoToPg((String fieldVariable, String queryVariable) ->
//
