package dev.logos.app.register;

import javax.annotation.processing.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.lang.model.SourceVersion.RELEASE_8;
import static javax.tools.StandardLocation.CLASS_OUTPUT;


@SupportedAnnotationTypes("dev.logos.app.register.registerModule")
@SupportedSourceVersion(RELEASE_8)
public class RegisterModuleProcessor extends AbstractProcessor {

    private static final Set<Element> annotatedClasses = new HashSet<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Filer filer = processingEnv.getFiler();

        roundEnv.getElementsAnnotatedWith(registerModule.class)
                .stream()
                .filter(element -> element.getKind().isClass())
                .forEach(annotatedClasses::add);

        if (roundEnv.processingOver() && !annotatedClasses.isEmpty()) {
            try {
                for (Element element : annotatedClasses) {
                    String modulePath = processingEnv.getElementUtils().getPackageOf(element)
                                                     .getQualifiedName()
                                                     .toString();
                    String uniqueFileName = "META-INF/app-modules-%s".formatted(
                        modulePath.replace('.', '-') + ".txt");

                    FileObject file = filer.createResource(CLASS_OUTPUT, "", uniqueFileName);

                    OutputStreamWriter osw = new OutputStreamWriter(file.openOutputStream(), UTF_8);
                    try (Writer writer = new BufferedWriter(osw)) {
                        writer.write(element.toString() + "\n");
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }
}
