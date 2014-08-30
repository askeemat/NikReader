package com.askee.android.nikreader;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class GestureControlView extends View {
	private static final String TAG = MainActivity.TAG;
	private static final float SWIPE_INVALID_HEIGHT = 20; // 20mm
	private static final float SWIPE_DETECT_WIDTH = 20; // 20mm
	private static final int SWIPE_MINIMUM_VELOCITY = 100;
	private static final float INCH_MM = 25.4f; // 25.4mm
	private static final float PINCHOUT_DISTANCE = 200; // 200px
	
	private int mSwipeInvalidHeight;
	private int mSwipeDetectWidth;
	private IControlCallback mCallback;
	
	// UI Component
	private GestureDetector mGestureDetector;
	private ScaleGestureDetector mScaleGestureDetector;
	
	private GestureControlView(Context context) {
		super(context);
	}
	
	public GestureControlView(Context context, IControlCallback callback) {
		super(context);
		
		// Calculate parameter;
		int dpi = getResources().getDisplayMetrics().densityDpi;
		Log.i(TAG, "dpi : " + dpi);
		mSwipeInvalidHeight = (int)(SWIPE_INVALID_HEIGHT * ((float)dpi / INCH_MM));
		mSwipeDetectWidth = (int)(SWIPE_DETECT_WIDTH * ((float)dpi / INCH_MM));
		
		// Register callback.
		mCallback = callback;
		
		// UI initialization
		// Set as transparent view.
		this.setBackgroundColor(Color.alpha(0));
		mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
					float velocityY) {
				return handleGesture(e1, e2, velocityX, velocityY);
			}
			
			@Override
            public boolean onDown(MotionEvent e) {
				// This code is needed to call onFling().
				return true;
            }
		});
		
		mScaleGestureDetector = new ScaleGestureDetector(context,
			new ScaleGestureDetector.OnScaleGestureListener() {
				float initDistance;
				@Override
				public void onScaleEnd(ScaleGestureDetector detector) {
					// TODO Auto-generated method stub
					float finalDistance = detector.getCurrentSpan();
					if (initDistance - finalDistance > PINCHOUT_DISTANCE) {
						Log.i(TAG, "pinchout");
						mCallback.pinchout();
					}
					Log.i(TAG, "onScaleEnd");
				}
				
				@Override
				public boolean onScaleBegin(ScaleGestureDetector detector) {
					// TODO Auto-generated method stub
					initDistance = detector.getCurrentSpan();
					Log.i(TAG, "onScaleBegin : distance=" + initDistance);
					return true;
				}
				
				@Override
				public boolean onScale(ScaleGestureDetector detector) {
					// TODO Auto-generated method stub
					Log.i(TAG, "onScale");
					return true;
				}
			});
	}
	
	private boolean handleGesture(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		if ((Math.abs(e1.getY() - e2.getY()) >= mSwipeInvalidHeight) || 
				(Math.abs(e1.getX() - e2.getX()) < mSwipeDetectWidth) ||
				Math.abs(velocityX) < SWIPE_MINIMUM_VELOCITY) {
			return false;
		}
		if (e1.getX() < e2.getX()) {
			if (mCallback != null) {
				mCallback.swipeLeftToRight();
			}
		}
		else {
			if (mCallback != null) {
				mCallback.swipeRightToLeft();
			}
		}
		return true;
	}
	
	public boolean onTouchEvent(MotionEvent event) {
		if (mGestureDetector.onTouchEvent(event) == true) {
			Log.i(TAG, "touch event is handled in GestureDetector");
			return true;
		}
		else if (mScaleGestureDetector.onTouchEvent(event) == true) {
			Log.i(TAG, "touch event is handled in ScaleGestureDetector");
			return true;
		}
		Log.i(TAG, "touch event is not handled");
		return false;
	}
}
