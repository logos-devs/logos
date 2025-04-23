package dev.logos.service.storage.pg.exporter.codegen.module;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.squareup.javapoet.*;
import dev.logos.app.register.registerModule;
import dev.logos.service.storage.EntityStorage;
import dev.logos.service.storage.TableStorage;
import dev.logos.service.storage.pg.exporter.descriptor.SchemaDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.TableDescriptor;

import java.io.IOException;
import java.util.UUID;

import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;

public class StorageModuleGenerator {

    public JavaFile generate(
            String targetPackage,
            SchemaDescriptor schemaDescriptor,
            TableDescriptor tableDescriptor
    ) throws IOException {
        String packageName = targetPackage + "." + schemaDescriptor.name() + "." + tableDescriptor.name();

        ClassName entityClassName = ClassName.bestGuess(
                targetPackage + "." + schemaDescriptor.name() + "." + tableDescriptor.getClassName());
        ClassName tableStorageClassName = ClassName.bestGuess(
                packageName + "." + tableDescriptor.getClassName() + "TableStorage");

        String tableInstanceVariableName = tableDescriptor.getInstanceVariableName();

        return JavaFile
                .builder(packageName,
                        TypeSpec.classBuilder("StorageModule")
                                .addAnnotation(AnnotationSpec.builder(registerModule.class).build())
                                .addModifiers(PUBLIC)
                                .superclass(AbstractModule.class)
                                .addMethod(MethodSpec.methodBuilder("configure")
                                                     .addAnnotation(Override.class)
                                                     .addModifiers(PROTECTED)
                                                     .addStatement("bind($T.class).in($T.class)",
                                                             tableStorageClassName,
                                                             Singleton.class)
                                                     .addStatement(
                                                             "bind(new $T(){}).to($T.class)",
                                                             ParameterizedTypeName.get(
                                                                     ClassName.get(TypeLiteral.class),
                                                                     ParameterizedTypeName.get(
                                                                             ClassName.get(EntityStorage.class),
                                                                             entityClassName,
                                                                             ClassName.get(UUID.class))),
                                                             tableStorageClassName)
                                                     .addStatement("super.configure()")
                                                     .build())
                                .build()
                )
                .addStaticImport(
                        ClassName.bestGuess(String.format("%s.%s", targetPackage, schemaDescriptor.getClassName())),
                        tableInstanceVariableName)
                .build();
    }
}
