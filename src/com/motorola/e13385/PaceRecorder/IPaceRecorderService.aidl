package com.motorola.e13385.PaceRecorder;

import com.motorola.e13385.PaceRecorder.ICallback;

interface IPaceRecorderService {
    void startCount();
    void stopCount();
    boolean isCounting();
    void clearCount();
    void registerCallback(ICallback cb);
    void unregisterCallback(ICallback cb);
}