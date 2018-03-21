package ogi.libgl;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.FloatBuffer;

import ogi.libgl.util.CheckThread;
import ogi.libgl.util.GLHelper;

import static ogi.libgl.util.GLHelper.glCheck;

public class Pass {

    private static final String TAG = "LibGL";

    private final String mVertexSource;
    private final String mFragmentSource;

    private final CheckThread mCheckThread = new CheckThread();

    private int mProgram = -1;

    private int mUniformPositionMatrix = -1;

    private int mUniformTexture = -1;
    private int mUniformTexCoordsMatrix = -1;

    private int mAttributePosition = -1;
    private int mAttributeTextCoords = -1;

    private final float[] mPositionMatrix = new float[16];

    public Pass(String vert, String frag) {
        mVertexSource = vert;
        mFragmentSource = frag;
    }

    public void onCreate() {
        mCheckThread.init();
        mProgram = GLHelper.buildProgram(mVertexSource, mFragmentSource);

        mUniformPositionMatrix = GLES20.glGetUniformLocation(mProgram, "uPositionMatrix"); glCheck();

        mUniformTexture = GLES20.glGetUniformLocation(mProgram, "uTexture"); glCheck();
        mUniformTexCoordsMatrix = GLES20.glGetUniformLocation(mProgram, "uTexCoordsMatrix"); glCheck();


        mAttributePosition = GLES20.glGetAttribLocation(mProgram, "aPosition"); glCheck();
        mAttributeTextCoords = GLES20.glGetAttribLocation(mProgram, "aTexCoords"); glCheck();

        Matrix.setIdentityM(mPositionMatrix, 0);
    }

    public void onDraw(BaseTextureInput... inputs) {
        mCheckThread.check();
        GLES20.glUseProgram(mProgram); glCheck();

        GLES20.glUniformMatrix4fv(mUniformPositionMatrix, 1, false, mPositionMatrix, 0); glCheck();

        inputs[0].onDraw(mUniformTexture, mUniformTexCoordsMatrix, GLES20.GL_TEXTURE0);

        GLES20.glEnableVertexAttribArray(mAttributePosition); glCheck();
        GLES20.glVertexAttribPointer(mAttributePosition, 2, GLES20.GL_FLOAT, false, 4 * 2, sPositionBuffer); glCheck();

        GLES20.glEnableVertexAttribArray(mAttributeTextCoords); glCheck();
        GLES20.glVertexAttribPointer(mAttributeTextCoords, 2, GLES20.GL_FLOAT, false, 4 * 2, sTexCoordsBuffer); glCheck();

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4); glCheck();

        inputs[0].onDoneDraw();
    }

    public void onDestroy() {
        mCheckThread.deinit(false);
        if (mProgram != -1) {
            GLES20.glDeleteProgram(mProgram); glCheck();
            mProgram = -1;
        }

        mUniformPositionMatrix = -1;
        mUniformTexCoordsMatrix = -1;
        mUniformTexture = -1;

        mAttributePosition = -1;
        mAttributeTextCoords = -1;
    }

    private static final FloatBuffer sPositionBuffer = GLHelper.getBuffer(1, -1, -1, -1, 1, 1, -1, 1).asReadOnlyBuffer();
    private static final FloatBuffer sTexCoordsBuffer = GLHelper.getBuffer(1,  0, 0, 0, 1, 1, 0, 1).asReadOnlyBuffer();
}
