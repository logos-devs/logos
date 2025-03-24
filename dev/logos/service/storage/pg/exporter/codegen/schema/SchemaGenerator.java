package dev.logos.service.storage.pg.exporter.codegen.schema;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import dev.logos.service.storage.pg.Identifier;
import dev.logos.service.storage.pg.Schema;
import dev.logos.service.storage.pg.exporter.descriptor.SchemaDescriptor;

import java.util.LinkedHashMap;

import static javax.lang.model.element.Modifier.*;

public class SchemaGenerator {
    public SchemaGenerator() {
    }

    public TypeSpec generate(SchemaDescriptor schemaDescriptor,
                             LinkedHashMap<String, TypeSpec> tableClasses) {

        TypeSpec.Builder schemaClassBuilder = TypeSpec.classBuilder(schemaDescriptor.getClassName())
                                                      .addModifiers(PUBLIC)
                                                      .superclass(Schema.class)
                                                      .addMethod(MethodSpec.constructorBuilder()
                                                                           .addStatement("super($S, $S)",
                                                                                   schemaDescriptor.name(),
                                                                                   Identifier.quoteIdentifier(
                                                                                           schemaDescriptor.name()))
                                                                           .build())
                                                      .addTypes(tableClasses.values());

        tableClasses.forEach((tableInstanceVariableName, tableClass) -> {
            ClassName tableClassName = ClassName.bestGuess(tableClass.name);
            schemaClassBuilder.addField(
                    FieldSpec.builder(tableClassName,
                                     tableInstanceVariableName,
                                     PUBLIC, STATIC, FINAL)
                             .initializer("new $T()", tableClassName)
                             .build());
        });

        return schemaClassBuilder.build();
    }
}
