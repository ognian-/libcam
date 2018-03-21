package ogi.libgl.util;

public class CheckThread {

    private final Object mLock = new Object();
    private long mThreadId = -1;
    private String mThreadName;

    public void init() {
        synchronized (mLock) {
            if (mThreadId != -1) throw new IllegalStateException("Already inited");
            mThreadId = Thread.currentThread().getId();
            mThreadName = Thread.currentThread().getName();
        }
    }

    public void check() {
        synchronized (mLock) {
            if (mThreadId == -1) throw new IllegalStateException("Not inited");
            if (mThreadId != Thread.currentThread().getId()) throw new IllegalStateException("Called on wrong thread, should be " + mThreadName);
        }
    }

    public void deinit(boolean check) {
        synchronized (mLock) {
            if (check && mThreadId == -1) throw new IllegalStateException("Not inited");
            if (mThreadId != Thread.currentThread().getId()) throw new IllegalStateException("Called on wrong thread, should be " + mThreadName);
            mThreadId = -1;
            mThreadName = null;
        }
    }

}
