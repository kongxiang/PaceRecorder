/**
 * 
 */
package com.motorola.e13385.PaceRecorder;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

/**
 * @author e13385
 * 
 */
public class PaceRecorderService extends Service {

	private static final String TAG = "PaceRecorderService";
	private RemoteCallbackList<ICallback> mCallbacks = new RemoteCallbackList<ICallback>();
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private MySensorEventListener mSensorEventListener;
	private PowerManager.WakeLock mWakeLock;

	private int mMajorCounter = 0;
	private float mAccValue = 0;
	private float mLastAccValue = 0;

	class MySensorEventListener implements SensorEventListener {

		public void onAccuracyChanged(Sensor arg0, int arg1) {
		}

		public void onSensorChanged(SensorEvent arg0) {
			mAccValue = (float) Math
					.sqrt((Math.pow(arg0.values[0], 2)
							+ Math.pow(arg0.values[1], 2) + Math.pow(
							arg0.values[2], 2)));
			// String value = arg0.values[0] + " " + arg0.values[1] + " " +
			// arg0.values[2];
			if (mAccValue >= 11 && mLastAccValue < 11) {
				mMajorCounter = mMajorCounter + 1;
			}
			mLastAccValue = mAccValue;
			refresh();
		}
	}

	public class LocalBinder extends Binder {
		PaceRecorderService getService() {
			return PaceRecorderService.this;
		}
	}

	@Override
	public void onCreate() {
		Log.v(TAG, "onCreate()");
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mAccelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "onDestroy");
		mCallbacks.kill();
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		Log.v(TAG, "onBind()");
		return new PaceRecorderServiceImpl();
	}
	
	public void refresh() {
		// Broadcast to all clients the new value.
		final int N = mCallbacks.beginBroadcast();
		for (int i = 0; i < N; i++) {
			try {
				Log.v(TAG, "Noted");
				mCallbacks.getBroadcastItem(i).valueChanged(mMajorCounter, mAccValue);
			} catch (RemoteException e) {
			}
		}
		mCallbacks.finishBroadcast();
	}
	
	public class PaceRecorderServiceImpl extends IPaceRecorderService.Stub {


		public void startCount() throws RemoteException {
			if (mSensorEventListener == null) {
				mSensorEventListener = new MySensorEventListener();
				mSensorManager.registerListener(mSensorEventListener,
						mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
				mWakeLock.acquire();
			}
		}

		public void stopCount() throws RemoteException {
			if (mSensorEventListener != null) {
				mSensorManager.unregisterListener(mSensorEventListener);
				mSensorEventListener = null;
				mWakeLock.release();
			}
		}

		public boolean isCounting() throws RemoteException {
			refresh();
			return mSensorEventListener != null;
		}

		public void registerCallback(ICallback cb) throws RemoteException {
			if (cb != null) mCallbacks.register(cb);
		}

		public void unregisterCallback(ICallback cb) throws RemoteException {
			if (cb != null) mCallbacks.unregister(cb);
		}

		public void clearCount() throws RemoteException {
			refresh();
			mMajorCounter = 0;
			mLastAccValue = 0;
		}
	
	}
}
