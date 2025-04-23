package dev.logos.service.storage.pg.exporter.codegen.storage;

import com.squareup.javapoet.*;
import dev.logos.app.register.registerModule;
import dev.logos.service.storage.TableStorage;
import dev.logos.service.storage.pg.exporter.descriptor.SchemaDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.TableDescriptor;

import java.io.IOException;
import java.util.UUID;

import static javax.lang.model.element.Modifier.PUBLIC;

public class TableStorageGenerator {
    public JavaFile generate(
            String targetPackage,
            SchemaDescriptor schemaDescriptor,
            TableDescriptor tableDescriptor
    ) throws IOException {
        String packageName = targetPackage + "." + schemaDescriptor.name() + "." + tableDescriptor.name();

        ClassName entityClassName = ClassName.bestGuess(
                targetPackage + "." + schemaDescriptor.name() + "." + tableDescriptor.getClassName());
        String tableInstanceVariableName = tableDescriptor.getInstanceVariableName();

        return JavaFile
                .builder(packageName,
                        TypeSpec.classBuilder(tableDescriptor.getClassName() + "TableStorage")
                                .addModifiers(PUBLIC)
                                .superclass(
                                        ParameterizedTypeName.get(
                                                ClassName.get(TableStorage.class),
                                                entityClassName,
                                                ClassName.get(UUID.class))
                                )
                                .addMethod(MethodSpec.constructorBuilder()
                                                     .addStatement("super($L, $T.class, $T.class)",
                                                             tableInstanceVariableName,
                                                             entityClassName,
                                                             ClassName.get(UUID.class)
                                                     )
                                                     .build())
                                .build()
                )
                .addStaticImport(
                        ClassName.bestGuess(String.format("%s.%s", targetPackage, schemaDescriptor.getClassName())),
                        tableInstanceVariableName)
                .build();
    }
}
