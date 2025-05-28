package dev.logos.service.storage.pg.exporter.codegen.type;

import com.squareup.javapoet.CodeBlock;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;

import java.util.List;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED64;

public class NumericMapper extends PgTypeMapper {
    public NumericMapper() {
        super(List.of("pg_catalog.numeric", "pg_catalog.decimal"), TYPE_FIXED64, "getBigDecimal");
    }

    @Override
    public CodeBlock protoToPg(String queryVariable, String dbField, String protoVariable, String protoGetter) {
        return CodeBlock.of("$L.bind($S, (BigDecimal) $L.$L());",
                queryVariable, dbField, protoVariable, protoGetter);
    }
}