package dev.logos.service.storage.pg.exporter.codegen.table;

import com.google.inject.Inject;
import com.squareup.javapoet.*;
import dev.logos.service.storage.exceptions.EntityReadException;
import dev.logos.service.storage.pg.Column;
import dev.logos.service.storage.pg.Identifier;
import dev.logos.service.storage.pg.Relation;
import dev.logos.service.storage.pg.exporter.descriptor.*;
import dev.logos.service.storage.pg.exporter.mapper.PgTypeMapper;
import org.jdbi.v3.core.statement.Query;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
                .addTypes(columnClasses); // Include column classes in the table class

        // Add toProtobuf method with derived field support
        MethodSpec.Builder toProtobufMethod = MethodSpec.methodBuilder("toProtobuf")
                .addModifiers(PUBLIC)
                .addException(EntityReadException.class)
                .addParameter(ResultSet.class, "resultSet")
                .returns(resultProtoClassName)
                .beginControlFlow("try");

        // Create builder for the proto message
        toProtobufMethod.addStatement("$T.Builder builder = $T.newBuilder()",
                resultProtoClassName,
                resultProtoClassName);

        // Process regular columns
        for (ColumnDescriptor columnDescriptor : columnDescriptors) {
            String columnName = columnDescriptor.name();
            String setterName = snakeToCamelCase(columnDescriptor.name());
            PgTypeMapper typeMapper = getPgTypeMapper(columnDescriptor.type());

            toProtobufMethod.beginControlFlow("if (resultSet.getObject($S) != null)", columnName)
                    .addStatement("builder.$N($L)",
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
                                            columnName)))
                    .endControlFlow();
        }

        // Process derived fields if present
        List<DerivedFieldDescriptor> derivedFields = tableDescriptor.derivedFieldDescriptors();
        if (!derivedFields.isEmpty()) {
            ClassName entityName = ClassName.get(targetPackage + "." + schemaDescriptor.name(),
                    tableDescriptor.getClassName().simpleName());
            ClassName derivedFieldValueClassName = ClassName.get(targetPackage + "." + schemaDescriptor.name(),
                    tableDescriptor.getClassName().simpleName() + "DerivedFieldValue");

            // Process each derived field
            for (DerivedFieldDescriptor derivedField : derivedFields) {
                String derivedFieldName = derivedField.name();
                String derivedFieldType = derivedField.returnType();
                PgTypeMapper typeMapper = getPgTypeMapper(derivedFieldType);

                // Check if the derived field exists in the result set
                toProtobufMethod.beginControlFlow("try")
                        .beginControlFlow("if (resultSet.getObject($S) != null)", derivedFieldName);

                // Get the appropriate field name in the DerivedFieldValue based on type
                String valueFieldName = Identifier.camelToSnakeCase(typeMapper.protoFieldTypeKeyword()) + "_value";
                // Use proper lowercase 's' for the setter method per protobuf-java convention
                String setterMethod = "set" + Identifier.snakeToCamelCase(valueFieldName).substring(0, 1).toUpperCase() 
                        + Identifier.snakeToCamelCase(valueFieldName).substring(1);

                // Create the DerivedFieldValue and set the appropriate oneof field
                toProtobufMethod.addStatement("$T.Builder valueBuilder = $T.newBuilder()",
                        derivedFieldValueClassName, derivedFieldValueClassName);

                // Use the type mapper to convert the PostgreSQL value to the proto value
                toProtobufMethod.addStatement("valueBuilder.$L($L)",
                        setterMethod,
                        typeMapper.pgToProto(
                                CodeBlock.of(
                                        "$L resultSet.$N($S)",
                                        typeMapper.resultSetFieldCast(),
                                        typeMapper.resultSetFieldGetter(),
                                        derivedFieldName)));

                // Add the derived field value to the map with the field name as the key
                toProtobufMethod.addStatement("builder.putDerivedFields($S, valueBuilder.build())", derivedFieldName);

                // End the if and try blocks
                toProtobufMethod.endControlFlow()
                        .nextControlFlow("catch ($T e)", SQLException.class)
                        .addStatement("// Ignore errors for derived fields that aren't in the result set")
                        .endControlFlow();
            }
        }

        // Build and return the entity
        toProtobufMethod.addStatement("return builder.build()")
                .nextControlFlow("catch ($T e)", SQLException.class)
                .addStatement("throw new $T(e)", EntityReadException.class)
                .endControlFlow();

        tableClassBuilder.addMethod(toProtobufMethod.build());

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
                                                        .addParameter(ClassName.get(Query.class), "query")
                                                        .addException(SQLException.class);

        for (ColumnDescriptor columnDescriptor : columnDescriptors) {
            String columnName = columnDescriptor.name();
            String columnType = columnDescriptor.type();
            if (!pgColumnTypeMappers.containsKey(columnType)) {
                throw new RuntimeException("There is no PgTypeMapper bound for type: " + columnType);
            }

            PgTypeMapper typeMapper = pgColumnTypeMappers.get(columnType);
            bindFieldsMethod.beginControlFlow("if (fields.containsKey($S))", columnName);
            bindFieldsMethod.addCode(typeMapper.protoToPg("query", "fields", columnName));
            bindFieldsMethod.addCode("\n");
            bindFieldsMethod.endControlFlow();
        }

        tableClassBuilder.addTypes(qualifierClasses);

        // Create set of qualifier names to avoid duplicates
        Set<String> qualifierNames = tableDescriptor.qualifierDescriptors().stream()
                .map(QualifierDescriptor::name)
                .collect(Collectors.toSet());

        for (QualifierDescriptor qualifierDescriptor : tableDescriptor.qualifierDescriptors()) {
            ClassName qualifierClassName = ClassName.bestGuess(snakeToCamelCase(qualifierDescriptor.name()));
            tableClassBuilder.addField(
                    FieldSpec.builder(qualifierClassName, qualifierDescriptor.getInstanceVariableName(), PUBLIC, STATIC, FINAL)
                             .initializer("new $T()", qualifierClassName)
                             .build());

            for (QualifierParameterDescriptor qualifierParameterDescriptor : qualifierDescriptor.parameters()) {
                String parameterName = qualifierParameterDescriptor.name();
                String columnType = qualifierParameterDescriptor.type();
                if (!pgColumnTypeMappers.containsKey(columnType)) {
                    throw new RuntimeException("There is no PgTypeMapper bound for type: " + columnType);
                }

                PgTypeMapper typeMapper = pgColumnTypeMappers.get(columnType);
                CodeBlock typeMapperCodeBlock = typeMapper.protoToPg("query", "fields", parameterName);
                bindFieldsMethod.beginControlFlow("if (fields.containsKey($S))", parameterName);
                bindFieldsMethod.addCode(typeMapperCodeBlock);
                bindFieldsMethod.addCode("\n");
                bindFieldsMethod.endControlFlow();
            }
        }

        tableClassBuilder.addMethod(bindFieldsMethod.build());

        // Add derived field classes as static fields - only for those NOT already defined as qualifiers
        for (DerivedFieldDescriptor derivedField : tableDescriptor.derivedFieldDescriptors()) {
            // Skip if a qualifier with the same name already exists
            if (!qualifierNames.contains(derivedField.name())) {
                tableClassBuilder.addField(
                        FieldSpec.builder(
                                ClassName.get("dev.logos.service.storage.pg", "DerivedFieldFunction"), 
                                derivedField.getInstanceVariableName(), 
                                PUBLIC, STATIC, FINAL)
                        .initializer("new dev.logos.service.storage.pg.DerivedFieldFunction($S)", derivedField.name())
                        .build());
            }
        }

        return tableClassBuilder.build();
    }
}