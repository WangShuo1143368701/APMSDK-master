package com.huawu.fivesmart.audio.apm.util;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

/**
 * Created by linkzhzhu on 2017/9/12.
 */

public class TXCThread {

    public TXCThread(String name){
        HandlerThread thread = new HandlerThread(name);
        mNewThread = true;
        thread.start();
        mLooper = thread.getLooper();
        mHandler = new Handler(mLooper);
        mThread = thread;
    }

    public TXCThread(Looper looper){
        mHandler = new Handler(looper);
        mLooper = looper;
        mNewThread = false;
    }

    public Handler getHandler(){
        return mHandler;
    }

    public Looper getLooper(){
        return mLooper;
    }

    @Override
    protected void finalize( ) throws Throwable {
        if(mNewThread)mHandler.getLooper().quit();
        super.finalize();
    }

    public void runSync(final Runnable task){
        final boolean[] runFlag = new boolean[1];
        if(Thread.currentThread().equals(mThread)){
            task.run();
        }
        else{
            synchronized (mHandler){
                runFlag[0] = false;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        task.run();
                        runFlag[0] = true;
                        synchronized (mHandler){
                            mHandler.notifyAll();
                        }
                    }
                });
                while(!runFlag[0]){
                    try{
                        mHandler.wait();
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void runAsync(final Runnable task){
        mHandler.post(task);
    }

    public void runAsyncDelay(final Runnable task, long delay){
        mHandler.postDelayed(task, delay);
    }



    private Handler mHandler;
    private Looper mLooper;
    private boolean mNewThread;
    private Thread mThread;
}
