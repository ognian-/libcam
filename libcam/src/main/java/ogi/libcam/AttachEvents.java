package ogi.libcam;

import ogi.libgl.util.WaitResult;

public class AttachEvents {

    public interface Callback {
        void onAttach();
        void onDetach();
    }

    private final Object mLock = new Object();
    private boolean mAttached = false;
    private WaitResult mAttach = null;
    private WaitResult mDetach = null;

    public void setAttached(boolean attached) {
        synchronized (mLock) {
            mAttached = attached;
        }
    }

    public boolean isAttached() {
        synchronized (mLock) {
            return mAttached;
        }
    }

    public void attach() {
        WaitResult attach;
        synchronized (mLock) {
            if (mAttached) return;
            if (mAttach != null) {
                attach = mAttach;
            } else {
                attach = mAttach = new WaitResult();
            }
        }
        attach.getResult();
    }

    public void detach() {
        WaitResult detach;
        synchronized (mLock) {
            if (!mAttached) return;
            if (mDetach != null) {
                detach = mDetach;
            } else {
                detach = mDetach = new WaitResult();
            }
        }
        detach.getResult();
    }

    public void handleEvents(Callback callback) {
        synchronized (mLock) {
            if (mAttach != null) {
                callback.onAttach();
                mAttach.setResult(null);
                mAttach = null;
                mAttached = true;
            }
            if (mDetach != null) {
                callback.onDetach();
                mDetach.setResult(null);
                mDetach = null;
                mAttached = false;
            }
        }
    }

}
