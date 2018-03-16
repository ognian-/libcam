package ogi.libcam;

import android.content.res.AssetManager;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class PreviewRenderer implements EglContextThread.Renderer, DestroyableGLSurfaceView.DestroyableRenderer {

    private final Pass mBlit;

    public PreviewRenderer(AssetManager am) throws IOException {
        mBlit = new Pass(GLHelper.loadShaderSource(am, "shaders/blit.vert"), GLHelper.loadShaderSource(am, "shaders/blit.frag"));
    }

    @Override
    public void onCreate() {
        mBlit.onCreate();
    }

    @Override
    public void onDraw(BaseTextureInput ... inputs) {
        mBlit.onDraw(inputs[0]);
    }

    @Override
    public void onDestroy() {
        mBlit.onDestroy();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        onCreate();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
    }

    @Override
    public void onDrawFrame(GL10 gl) {
    }
}
