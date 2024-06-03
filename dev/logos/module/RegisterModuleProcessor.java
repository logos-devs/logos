package dev.logos.module;

import javax.annotation.processing.*;
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


@SupportedAnnotationTypes("dev.logos.module.registerModule")
@SupportedSourceVersion(RELEASE_8)
public class RegisterModuleProcessor extends AbstractProcessor {

    private static final String FILE_NAME = "META-INF/app-modules.txt";
    private static final Set<String> processedClasses = new HashSet<>();
    private static boolean fileCreated = false;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Filer filer = processingEnv.getFiler();

        // collect all annotated classes in this round
        roundEnv.getElementsAnnotatedWith(registerModule.class)
                .stream()
                .filter(element -> element.getKind().isClass())
                .map(Object::toString)
                .forEach(processedClasses::add);

        // write the file on the last round
        if (roundEnv.processingOver() && !processedClasses.isEmpty()) {
            try {
                FileObject file;
                if (!fileCreated) {
                    file = filer.createResource(CLASS_OUTPUT, "", FILE_NAME);
                    fileCreated = true;
                } else {
                    file = filer.getResource(CLASS_OUTPUT, "", FILE_NAME);
                }

                OutputStreamWriter osw = new OutputStreamWriter(file.openOutputStream(), UTF_8);
                try (Writer writer = new BufferedWriter(osw)) {
                    for (String className : processedClasses) {
                        writer.write(className + "\n");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        return true;
    }
}
