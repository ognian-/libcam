package ogi.libcam;

import android.content.res.AssetManager;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLDisplay;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;
import android.util.Size;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashSet;

public final class GLHelper {

    private static final String TAG = "LibCam";

    public static final int EGL_DONT_CARE = -1;
    public static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

    public static EGLConfig chooseConfig(EGLDisplay display, EglAttribs attribs) {
        final int[] attrs = attribs.getAttribs();
        final int[] numConfigs = new int[1];
        final boolean successNum = EGL14.eglChooseConfig(display, attrs, 0, null, 0, 0, numConfigs, 0); eglCheck();
        if (!successNum) {
            return null;
        }
        final EGLConfig[] configs = new EGLConfig[numConfigs[0]];
        final boolean successChoose = EGL14.eglChooseConfig(display, attrs, 0, configs, 0, configs.length, numConfigs, 0); eglCheck();
        if (!successChoose) {
            return null;
        }
        return configs[0];
    }

    public static void glCheck() {
        final int err = GLES20.glGetError();
        if (err != GLES20.GL_NO_ERROR) {
            String error = "Unknown: 0x" + Integer.toHexString(err);
            switch (err) {
                case GLES20.GL_INVALID_ENUM: error = "GL_INVALID_ENUM"; break;
                case GLES20.GL_INVALID_VALUE: error = "GL_INVALID_VALUE"; break;
                case GLES20.GL_INVALID_OPERATION: error = "GL_INVALID_OPERATION"; break;
                case GLES20.GL_INVALID_FRAMEBUFFER_OPERATION: error = "GL_INVALID_FRAMEBUFFER_OPERATION"; break;
                case GLES20.GL_OUT_OF_MEMORY: error = "GL_OUT_OF_MEMORY"; break;
            }
            throw new RuntimeException("GL error: " + error);
        }
    }

    public static void eglCheck() {
        final int err = EGL14.eglGetError();
        if (err != EGL14.EGL_SUCCESS) {
            String error = "Unknown: 0x" + Integer.toHexString(err);
            switch (err) {
                case EGL14.EGL_NOT_INITIALIZED: error = "EGL_NOT_INITIALIZED"; break;
                case EGL14.EGL_BAD_ACCESS: error = "EGL_BAD_ACCESS"; break;
                case EGL14.EGL_BAD_ALLOC: error = "EGL_BAD_ALLOC"; break;
                case EGL14.EGL_BAD_ATTRIBUTE: error = "EGL_BAD_ATTRIBUTE"; break;
                case EGL14.EGL_BAD_CONTEXT: error = "EGL_BAD_CONTEXT"; break;
                case EGL14.EGL_BAD_CONFIG: error = "EGL_BAD_CONFIG"; break;
                case EGL14.EGL_BAD_CURRENT_SURFACE: error = "EGL_BAD_CURRENT_SURFACE"; break;
                case EGL14.EGL_BAD_DISPLAY: error = "EGL_BAD_DISPLAY"; break;
                case EGL14.EGL_BAD_SURFACE: error = "EGL_BAD_SURFACE"; break;
                case EGL14.EGL_BAD_MATCH: error = "EGL_BAD_MATCH"; break;
                case EGL14.EGL_BAD_PARAMETER: error = "EGL_BAD_PARAMETER"; break;
                case EGL14.EGL_BAD_NATIVE_PIXMAP: error = "EGL_BAD_NATIVE_PIXMAP"; break;
                case EGL14.EGL_BAD_NATIVE_WINDOW: error = "EGL_BAD_NATIVE_WINDOW"; break;
                case EGL14.EGL_CONTEXT_LOST: error = "EGL_CONTEXT_LOST"; break;
            }
            throw new RuntimeException("EGL error: " + error);
        }
    }

    public static int getTextureSlot(int name) {
        switch (name) {
            case GLES20.GL_TEXTURE0: return 0;
            case GLES20.GL_TEXTURE1: return 1;
            case GLES20.GL_TEXTURE2: return 2;
            case GLES20.GL_TEXTURE3: return 3;
            case GLES20.GL_TEXTURE4: return 4;
            case GLES20.GL_TEXTURE5: return 5;
            case GLES20.GL_TEXTURE6: return 6;
            case GLES20.GL_TEXTURE7: return 7;
            case GLES20.GL_TEXTURE8: return 8;
            default: throw new IllegalArgumentException();
        }
    }

