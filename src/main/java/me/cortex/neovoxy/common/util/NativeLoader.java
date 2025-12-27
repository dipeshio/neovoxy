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

                System.out
                        .println("NeoVoxy: Extracted native library: " + libName + " to " + tempFile.toAbsolutePath());
            }

            // Set the library path for LWJGL to find the natives
            // This is critical - it must happen BEFORE any LWJGL class is loaded
            String nativePath = tempDir.toAbsolutePath().toString();
            System.setProperty("org.lwjgl.librarypath", nativePath);
            System.out.println("NeoVoxy: Set org.lwjgl.librarypath to: " + nativePath);

        } catch (IOException e) {
            throw new RuntimeException("NeoVoxy: Failed to unpack natives", e);
        }
    }
}
