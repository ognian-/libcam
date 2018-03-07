package ogi.libcam;

import android.opengl.EGL14;

import java.util.HashMap;

import static ogi.libcam.GLHelper.assertOneOf;
import static ogi.libcam.GLHelper.assertRange;

public class EglAttribs {

    private final HashMap<Integer, Integer> m_attribs = new HashMap<>();

    EglAttribs setAlphaMaskSize(int value) {
        assertRange(value, 0, 32);
        m_attribs.put(EGL14.EGL_ALPHA_MASK_SIZE, value);
        return this;
    }

    EglAttribs setAlphaSize(int value) {
        assertRange(value, 0, 32);
        m_attribs.put(EGL14.EGL_ALPHA_SIZE, value);
        return this;
    }

    EglAttribs setBindToTextureRGB(int value) {
        assertOneOf(value, GLHelper.EGL_DONT_CARE, EGL14.EGL_TRUE, EGL14.EGL_FALSE);
        m_attribs.put(EGL14.EGL_BIND_TO_TEXTURE_RGB, value);
        return this;
    }

    EglAttribs setBindToTextureRGBA(int value) {
        assertOneOf(value, GLHelper.EGL_DONT_CARE, EGL14.EGL_TRUE, EGL14.EGL_FALSE);
        m_attribs.put(EGL14.EGL_BIND_TO_TEXTURE_RGBA, value);
        return this;
    }

    EglAttribs setBlueSize(int value) {
        assertRange(value, 0, 32);
        m_attribs.put(EGL14.EGL_BLUE_SIZE, value);
        return this;
    }

    EglAttribs setBufferSize(int value) {
        assertRange(value, 0, 32);
        m_attribs.put(EGL14.EGL_BUFFER_SIZE, value);
        return this;
    }

    EglAttribs setColorBufferType(int value) {
        assertOneOf(value, EGL14.EGL_RGB_BUFFER, EGL14.EGL_LUMINANCE_BUFFER);
        m_attribs.put(EGL14.EGL_COLOR_BUFFER_TYPE, value);
        return this;
    }

    EglAttribs setConfigCaveat(int value) {
        assertOneOf(value, GLHelper.EGL_DONT_CARE, EGL14.EGL_NONE, EGL14.EGL_SLOW_CONFIG, EGL14.EGL_NON_CONFORMANT_CONFIG);
        m_attribs.put(EGL14.EGL_CONFIG_CAVEAT, value);
        return this;
    }

    EglAttribs setConfigID(int value) {
        m_attribs.put(EGL14.EGL_CONFIG_ID, value);
        return this;
    }

    EglAttribs setConformant(int[] values) {
        int value = 0;
        for (int v : values) {
            assertOneOf(v, EGL14.EGL_OPENGL_BIT, EGL14.EGL_OPENGL_ES_BIT, EGL14.EGL_OPENGL_ES2_BIT, EGL14.EGL_OPENVG_BIT);
            value |= v;
        }
        m_attribs.put(EGL14.EGL_CONFORMANT, value);
        return this;
    }

    EglAttribs setDepthSize(int value) {
        assertRange(value, 0, 32);
        m_attribs.put(EGL14.EGL_DEPTH_SIZE, value);
        return this;
    }

    EglAttribs setGreenSize(int value) {
        assertRange(value, 0, 32);
        m_attribs.put(EGL14.EGL_GREEN_SIZE, value);
        return this;
    }

    EglAttribs setLevel(int value) {
        m_attribs.put(EGL14.EGL_LEVEL, value);
        return this;
    }

    EglAttribs setLuminanceSize(int value) {
        assertRange(value, 0, 32);
        m_attribs.put(EGL14.EGL_LUMINANCE_SIZE, value);
        return this;
    }

    EglAttribs setMatchNativePixmap(Object value) {
        throw new UnsupportedOperationException();
    }

    EglAttribs setNativeRenderable(int value) {
        assertOneOf(value, GLHelper.EGL_DONT_CARE, EGL14.EGL_TRUE, EGL14.EGL_FALSE);
        m_attribs.put(EGL14.EGL_NATIVE_RENDERABLE, value);
        return this;
    }

    EglAttribs setMaxSwapInterval(int value) {
        assertRange(value, 0, Integer.MAX_VALUE);
        m_attribs.put(EGL14.EGL_MAX_SWAP_INTERVAL, value);
        return this;
    }

    EglAttribs setMinSwapInterval(int value) {
        assertRange(value, 0, Integer.MAX_VALUE);
        m_attribs.put(EGL14.EGL_MIN_SWAP_INTERVAL, value);
        return this;
    }

    EglAttribs setRedSize(int value) {
        assertRange(value, 0, 32);
        m_attribs.put(EGL14.EGL_RED_SIZE, value);
        return this;
    }

    EglAttribs setSampleBuffers(int value) {
        assertRange(value, 0, 1);
        m_attribs.put(EGL14.EGL_SAMPLE_BUFFERS, value);
        return this;
    }

    EglAttribs setSamples(int value) {
        assertRange(value, 0, Integer.MAX_VALUE);
        m_attribs.put(EGL14.EGL_SAMPLES, value);
        return this;
    }

    EglAttribs setStencilSize(int value) {
        assertRange(value, 0, 32);
        m_attribs.put(EGL14.EGL_STENCIL_SIZE, value);
        return this;
    }

    EglAttribs setRenderableType(int value) {
        m_attribs.put(EGL14.EGL_RENDERABLE_TYPE, value);
        return this;
    }

    EglAttribs setSurfaceType(int value) {
        assertOneOf(value,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_MULTISAMPLE_RESOLVE_BOX_BIT, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_PIXMAP_BIT, EGL14.EGL_SWAP_BEHAVIOR_PRESERVED_BIT, EGL14.EGL_VG_ALPHA_FORMAT_PRE_BIT,
            EGL14.EGL_VG_COLORSPACE_LINEAR_BIT, EGL14.EGL_WINDOW_BIT, EGL14.EGL_TRANSPARENT_TYPE);
        m_attribs.put(EGL14.EGL_SURFACE_TYPE, value);
        return this;
    }

    EglAttribs setTransparentRedValue(int value) {
        assertRange(value, 0, 32);
        m_attribs.put(EGL14.EGL_TRANSPARENT_RED_VALUE, value);
        return this;
    }

    EglAttribs setTransparentGreenValue(int value) {
        assertRange(value, 0, 32);
        m_attribs.put(EGL14.EGL_TRANSPARENT_GREEN_VALUE, value);
        return this;
    }

    EglAttribs setTransparentBlueValue(int value) {
        assertRange(value, 0, 32);
        m_attribs.put(EGL14.EGL_TRANSPARENT_BLUE_VALUE, value);
        return this;
    }

    public int[] getAttribs() {
        final int[] attribs = new int[m_attribs.size() * 2 + 1];
        int i = 0;
        for (final int key : m_attribs.keySet()) {
            attribs[i++] = key;
            attribs[i++] = m_attribs.get(key);
        }
        attribs[i] = EGL14.EGL_NONE;
        return attribs;
    }

}