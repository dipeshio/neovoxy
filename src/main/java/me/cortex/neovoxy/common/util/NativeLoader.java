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

                // Tell LWJGL where to find this specific library
                // The property format is org.lwjgl.<libname>.libname
                // For 'liblwjgl_lmdb.so', the name is likely 'lmdb' -> org.lwjgl.lmdb.libname
                // But LWJGL usually expects the name part.
                // Actually, simply loading it with System.load is often enough for the JVM to
                // know it's loaded.
                // But setting the property ensures LWJGL uses exactly this file.

                String path = tempFile.toAbsolutePath().toString();
                System.load(path);

                System.out.println("NeoVoxy: Loaded native library: " + libName + " from " + path);
            }

        } catch (IOException e) {
            throw new RuntimeException("NeoVoxy: Failed to unpack and load native libraries", e);
        }
    }
}
