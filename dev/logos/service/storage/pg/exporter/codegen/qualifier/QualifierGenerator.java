package dev.logos.service.storage.pg.exporter.codegen.qualifier;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import dev.logos.service.storage.pg.Identifier;
import dev.logos.service.storage.pg.exporter.descriptor.QualifierDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.TableDescriptor;

import javax.lang.model.element.Modifier;
import java.util.LinkedHashMap;

public class QualifierGenerator {
    public TypeSpec generate(TableDescriptor tableDescriptor,
                             QualifierDescriptor qualifierDescriptor) {
        String qualifierIdentifier = qualifierDescriptor.name();
        String qualifierClassNameStr = Identifier.snakeToCamelCase(qualifierIdentifier);
        if (qualifierClassNameStr.equals(tableDescriptor.getClassName().simpleName())) {
            qualifierClassNameStr = qualifierClassNameStr + "_";
        }
        ClassName qualifierClassName = ClassName.bestGuess(qualifierClassNameStr);
        ClassName qfp = ClassName.get("dev.logos.service.storage.pg", "QualifierFunctionParameter");

        // Step 1: Build static buildParams method
        MethodSpec.Builder buildParams = MethodSpec.methodBuilder("buildParams")
                                                   .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                                                   .returns(ParameterizedTypeName.get(
                                                           ClassName.get(LinkedHashMap.class),
                                                           ClassName.get(String.class),
                                                           qfp))
                                                   .addStatement("$T<$T, $T> m = new $T<>()",
                                                           LinkedHashMap.class, String.class, qfp,
                                                           LinkedHashMap.class);
        for (var param : qualifierDescriptor.parameters()) {
            buildParams.addStatement(
                    "m.put($S, new $T($S, $S))",
                    param.name(), qfp, param.name(), param.type());
        }
        buildParams.addStatement("return m");

        // Step 2: Constructor
        MethodSpec constructor = MethodSpec.constructorBuilder()
                                           .addModifiers(Modifier.PUBLIC)
                                           .addStatement("super($S, buildParams())", qualifierIdentifier)
                                           .build();

        // Step 3: Compose class
        return TypeSpec.classBuilder(qualifierClassName)
                       .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                       .superclass(ClassName.get("dev.logos.service.storage.pg", "QualifierFunction"))
                       .addMethod(constructor)
                       .addMethod(buildParams.build())
                       .build();
    }
}