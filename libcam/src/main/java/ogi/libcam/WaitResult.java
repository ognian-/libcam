package ogi.libcam;

public class WaitResult {

    private final Object mLock = new Object();
    private boolean mDone = false;
    private Object mResult = null;
    private Exception mException = null;

    public void setResult(Object result) {
        synchronized (mLock) {
            mDone = true;
            mResult = result;
            mLock.notify();
        }
    }

    public void setException(Exception exception) {
        synchronized (mLock) {
            mDone = true;
            mException = exception;
            mLock.notify();
        }
    }

    public Object getResult(boolean reset) throws Exception {
        synchronized (mLock) {
            while (!mDone) mLock.wait();
            try {
                if (mException != null) throw mException;
                return mResult;
            } finally {
                if (reset) {
                    mDone = false;
                    mResult = null;
                    mException = null;
                }
            }
        }
    }

    public Object getResultNoThrows(boolean reset) {
        try {
            return getResult(reset);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
