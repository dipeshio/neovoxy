package me.cortex.neovoxy.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class NativeLoader {
    private static final String NATIVE_DIR = "/META-INF/natives/linux";
    private static final String[] LIBRARIES = {
            "liblwjgl_lmdb.so",
            "liblwjgl_zstd.so"
    };

    public static void load() {
        try {
            Path tempDir = Files.createTempDirectory("neovoxy_natives");
            tempDir.toFile().deleteOnExit();

            for (String libName : LIBRARIES) {
                String resourcePath = NATIVE_DIR + "/" + libName;
                InputStream stream = NativeLoader.class.getResourceAsStream(resourcePath);

                if (stream == null) {
                    System.err.println("NeoVoxy: Could not find native library: " + resourcePath);
                    continue;
                }

                Path tempFile = tempDir.resolve(libName);
                Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING);
                tempFile.toFile().deleteOnExit();
                tempFile.toFile().setExecutable(true);

                String absPath = tempFile.toAbsolutePath().toString();
                System.out
                        .println("NeoVoxy: Loading native library directly: " + absPath);

                try {
                    System.load(absPath);
                    System.out.println("NeoVoxy: Successfully loaded: " + libName);
                } catch (UnsatisfiedLinkError e) {
                    System.err.println("NeoVoxy: Failed to load " + libName + ": " + e.getMessage());
                }
            }

            // We NO LONGER set org.lwjgl.librarypath as it doesn't work across module
            // layers for
            // JarJar.
            // Direct System.load() injects it into the ClassLoader.

        } catch (IOException e) {
            throw new RuntimeException("NeoVoxy: Failed to unpack natives", e);
        }
    }
}
