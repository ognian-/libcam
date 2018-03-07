package ogi.libcam;

import android.content.res.AssetManager;
import android.opengl.GLES20;

import java.io.IOException;

public class BaseRenderer implements OutputSurface.Renderer {

    private static final String TAG = "LibCam";

    private final Pass mBlit;

    public BaseRenderer(AssetManager am) throws IOException {
        mBlit = new Pass(GLHelper.loadShaderSource(am, "shaders/blit.vert"), GLHelper.loadShaderSource(am, "shaders/blit.frag"));
    }

    @Override
    public void onCreate() {
        mBlit.onCreate();
    }

    @Override
    public void onDraw(ExternalTexture texture) {
        mBlit.onDraw(texture, GLES20.GL_TEXTURE0);
    }

    @Override
    public void onDestroy() {
        mBlit.onDestroy();
    }

}
