package com.askee.android.nikreader;

import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

/**
 * Reader class represents UI component of PDF reader
 */
public class ReaderView extends View implements IReaderCallback, IContentView {
	private static final String TAG = MainActivity.TAG;
	private static final int WAIT_DIALOG_DELAY = 500;
	
	private IReader mReader = null;
	private int mViewWidth = 0, mViewHeight = 0;
	private Bitmap mBitmapRendered = null;
	private Point mDisplayPosition = null;
	private Context mContext;
	private boolean mPageAvailable = false;
	private boolean mAsyncTaskWork = false;
	
	public ReaderView(Context context) {
		super(context);
		mContext = context;
	}
	
	public ReaderView(Context context, List<String> fileList) {
		super(context);
		mContext = context;
		setFileList(fileList);
	}
	
	public void setFileList(List<String> fileList) {
		mReader = new PdfHopperPreRender(mContext, fileList);
		mReader.setCallback(this);
	}
		
	private boolean doDrawTask() {
		if (mAsyncTaskWork == true) {
			Log.i(TAG, "AsyncTask is not available");
			return false;
		}
		mAsyncTaskWork = true;
		new AsyncTask<Void,Void,Bitmap>() {
			private boolean mPageDisplayed;
			private ProgressDialog mProgressDialog;
			
			@Override
			protected Bitmap doInBackground(Void... v) {
				mPageDisplayed = false;
				mPageAvailable = false;
				Bitmap bmp = mReader.getCurrentPage();
				if (bmp != null) {
					Log.i(TAG, "doInBackground() : Page is found");
					return bmp;
				}
				else {
					Log.i(TAG, "doInBackground() : Page not found. Wait...");
					while (mPageAvailable == false);
					// Retry to get page bitmap.
					bmp = mReader.getCurrentPage();
					if (bmp != null) {
						return bmp;
					}
					
				}
				return null;
			}

			@Override
			protected void onPreExecute() {
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						synchronized(this) {
							if (mPageDisplayed == false) {
								mProgressDialog = new ProgressDialog(mContext);
							    mProgressDialog.setTitle("Loading now. \nPlease wait...");
							    mProgressDialog.setIndeterminate(true);
							    mProgressDialog.setCancelable(false);
							    mProgressDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
									@Override
									public boolean onKey(
											DialogInterface dialog,
											int keyCode, KeyEvent event) {
										if (KeyEvent.KEYCODE_SEARCH == keyCode || KeyEvent.KEYCODE_BACK == keyCode) {
											 return true;
										}
										return false;
									}
							    });
							    mProgressDialog.show();
							}
						}
					}
				}, WAIT_DIALOG_DELAY);
			}
			
			@Override
			protected void onCancelled() {
				mAsyncTaskWork = false;
			}

			@Override
			protected void onPostExecute(Bitmap bmp) {
				float scaleRate;
				
				synchronized(this) {
					mPageDisplayed = true;
				}
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
				}
				if (bmp == null) {
					return;
				}
				mDisplayPosition = new Point();
				int bmpWidth = bmp.getWidth();
				int bmpHeight = bmp.getHeight();
				if (((float)bmpWidth / mViewWidth) > ((float)bmpHeight / mViewHeight)) {
					scaleRate = (float)bmpWidth / mViewWidth;
					mDisplayPosition.x = 0;
					mDisplayPosition.y = (mViewHeight - (int)(bmpHeight / scaleRate)) / 2;
				}
				else {
					scaleRate = (float)bmpHeight / mViewHeight;
					mDisplayPosition.x = (mViewWidth - (int)(bmpWidth / scaleRate)) / 2;
					mDisplayPosition.y = 0;
				}
				mBitmapRendered = Bitmap.createScaledBitmap(bmp,
						(int)(bmpWidth / scaleRate) ,(int)(bmpHeight / scaleRate), false);
				
				invalidate();
				mAsyncTaskWork = false;
			}
		}.execute();
		return true;
	}
	
	
	@Override
	protected void onSizeChanged (int w, int h, int oldw, int oldh) {
		mViewWidth = w;
		mViewHeight = h;
		doDrawTask();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		if (mBitmapRendered != null && mDisplayPosition != null) {
			canvas.drawBitmap(mBitmapRendered, mDisplayPosition.x, mDisplayPosition.y, null);
		}
	}
	
	@Override
	public boolean movePage(int pageNum) {
		if (mReader.movePage(pageNum) == true) {
			doDrawTask();
			return true;
		}
		return false;
	}

	@Override
	public boolean gotoNextPage() {
		if (mReader.gotoNextPage() == true) {
			Log.i(TAG, "doDrawTask() will call");
			doDrawTask();
			return true;
		}
		return false;
	}

	@Override
	public boolean gotoPrevPage() {
		if (mReader.gotoPrevPage() == true) {
			doDrawTask();
			return true;
		}
		return false;
	}

	@Override
	public int getEntirePageNum() {
		return mReader.getEntirePageNum();
	}

	@Override
	public int getEntirePagePos() {
		return mReader.getEntirePagePos();
	}

	@Override
	public void onCurrentPageAvailable() {
		Log.i(TAG, "onCurrentPageAvailable");
		mPageAvailable = true;
	}


}
