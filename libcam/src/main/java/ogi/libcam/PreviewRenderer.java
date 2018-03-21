package ogi.libcam;

import android.content.res.AssetManager;

import java.io.IOException;

import ogi.libgl.BaseTextureInput;
import ogi.libgl.context.EglContextThread;
import ogi.libgl.util.GLHelper;

public class PreviewRenderer implements EglContextThread.Callback {

    private final Pass mBlit;

    public PreviewRenderer(AssetManager am) throws IOException {
        mBlit = new Pass(GLHelper.loadShaderSource(am, "shaders/blit.vert"), GLHelper.loadShaderSource(am, "shaders/blit.frag"));
    }

    @Override
    public void onCreate() {
        mBlit.onCreate();
    }

    @Override
    public void onDraw(BaseTextureInput... inputs) {
        if (inputs == null || inputs.length < 1) throw new IllegalArgumentException("Must provide the input texture");
        mBlit.onDraw(inputs[0]);
    }

    @Override
    public void onDestroy() {
        mBlit.onDestroy();
    }

}
