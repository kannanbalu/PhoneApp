package course.examples.phoneapp;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.content.Context;
import android.util.Log;

/**
 * Listener class to capture the shake gesture of a device and provide an event for performing any operation when triggered <br/>
 * Created by kannanb on 10/1/2015.
 */
public class ShakeListener implements SensorEventListener
{
    private static final int FORCE_THRESHOLD = 350;
    private static final int TIME_THRESHOLD = 100;
    private static final int SHAKE_TIMEOUT = 500;
    private static final int SHAKE_DURATION = 1000;
    private static final int SHAKE_COUNT = 3;

    private SensorManager mSensorMgr;
    private Sensor sensor;
    private float mLastX=-1.0f, mLastY=-1.0f, mLastZ=-1.0f;
    private long mLastTime;
    private OnShakeListener mShakeListener;
    private Context mContext;
    private int mShakeCount = 0;
    private long mLastShake;
    private long mLastForce;

    public static final String LOG_TAG_NAME = "PhoneApp.ShakeListener";

    /**
     * Listener interface for listening to shake event of the device
     */
    public interface OnShakeListener
    {
        /**
         * Shake event to be overriden for performing any desired operation on the shake of the device
         */
        public void onShake();
    }

    /**
     * Constructor to prepare for initializing and absorbing shake event on the device
     * @param context on which the shake event is consumed
     */
    public ShakeListener(Context context)
    {
        mContext = context;
        resume();
    }

    /**
     * Method to set the user defined OnShakeListener
     * @param listener interested in the shake event of the device
     */
    public void setOnShakeListener(OnShakeListener listener)
    {
        mShakeListener = listener;
    }

    /**
     * Method to register the Accerlerometer Sensor to help sense the shake of the device
     */
    public void resume() {
        mSensorMgr = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
        if (mSensorMgr == null) {
            throw new UnsupportedOperationException("Sensors not supported");
        }
        //sensor = mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensor = mSensorMgr.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
        Log.i(LOG_TAG_NAME, "Sensor returned : " + sensor);
        boolean supported = mSensorMgr.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        if (!supported) {
            mSensorMgr.unregisterListener(this, sensor);
            //throw new UnsupportedOperationException("Accelerometer sensor cannot be registered");
        }
    }

    /**
     * Method to unregister the sensor when the activity has been paused. This will save battery power
     */
    public void pause() {
        if (mSensorMgr != null) {
            mSensorMgr.unregisterListener(this, sensor);
            mSensorMgr = null;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    /**
     * Method to perform calculation on how to detect a shake of a device by the user
     * @param event
     */
    public void onSensorChanged(SensorEvent event)
    {
        float [] values = event.values;
        Sensor sensor = event.sensor;
        if (sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;
        long now = System.currentTimeMillis();

        if ((now - mLastForce) > SHAKE_TIMEOUT) {
            mShakeCount = 0;
        }

        if ((now - mLastTime) > TIME_THRESHOLD) {
            long diff = now - mLastTime;
            float speed = Math.abs(values[0] + values[1] + values[2] - mLastX - mLastY - mLastZ) / diff * 10000;
            if (speed > FORCE_THRESHOLD) {
                if ((++mShakeCount >= SHAKE_COUNT) && (now - mLastShake > SHAKE_DURATION)) {
                    mLastShake = now;
                    mShakeCount = 0;
                    if (mShakeListener != null) {
                        mShakeListener.onShake();
                    }
                }
                mLastForce = now;
            }
            mLastTime = now;
            mLastX = values[0];
            mLastY = values[1];
            mLastZ = values[2];
        }
    }

}