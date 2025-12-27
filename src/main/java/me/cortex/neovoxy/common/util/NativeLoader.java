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
            // Use a stable, persistent directory in the user's home folder
            String userHome = System.getProperty("user.home");
            Path nativeDir = Path.of(userHome, ".neovoxy", "natives");

            if (!Files.exists(nativeDir)) {
                Files.createDirectories(nativeDir);
            }

            for (String libName : LIBRARIES) {
                String resourcePath = NATIVE_DIR + "/" + libName;
                InputStream stream = NativeLoader.class.getResourceAsStream(resourcePath);

                if (stream == null) {
                    System.err.println("NeoVoxy: Could not find native library: " + resourcePath);
                    continue;
                }

                Path targetFile = nativeDir.resolve(libName);

                // Always overwrite to ensure we have the correct version from this jar
                Files.copy(stream, targetFile, StandardCopyOption.REPLACE_EXISTING);

                // Ensure executable
                targetFile.toFile().setExecutable(true);

                String absPath = targetFile.toAbsolutePath().toString();
                System.out.println("NeoVoxy: Extracted native library to: " + absPath);
            }

            // Set the library path for LWJGL to find the natives later
            String nativePath = nativeDir.toAbsolutePath().toString();
            System.setProperty("org.lwjgl.librarypath", nativePath);
            System.out.println("NeoVoxy: Set persistent org.lwjgl.librarypath to: " + nativePath);

        } catch (IOException e) {
            throw new RuntimeException("NeoVoxy: Failed to unpack natives", e);
        }
    }
}
