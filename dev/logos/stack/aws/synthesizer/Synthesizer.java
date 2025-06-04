package dev.logos.stack.aws.synthesizer;

import com.google.inject.Injector;
import dev.logos.module.ModuleLoader;
import org.slf4j.Logger;
import software.amazon.awscdk.cxapi.CloudAssembly;

import java.io.*;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.slf4j.LoggerFactory.getLogger;

public class Synthesizer {
    private static final Logger logger = getLogger(Synthesizer.class);

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Provide an output zipfile as the only argument");
            System.exit(1);
        }

        Injector injector = ModuleLoader.createInjector();
        CloudAssembly assembly = injector.getInstance(CloudAssembly.class);

        String outputDirectory = assembly.getDirectory();
        logger.info("Stack synthesis complete: {}", outputDirectory);

        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(Path.of(args[0]).toFile()))) {
            zipDirectory(new File(outputDirectory), outputDirectory, zipOut);
        }

        logger.info("Stack output zipped successfully: stack.zip");
    }

    private static void zipDirectory(File fileToZip, String sourceDir, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) return;

        if (fileToZip.isDirectory()) {
            for (File childFile : fileToZip.listFiles()) {
                zipDirectory(childFile, sourceDir, zipOut);
            }
        } else {
            try (FileInputStream fis = new FileInputStream(fileToZip)) {
                String entryPath = fileToZip.getAbsolutePath().substring(sourceDir.length() + 1);
                zipOut.putNextEntry(new ZipEntry(entryPath));

                byte[] bytes = new byte[1024];
                int length;
                while ((length = fis.read(bytes)) >= 0) {
                    zipOut.write(bytes, 0, length);
                }
            }
        }
    }
}
