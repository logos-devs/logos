package dev.logos.service.storage.pg.exporter.codegen.service;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import dev.logos.service.storage.pg.exporter.codegen.type.StringMapper;
import dev.logos.service.storage.pg.exporter.descriptor.DerivedFieldDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.DerivedFieldParameterDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.QualifierDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.QualifierParameterDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.TableDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class StorageServiceBaseGeneratorTest {
    @Test
    public void test_generateQueryMethod_generatesValidMethod() {
        StorageServiceBaseGenerator generator = new StorageServiceBaseGenerator(Map.of("string", new StringMapper()));
        
        // Package name for qualifiers and entity
        String packageName = "app.test.storage.person";
        
        // Create the class names we need
        ClassName listRequestClassName = ClassName.bestGuess("ListPersonRequest");
        ClassName qualifierCallClassName = ClassName.bestGuess("PersonQualifierCall");
        ClassName derivedFieldCallClassName = ClassName.bestGuess("PersonDerivedFieldCall");
        
        // Generate the method
        MethodSpec queryMethodSpec = generator.generateQueryMethod(
                new TableDescriptor("person", List.of(), 
                        List.of(
                            new QualifierDescriptor("by_name", List.of(
                                    new QualifierParameterDescriptor("name", "string")
                            ))
                        ),
                        List.of(
                            new DerivedFieldDescriptor("get_friends", "string", List.of(
                                    new DerivedFieldParameterDescriptor("max_count", "string")
                            ))
                        )
                ),
                listRequestClassName,
                qualifierCallClassName,
                derivedFieldCallClassName,
                packageName);

        // Basic verification
        String methodString = queryMethodSpec.toString();
        
        // Verify qualifier handling
        assertTrue("Method should contain PersonQualifierCall handling", 
                   methodString.contains("PersonQualifierCall qualifierCall"));
        assertTrue("Method should contain qualifier switch statement", 
                   methodString.contains("switch (qualifierCall.getQualifierCase())"));
        assertTrue("Method should contain BY_NAME case", 
                   methodString.contains("case BY_NAME:"));
                   
        // Verify derived field handling
        assertTrue("Method should contain PersonDerivedFieldCall handling", 
                   methodString.contains("PersonDerivedFieldCall derivedFieldCall"));
        assertTrue("Method should contain derived field switch statement", 
                   methodString.contains("switch (derivedFieldCall.getDerivedFieldCase())"));
        assertTrue("Method should contain GET_FRIENDS case", 
                   methodString.contains("case GET_FRIENDS:"));
    }
}