package dev.logos.service.storage.pg.exporter.codegen.type;

import com.squareup.javapoet.CodeBlock;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;

import java.util.List;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE;

public class DoubleMapper extends PgTypeMapper {
    public DoubleMapper() {
        super(List.of("pg_catalog.float8"), TYPE_DOUBLE, "getDouble");
    }

    @Override
    public CodeBlock protoToPg(String queryVariable, String dbField, String protoVariable, String protoGetter) {
        return CodeBlock.of("$L.bind($S, (Double) $L.$L());",
                queryVariable, dbField, protoVariable, protoGetter);
    }
}