    public static int buildProgram(String vertexSource, String fragmentSource) {
        final int vertexShader = buildShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            throw new RuntimeException();
        }

        final int fragmentShader = buildShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (fragmentShader == 0) {
            throw new RuntimeException();
        }

        final int program = GLES20.glCreateProgram(); glCheck();
        if (program == 0) {
            throw new RuntimeException();
        }

        GLES20.glAttachShader(program, vertexShader); glCheck();
        GLES20.glAttachShader(program, fragmentShader); glCheck();
        GLES20.glLinkProgram(program); glCheck();

        final int[] status = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0); glCheck();
        if (status[0] == GLES20.GL_FALSE) {
            final String infoLog = GLES20.glGetProgramInfoLog(program); glCheck();
            Log.e(TAG, infoLog);
            GLES20.glDeleteProgram(program); glCheck();
            throw new RuntimeException();
        }

        return program;
    }

    public static int buildShader(int type, String shaderSource) {
        final int shader = GLES20.glCreateShader(type); glCheck();
        if (shader == 0) {
            throw new RuntimeException();
        }

        GLES20.glShaderSource(shader, shaderSource); glCheck();
        GLES20.glCompileShader(shader); glCheck();

        final int[] status = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0); glCheck();
        if (status[0] == GLES20.GL_FALSE) {
            final String infoLog = GLES20.glGetShaderInfoLog(shader); glCheck();
            Log.e(TAG, infoLog);
            GLES20.glDeleteShader(shader); glCheck();
            throw new RuntimeException();
        }

        return shader;
    }

    public static int genTexture(int target, int format, Size size) {
        assertOneOf(target, GLES20.GL_TEXTURE_2D, GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        if (size != null) {
            assertOneOf(format, GLES20.GL_ALPHA, GLES20.GL_RGB, GLES20.GL_RGBA, GLES20.GL_LUMINANCE, GLES20.GL_LUMINANCE_ALPHA);
        }
        final int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0); glCheck();
        GLES20.glBindTexture(target, tex[0]); glCheck();
        if (size != null) {
            GLES20.glTexImage2D(target, 0, format, size.getWidth(), size.getHeight(), 0, format, GLES20.GL_UNSIGNED_BYTE, null); glCheck();
        }
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE); glCheck();
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE); glCheck();
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR); glCheck();
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR); glCheck();
        return tex[0];
    }

    public static int genTextureExternal() {
        return genTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, -1, null);
    }

    public static FloatBuffer getBuffer(float ... data) {
        FloatBuffer buffer = ByteBuffer.allocateDirect(data.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        buffer.put(data);
        buffer.position(0);
        return buffer;
    }

    public static String loadShaderSource(AssetManager am, String path) throws IOException {
        InputStreamReader isr = null;
        try {
            isr = new InputStreamReader(am.open(path), "UTF-8");
            final char[] buf = new char[1024 * 8];
            final StringBuilder str = new StringBuilder();
            int i = 0;
            while ( (i = isr.read(buf)) != -1 ) str.append(buf, 0 ,i);
            return str.toString();
        } finally {
            if (isr != null) {
                isr.close();
            }
        }
    }

    public static void assertTrue(boolean x) {
        if (!x) throw new AssertionError();
    }

    public static void assertRange(int value, int min, int max) {
        if (value < min || value > max) throw new IllegalArgumentException("Value outside range");
    }

    public static void assertOneOf(int value, int ... allowed) {
        for (int i : allowed) {
            if (value == i) return;
        }
        throw new IllegalArgumentException("Value not allowed");
    }

    private static HashSet<Object> sCreated = new HashSet<>();

    public static void signalOnCreated(Object object) {
        synchronized (sCreated) {
            if (!sCreated.add(object)) {
                throw new RuntimeException("Already created");
            }
        }
    }

    public static void signalOnDestroyed(Object object) {
        synchronized (sCreated) {
            if (!sCreated.remove(object)) {
                throw new RuntimeException("Not created");
            }
        }
    }

    public static void assertClean() {
        synchronized (sCreated) {
            if (!sCreated.isEmpty()) {
                String msg = "Not cleaned up: ";
                for (Object obj : sCreated) {
                    msg += String.valueOf(obj) + ", ";
                }
                throw new RuntimeException(msg);
            }
        }
    }

}
