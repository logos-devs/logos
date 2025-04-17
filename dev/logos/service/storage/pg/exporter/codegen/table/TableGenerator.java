package dev.logos.service.storage.pg.exporter.codegen.table;

import com.google.inject.Inject;
import com.squareup.javapoet.*;
import dev.logos.service.storage.exceptions.EntityReadException;
import dev.logos.service.storage.pg.Column;
import dev.logos.service.storage.pg.Identifier;
import dev.logos.service.storage.pg.Relation;
import dev.logos.service.storage.pg.exporter.descriptor.ColumnDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.QualifierDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.SchemaDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.TableDescriptor;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;
import org.jdbi.v3.core.statement.Query;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static dev.logos.service.storage.pg.Identifier.snakeToCamelCase;
import static javax.lang.model.element.Modifier.*;

public class TableGenerator {
    private final Map<String, PgTypeMapper> pgColumnTypeMappers;

    @Inject
    public TableGenerator(
            Map<String, PgTypeMapper> pgColumnTypeMappers
    ) {
        this.pgColumnTypeMappers = pgColumnTypeMappers;
    }

    PgTypeMapper getPgTypeMapper(String type) {
        if (!pgColumnTypeMappers.containsKey(type)) {
            throw new RuntimeException("There is no PgTypeMapper bound for type: " + type);
        }

        return pgColumnTypeMappers.get(type);
    }

    public TypeSpec generate(String targetPackage,
                             SchemaDescriptor schemaDescriptor,
                             TableDescriptor tableDescriptor,
                             Iterable<TypeSpec> columnClasses,
                             List<ColumnDescriptor> columnDescriptors,
                             Iterable<TypeSpec> qualifierClasses) {

        ClassName resultProtoClassName =
                ClassName.get(targetPackage + "." + schemaDescriptor.name(),
                        tableDescriptor.getClassName().simpleName());

        TypeSpec.Builder tableClassBuilder = TypeSpec
                .classBuilder(tableDescriptor.getClassName())
                .addModifiers(PUBLIC, STATIC)
                .superclass(Relation.class)
                .addMethod(MethodSpec.constructorBuilder()
                                     .addStatement("super($S, $S, $S)",
                                             schemaDescriptor.name(),
                                             tableDescriptor.name(),
                                             Identifier.quoteIdentifier(
                                                     schemaDescriptor.name()) + "." + Identifier.quoteIdentifier(
                                                     tableDescriptor.name()))
                                     .build())
                .addMethod(
                        MethodSpec
                                .methodBuilder("toProtobuf")
                                .addModifiers(PUBLIC)
                                .addException(EntityReadException.class)
                                .addParameter(ResultSet.class, "resultSet")
                                .returns(resultProtoClassName)
                                .addStatement(
                                        CodeBlock.builder()
                                                 .beginControlFlow("try")
                                                 .add(
                                                         CodeBlock.builder()
                                                                  .add("$T.Builder builder = $T.newBuilder();\n",
                                                                          resultProtoClassName,
                                                                          resultProtoClassName)
                                                                  .add(columnDescriptors
                                                                          .stream()
                                                                          .map(columnDescriptor -> {
                                                                              String columnName = columnDescriptor.name();
                                                                              String setterName = snakeToCamelCase(columnDescriptor.name());
                                                                              PgTypeMapper typeMapper = getPgTypeMapper(columnDescriptor.type());
                                                                              return CodeBlock.of(
                                                                                      "if (resultSet.getObject($S) != null) { builder.$N($L); }",
                                                                                      columnName,
                                                                                      "%s%s%s".formatted(
                                                                                              typeMapper.protoFieldRepeated() ? "addAll" : "set",
                                                                                              setterName.substring(0, 1).toUpperCase(),
                                                                                              setterName.substring(1)
                                                                                      ),
                                                                                      typeMapper.pgToProto(
                                                                                              CodeBlock.of(
                                                                                                      "$L resultSet.$N($S)",
                                                                                                      typeMapper.resultSetFieldCast(),
                                                                                                      typeMapper.resultSetFieldGetter(),
                                                                                                      columnName)));
                                                                          }).collect(CodeBlock.joining("\n")))
                                                                  .add("return builder.build()")
                                                                  .build()).build()
                                )
                                .nextControlFlow("catch ($T e)", SQLException.class)
                                .addStatement("throw new $T(e)", EntityReadException.class)
                                .endControlFlow()
                                .build())
                .addTypes(columnClasses);

        // Relation.getColumns
        MethodSpec.Builder getColumnsMethodBuilder =
                MethodSpec.methodBuilder("getColumns")
                          .addModifiers(PUBLIC)
                          .returns(ParameterizedTypeName.get(ClassName.get(Map.class),
                                  ClassName.get(String.class),
                                  ClassName.get(Column.class)));

        CodeBlock.Builder mapOfBuilder = CodeBlock.builder().add("return $T.ofEntries(", Map.class);

        int columnIndex = 0;
        for (ColumnDescriptor columnDescriptor : columnDescriptors) {
            String className = snakeToCamelCase(columnDescriptor.name());
            if (className.equals(tableDescriptor.getClassName().simpleName())) {
                className = className + "_";
            }

            ClassName columnClassName = ClassName.bestGuess(className);
            tableClassBuilder.addField(
                    FieldSpec.builder(columnClassName, columnDescriptor.getInstanceVariableName(), PUBLIC, STATIC, FINAL)
                             .initializer("new $T()", columnClassName)
                             .build());

            if (columnIndex > 0) {
                mapOfBuilder.add(",");
            }

            mapOfBuilder.add("\n  $T.entry($S, (Column) $L)", Map.class, columnDescriptor.name(),
                    columnDescriptor.getInstanceVariableName());
            columnIndex++;
        }

        mapOfBuilder.add("\n);");

        getColumnsMethodBuilder.addCode(mapOfBuilder.build());

        tableClassBuilder.addMethod(getColumnsMethodBuilder.build());

        // Relation.bindFields
        MethodSpec.Builder bindFieldsMethod = MethodSpec.methodBuilder("bindFields")
                                                        .addModifiers(PUBLIC)
                                                        .addParameter(ParameterizedTypeName.get(ClassName.get(Map.class),
                                                                ClassName.get(String.class),
                                                                ClassName.get(Object.class)), "fields")
                                                        .addParameter(ClassName.get(Query.class), "query");

        for (ColumnDescriptor columnDescriptor : columnDescriptors) {
            String columnType = columnDescriptor.type();
            if (!pgColumnTypeMappers.containsKey(columnType)) {
                throw new RuntimeException("There is no PgTypeMapper bound for type: " + columnType);
            }

            PgTypeMapper typeMapper = pgColumnTypeMappers.get(columnType);
            String columnName = columnDescriptor.name();
            CodeBlock typeMapperCodeBlock = typeMapper.protoToPg("query", "fields", columnName);
            bindFieldsMethod.addCode("if (fields.containsKey($S)) { $L\n }", columnName, typeMapperCodeBlock);
        }

        tableClassBuilder.addMethod(bindFieldsMethod.build());

        tableClassBuilder.addTypes(qualifierClasses);

        for (QualifierDescriptor qualifierDescriptor : tableDescriptor.qualifierDescriptors()) {
            ClassName qualifierClassName = ClassName.bestGuess(snakeToCamelCase(qualifierDescriptor.name()));
            tableClassBuilder.addField(
                    FieldSpec.builder(qualifierClassName, qualifierDescriptor.getInstanceVariableName(), PUBLIC, STATIC, FINAL)
                             .initializer("new $T()", qualifierClassName)
                             .build());
        }

        return tableClassBuilder.build();
    }
}
