package dev.logos.service.storage.pg.exporter.codegen.service;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import dev.logos.service.storage.pg.exporter.codegen.type.StringMapper;
import dev.logos.service.storage.pg.exporter.descriptor.QualifierDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.QualifierParameterDescriptor;
import dev.logos.service.storage.pg.exporter.descriptor.TableDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class StorageServiceBaseGeneratorTest {
    @Test
    public void test_generateQueryMethod_generatesValidMethod() {
        StorageServiceBaseGenerator generator = new StorageServiceBaseGenerator(Map.of("string", new StringMapper()));
        MethodSpec queryMethodSpec = generator.generateQueryMethod(new TableDescriptor("person", List.of(), List.of(
                        new QualifierDescriptor("by_name", List.of(
                                new QualifierParameterDescriptor("name", "string")
                        ))

                )), ClassName.bestGuess("ListPersonRequest"),
                "app.test.storage.person");

        // language=Java
        assertEquals("""
                        @java.lang.Override
                        public final <Request> dev.logos.service.storage.pg.Select.Builder query(Request request) {
                          dev.logos.service.storage.pg.Select.Builder builder = dev.logos.service.storage.pg.Select.builder().from(person);
                          if (request instanceof ListPersonRequest listRequest) {
                            if (listRequest.hasLimit()) {
                              builder.limit(listRequest.getLimit());
                            }
                        
                            if (listRequest.hasOffset()) {
                              builder.offset(listRequest.getOffset());
                            }
                        
                            for (app.test.storage.person.ByName byNameMessage : listRequest.getByNameList()) {
                              java.util.LinkedHashMap<java.lang.String, java.lang.Object> byNameParams = new java.util.LinkedHashMap<>();
                              byNameParams.put("name", byNameMessage.getName());
                              builder.qualifier(person.byName, byNameParams);
                            }
                          }
                          builder = query(request, builder);
                          return builder;
                        }
                        """
                , queryMethodSpec.toString());
    }
}
