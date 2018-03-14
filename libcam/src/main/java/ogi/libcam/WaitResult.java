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
            mException = null;
            mLock.notify();
        }
    }

    public void setException(Exception exception) {
        synchronized (mLock) {
            mDone = true;
            mException = exception;
            mResult = null;
            mLock.notify();
        }
    }

    public Object getResult() {
        try {
            synchronized (mLock) {
                while (!mDone) mLock.wait();
                if (mException != null) throw mException;
                return mResult;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
