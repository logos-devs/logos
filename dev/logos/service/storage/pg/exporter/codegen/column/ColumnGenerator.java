package dev.logos.service.storage.pg.exporter.codegen.column;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import dev.logos.service.storage.pg.Column;
import dev.logos.service.storage.pg.Identifier;
import dev.logos.service.storage.pg.exporter.descriptor.ColumnDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.TableDescriptor;

import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

public class ColumnGenerator {
    public ColumnGenerator() {
    }

    public TypeSpec generate(TableDescriptor tableDescriptor,
                             ColumnDescriptor columnDescriptor) {
        String columnIdentifier = columnDescriptor.name();

        String columnClassNameStr = Identifier.snakeToCamelCase(columnIdentifier);
        if (columnClassNameStr.equals(tableDescriptor.getClassName().simpleName())) {
            columnClassNameStr = columnClassNameStr + "_";
        }
        ClassName columnClassName = ClassName.bestGuess(columnClassNameStr);

        return TypeSpec.classBuilder(columnClassName)
                       .addModifiers(PUBLIC, STATIC)
                       .superclass(Column.class)
                       .addMethod(MethodSpec.constructorBuilder()
                                            .addStatement("super($S, $S, $S)",
                                                    columnIdentifier,
                                                    Identifier.quoteIdentifier(tableDescriptor.name()) + '.'
                                                            + Identifier.quoteIdentifier(columnIdentifier),
                                                    columnDescriptor.type())
                                            .build())
                       .build();
    }
}
