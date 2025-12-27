package me.cortex.neovoxy.client.core.gl;

import me.cortex.neovoxy.common.Logger;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * OpenGL shader program wrapper with include preprocessing.
 */
public class GlShader implements AutoCloseable {
    
    private static final Pattern IMPORT_PATTERN = Pattern.compile("#import\\s+<([^>]+)>");
    
    private final int programId;
    private final Map<String, Integer> uniformLocations = new HashMap<>();
    private boolean isClosed = false;
    
    private GlShader(int programId) {
        this.programId = programId;
    }
    
    public int id() {
        return programId;
    }
    
    public void use() {
        glUseProgram(programId);
    }
    
    public static void unbind() {
        glUseProgram(0);
    }
    
    /**
     * Get uniform location (cached).
     */
    public int getUniformLocation(String name) {
        return uniformLocations.computeIfAbsent(name, n -> glGetUniformLocation(programId, n));
    }
    
    /**
     * Set uniform int.
     */
    public void setUniform(String name, int value) {
        glProgramUniform1i(programId, getUniformLocation(name), value);
    }
    
    /**
     * Set uniform float.
     */
    public void setUniform(String name, float value) {
        glProgramUniform1f(programId, getUniformLocation(name), value);
    }
    
    /**
     * Set uniform vec2.
     */
    public void setUniform(String name, float x, float y) {
        glProgramUniform2f(programId, getUniformLocation(name), x, y);
    }
    
    /**
     * Set uniform vec3.
     */
    public void setUniform(String name, float x, float y, float z) {
        glProgramUniform3f(programId, getUniformLocation(name), x, y, z);
    }
    
    /**
     * Set uniform vec4.
     */
    public void setUniform(String name, float x, float y, float z, float w) {
        glProgramUniform4f(programId, getUniformLocation(name), x, y, z, w);
    }
    
    /**
     * Set uniform ivec3.
     */
    public void setUniform(String name, int x, int y, int z) {
        glProgramUniform3i(programId, getUniformLocation(name), x, y, z);
    }
    
    /**
     * Set uniform mat4.
     */
    public void setUniformMatrix4(String name, boolean transpose, float[] matrix) {
        glProgramUniformMatrix4fv(programId, getUniformLocation(name), transpose, matrix);
    }
    
    @Override
    public void close() {
        if (!isClosed) {
            glDeleteProgram(programId);
            isClosed = true;
        }
    }
    
    /**
     * Builder for creating shader programs.
     */
    public static class Builder {
        private String vertexSource;
        private String fragmentSource;
        private String computeSource;
        private String geometrySource;
        private final Map<String, String> defines = new HashMap<>();
        
        public Builder vertex(String source) {
            this.vertexSource = source;
            return this;
        }
        
        public Builder fragment(String source) {
            this.fragmentSource = source;
            return this;
        }
        
        public Builder compute(String source) {
            this.computeSource = source;
            return this;
        }
        
        public Builder geometry(String source) {
            this.geometrySource = source;
            return this;
        }
        
        public Builder define(String name) {
            this.defines.put(name, "");
            return this;
        }
        
        public Builder define(String name, String value) {
            this.defines.put(name, value);
            return this;
        }
        
        public Builder define(String name, int value) {
            this.defines.put(name, String.valueOf(value));
            return this;
        }
        
        /**
         * Load shader source from resource.
         */
        public static String loadSource(String resourcePath) {
            try (InputStream is = GlShader.class.getResourceAsStream("/assets/neovoxy/shaders/" + resourcePath)) {
                if (is == null) {
                    throw new RuntimeException("Shader not found: " + resourcePath);
                }
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load shader: " + resourcePath, e);
            }
        }
        
        /**
         * Process #import directives.
         */
        private String processImports(String source) {
            Matcher matcher = IMPORT_PATTERN.matcher(source);
            StringBuffer result = new StringBuffer();
            
            while (matcher.find()) {
                String importPath = matcher.group(1);
                // Convert neovoxy:path/to/file.glsl to path/to/file.glsl
                if (importPath.startsWith("neovoxy:")) {
                    importPath = importPath.substring(8);
                } else if (importPath.startsWith("voxy:")) {
                    // Legacy voxy: prefix
                    importPath = importPath.substring(5);
                }
                
                String importedSource = loadSource(importPath);
                // Recursively process imports
                importedSource = processImports(importedSource);
                matcher.appendReplacement(result, Matcher.quoteReplacement(importedSource));
            }
            matcher.appendTail(result);
            
            return result.toString();
        }
        
        /**
         * Prepend defines to shader source.
         */
        private String prependDefines(String source) {
            if (defines.isEmpty()) {
                return source;
            }
            
            StringBuilder sb = new StringBuilder();
            // Insert defines after #version line
            int versionEnd = source.indexOf('\n');
            if (source.startsWith("#version")) {
                sb.append(source, 0, versionEnd + 1);
                source = source.substring(versionEnd + 1);
            }
            
            for (Map.Entry<String, String> entry : defines.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    sb.append("#define ").append(entry.getKey()).append("\n");
                } else {
                    sb.append("#define ").append(entry.getKey()).append(" ").append(entry.getValue()).append("\n");
                }
            }
            
            sb.append(source);
            return sb.toString();
        }
        
        private int compileShader(int type, String source) {
            source = processImports(source);
            source = prependDefines(source);
            
            int shader = glCreateShader(type);
            glShaderSource(shader, source);
            glCompileShader(shader);
            
            if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
                String log = glGetShaderInfoLog(shader);
                glDeleteShader(shader);
                throw new RuntimeException("Shader compilation failed:\n" + log);
            }
            
            return shader;
        }
        
        public GlShader build() {
            int program = glCreateProgram();
            int[] shaders = new int[4];
            int shaderCount = 0;
            
            try {
                if (vertexSource != null) {
                    shaders[shaderCount++] = compileShader(GL_VERTEX_SHADER, vertexSource);
                }
                if (fragmentSource != null) {
                    shaders[shaderCount++] = compileShader(GL_FRAGMENT_SHADER, fragmentSource);
                }
                if (geometrySource != null) {
                    shaders[shaderCount++] = compileShader(GL_GEOMETRY_SHADER, geometrySource);
                }
                if (computeSource != null) {
                    shaders[shaderCount++] = compileShader(GL_COMPUTE_SHADER, computeSource);
                }
                
                for (int i = 0; i < shaderCount; i++) {
                    glAttachShader(program, shaders[i]);
                }
                
                glLinkProgram(program);
                
                if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
                    String log = glGetProgramInfoLog(program);
                    throw new RuntimeException("Shader linking failed:\n" + log);
                }
                
                // Detach and delete shaders after linking
                for (int i = 0; i < shaderCount; i++) {
                    glDetachShader(program, shaders[i]);
                    glDeleteShader(shaders[i]);
                }
                
                return new GlShader(program);
                
            } catch (Exception e) {
                // Clean up on failure
                for (int i = 0; i < shaderCount; i++) {
                    if (shaders[i] != 0) {
                        glDeleteShader(shaders[i]);
                    }
                }
                glDeleteProgram(program);
                throw e;
            }
        }
    }
}
