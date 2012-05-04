package com.motorola.e13385.PaceRecorder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class PaceRecorderActivity extends Activity {
	private static final String TAG = "PaceRecorderActivity";
	private Button mStartStopButton;
	private Button mClearButton;
	private TextView mValueView;
	private TextView mCounterView;
	private IPaceRecorderService mService = null;
	private Handler mHandler;
	private boolean isCounting = false;
	
	private static final int BUMP_MSG = 1;
	
    private ICallback mCallback = new ICallback.Stub() {
		public void valueChanged(int CounterValue, float AccValue)
				throws RemoteException {
			mHandler.sendMessage(mHandler.obtainMessage(BUMP_MSG, CounterValue, 0, Float.valueOf(AccValue)));
			
		}
    };


    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            mService = IPaceRecorderService.Stub.asInterface(service);
            mCounterView.setText("Ready");
            try {
                mService.registerCallback(mCallback);
                isCounting = mService.isCounting();
            } catch (RemoteException e) {
            }
    		if (isCounting)
    			mStartStopButton.setText(R.string.button_stop);
    		else
    			mStartStopButton.setText(R.string.button_start);            
        }
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
            mCounterView.setText("Stopped");
        }
    };

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate()");    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mStartStopButton = (Button) findViewById(R.id.button1);
        mClearButton = (Button) findViewById(R.id.button2);
        mValueView = (TextView) findViewById(R.id.textview1);
        mCounterView = (TextView) findViewById(R.id.textview2);
        mHandler = new Handler(){
            @Override 
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case BUMP_MSG:
                        mCounterView.setText(String.valueOf(msg.arg1));
                        Float accValue = (Float) msg.obj;
                        mValueView.setText(accValue.toString());
                        break;
                    default:
                        super.handleMessage(msg);
                }
            }
        };

        startService(new Intent(PaceRecorderActivity.this, PaceRecorderService.class));
		bindService(new Intent(IPaceRecorderService.class.getName()), mConnection, Context.BIND_AUTO_CREATE);

		mStartStopButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.v(TAG, "onClick()");
				mStartStopButton.setEnabled(false);
				try {
					isCounting = mService.isCounting();
					if (isCounting != true) {
						mService.startCount();
						mStartStopButton.setText(R.string.button_stop);
					} else {
						mService.stopCount();
						mStartStopButton.setText(R.string.button_start);

					}
				} catch (RemoteException e) {
				}
				mStartStopButton.setEnabled(true);

			} // onClick()
		}); // new OnClickListener()
		
		mClearButton.setOnClickListener(new OnClickListener() {

			public void onClick(View arg0) {
				try {
					mService.clearCount();
				} catch (RemoteException e) {
				}
				
			} // onClick()
		}); // new OnClickListener()

    } //onCreate()
    
    
    @Override
    public void onDestroy() {
    	Log.v(TAG, "onDestroy()");
    	if (mService != null) {
			try {
				mService.unregisterCallback(mCallback);
			} catch (RemoteException e) {
			}
    		unbindService(mConnection);
    	}
    	super.onDestroy();
    }
}