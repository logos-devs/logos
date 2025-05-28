package dev.logos.service.storage.pg.exporter.codegen.type;

import com.squareup.javapoet.CodeBlock;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;

import java.util.List;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING;

public class StringMapper extends PgTypeMapper {
    public StringMapper() {
        super(List.of("pg_catalog.bpchar", "pg_catalog.varchar", "pg_catalog.text"), TYPE_STRING, "getString");
    }

    @Override
    public CodeBlock protoToPg(String queryVariable, String dbField, String protoVariable, String protoGetter) {
        return CodeBlock.of("$L.bind($S, (String) $L.$L());",
                queryVariable, dbField, protoVariable, protoGetter);
    }
}