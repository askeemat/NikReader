package com.askee.android.nikreader;

import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

/**
 * Reader class represents UI component of PDF reader
 */
public class ThumbnailView extends FrameLayout implements IReaderCallback, IContentView {
	private static final String TAG = MainActivity.TAG;
	private static final int WAIT_DIALOG_DELAY = 2000;
	private static final int NUM_THUMBNAIL = 4;
	
	private IReader mReader = null;
	private Context mContext;
	private boolean mAsyncTaskWork = false;
	private ImageView[] mThumbnailView;
	private Bitmap[] mThumbnailBitmap;
	private IThumbnailCallback mThumbnailCallback;
	
	public ThumbnailView(Context context, List<String> fileList) {
		super(context);
		Log.i(TAG, "Constructor : ThumbnailView!");
		mContext = context;
		setFileList(fileList);

		View thumbLayout = LayoutInflater.from(mContext).inflate(R.layout.thumbnail_layout, null);
		this.addView(thumbLayout);
		this.addView(new BorderView(mContext));
		
		// Add thumbnail view.
		mThumbnailView = new ImageView[NUM_THUMBNAIL];
		mThumbnailView[0] = (ImageView)thumbLayout.findViewById(R.id.thumb_page1);
		mThumbnailView[1] = (ImageView)thumbLayout.findViewById(R.id.thumb_page2);
		mThumbnailView[2] = (ImageView)thumbLayout.findViewById(R.id.thumb_page3);
		mThumbnailView[3] = (ImageView)thumbLayout.findViewById(R.id.thumb_page4);
		
		// Register touch event listener for each ThumbnailView.
		OnTouchListener thumbnailOnTouchListener = new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Log.i(TAG, "Touch thumbnail");
				for (int i = 0; i < NUM_THUMBNAIL; i++) {
					if (mThumbnailView[i] == v) {
						ThumbnailView.this.mThumbnailCallback.onPageSelected(mReader.getEntirePagePos() + i);
						return true;
					}
				}
				return false;
			}
		};
		for (ImageView tv : mThumbnailView) {
			tv.setOnTouchListener(thumbnailOnTouchListener);
			tv.setScaleType(ScaleType.FIT_CENTER);
		}
		
		mThumbnailBitmap = new Bitmap[NUM_THUMBNAIL];
		
		// Callback
		mThumbnailCallback = null;
	}
	
	public void setThumbnailCallback(IThumbnailCallback callback) {
		mThumbnailCallback = callback;
	}
	
	
	public void setFileList(List<String> fileList) {
		mReader = new PdfHopperPreRender(mContext, fileList);
		mReader.setCallback(this);
	}
		
	private boolean doDrawTask() {
		if (mAsyncTaskWork == true) {
			return false;
		}
		mAsyncTaskWork = true;
		new AsyncTask<Void, Void, Boolean>() {
			private boolean mPageDisplayed;
			private ProgressDialog mProgressDialog;
			
			@Override
			protected Boolean doInBackground(Void... v) {
				int startPage = (mReader.getEntirePagePos() / NUM_THUMBNAIL) * NUM_THUMBNAIL;
				for (int i = 0; i < NUM_THUMBNAIL; i++) {
					ImageView imgview = mThumbnailView[i];
					mThumbnailBitmap[i] = mReader.getThumbnail(i + startPage + 1);
					Log.i(TAG, "bitmap : width=" + mThumbnailBitmap[i].getWidth() + ", height=" + mThumbnailBitmap[i].getHeight());
				}
				return true;
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
			protected void onPostExecute(Boolean result) {
				synchronized(this) {
					mPageDisplayed = true;
				}
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
				}
				// Dislay thumbnails.
				for (int i = 0; i < NUM_THUMBNAIL; i++) {
					Bitmap bmp = Bitmap.createScaledBitmap(mThumbnailBitmap[i],
							mThumbnailBitmap[i].getWidth() * 4, mThumbnailBitmap[i].getHeight() * 4, false);
					mThumbnailView[i].setImageBitmap(mThumbnailBitmap[i]);
				}
				mAsyncTaskWork = false;
			}
		}.execute();
		return true;
	}
	
	
	@Override
	protected void onSizeChanged (int w, int h, int oldw, int oldh) {
		doDrawTask();
	}
	
	
	/* (non-Javadoc)
	 * @see com.askee.android.nikreader.IPageControl#movePage(int)
	 */
	@Override
	public boolean movePage(int pageNum) {
		if (mReader.movePage(pageNum) == true) {
			doDrawTask();
			return true;
		}
		return false;
	}

	
	/* (non-Javadoc)
	 * @see com.askee.android.nikreader.IPageControl#gotoNextPage()
	 */
	@Override
	public boolean gotoNextPage() {
		if (mReader.movePage(this.getEntirePagePos() + NUM_THUMBNAIL) == true) {
			doDrawTask();
			return true;
		}
		return false;
	}


	/* (non-Javadoc)
	 * @see com.askee.android.nikreader.IPageControl#gotoPrevPage()
	 */
	@Override
	public boolean gotoPrevPage() {
		if (mReader.movePage(this.getEntirePagePos() - NUM_THUMBNAIL) == true) {
			doDrawTask();
			return true;
		}
		return false;
	}


	/* (non-Javadoc)
	 * @see com.askee.android.nikreader.IPageControl#getEntirePageNum()
	 */
	@Override
	public int getEntirePageNum() {
		return mReader.getEntirePageNum();
	}


	/* (non-Javadoc)
	 * @see com.askee.android.nikreader.IPageControl#getEntirePagePos()
	 */
	@Override
	public int getEntirePagePos() {
		return mReader.getEntirePagePos();
	}

	@Override
	public void onCurrentPageAvailable() {

	}

	private class BorderView extends View {
		private Paint mPen;
		BorderView(Context context) {
			super(context);
			this.setAlpha(1.0f);
			mPen = new Paint();
			mPen.setStyle(Style.STROKE);
			mPen.setPathEffect(new DashPathEffect(new float[]{ 50.0f, 50.0f }, 0));
			mPen.setColor(Color.BLACK);
		}
		@Override
		protected void onDraw(Canvas canvas) {
			float width = (float)canvas.getWidth();
			float height = (float)canvas.getHeight();
			canvas.drawLine(0, height / 2.0f, width - 1.0f, height / 2.0f, mPen);
			canvas.drawLine(width / 2.0f, 0, width / 2.0f, height - 1.0f, mPen);
		}
	}
}
