package com.askee.android.nikreader;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;

public class ReaderActivity extends Activity {
	
	private class BundleKey {
		static final String PAGE_NUM = "pageNum";
	}

	private static final String TAG = MainActivity.TAG;
	private String mTargetPath;
	
	// UI component
	//private ReaderView mReaderView;
	private ReaderView mReaderView;
	private ThumbnailView mThumbnailView;
	private IContentView mContentView;
	private GestureControlView mGestureControlView;
	private ControlPanel mControlPanel;
	private IControlCallback mReaderCallback;
	private IThumbnailCallback mThumbnailCallback;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Avoid to display OSK automatically when EditBox is focused.
		getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		
		// Divisible Title bar and status bar.
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.activity_reader);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		
		// Get intent data.
		Intent intent = getIntent();
		mTargetPath = intent.getExtras().getString(ChooseDirectoryActivity.IntentKey.DIR_PATH);
		Log.i(TAG, "mTargetPath = " + mTargetPath);
		
		// Get PDF file list.
		String[] files = new File(mTargetPath).list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				if (filename.toLowerCase(Locale.ENGLISH).endsWith(".pdf")) { 
					return true;
				}
				return false;
			}
		});
		
		if (files == null) {
			return;
		}
		
		List<String> fileList = new ArrayList<String>();
		for (String f : files) {
			String path = mTargetPath + "/" + f;
			File file = new File(path);
			if (file.canRead() == true) {
				fileList.add(path);
			}
		}
		Collections.sort(fileList);
		
		// Register callback to use communication between ReaderView and UI operation.
		mReaderCallback = new IControlCallback() {

			@Override
			public void swipeLeftToRight() {
				mContentView.gotoPrevPage();
				mControlPanel.updatePageInfo();
			}

			@Override
			public void swipeRightToLeft() {
				mContentView.gotoNextPage();
				mControlPanel.updatePageInfo();
			}

			@Override
			public void movePage(int pageNum) {
				mContentView.movePage(pageNum);
				mControlPanel.updatePageInfo();
			}

			@Override
			public int getCurretPage() {
				return mContentView.getEntirePagePos();
			}

			@Override
			public int getWholePageNum() {
				return mContentView.getEntirePageNum();
			}

			@Override
			public void pinchout() {
				mThumbnailView.setVisibility(View.VISIBLE);
				mReaderView.setVisibility(View.INVISIBLE);
				mContentView = mThumbnailView;
				// Take over ReaderView's page number to ThumbnailView.
				mThumbnailView.movePage(mReaderView.getEntirePagePos());
			}
			
		};
		
		mThumbnailCallback = new IThumbnailCallback() {

			@Override
			public void onPageSelected(int page) {
				Log.i(TAG, "onPageSelected : page=" + page);
				mThumbnailView.setVisibility(View.INVISIBLE);
				mReaderView.setVisibility(View.VISIBLE);
				mContentView = mReaderView;
				// Take over ThumbnailView's page number to ReaderView.
				mReaderView.movePage(mThumbnailView.getEntirePagePos());
			}
			
		};
		
		
		FrameLayout baseFrame = (FrameLayout)this.findViewById(R.id.reader_frame_layout_base);
		
		// Set ReaderView.
		//mReaderView = new ReaderView(this, fileList);
		mReaderView = new ReaderView(this, fileList);
		baseFrame.addView(mReaderView);
		mReaderView.setVisibility(View.VISIBLE);
		
		// Set thumbnailView/
		mThumbnailView = new ThumbnailView(this, fileList);
		baseFrame.addView(mThumbnailView);
		mThumbnailView.setVisibility(View.INVISIBLE);
		
		// default value of mContentView is ReaderView.
		mContentView = mReaderView;
		
		// Set GestureControlView.
		mGestureControlView = new GestureControlView(this, mReaderCallback);
		baseFrame.addView(mGestureControlView);
		
		// Set Option menu layer.
		mControlPanel = new ControlPanel(this, mReaderCallback);
		baseFrame.addView(mControlPanel.getControlPanel());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.reader, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		// Turn off back light.
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
	
	@Override
	protected void onResume() 
	{
		super.onResume();
		// Back light keeps on.
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
	
	@Override  
	protected void onSaveInstanceState(Bundle outState) {  
		super.onSaveInstanceState(outState);
		int pageNum = mReaderView.getEntirePagePos();
		outState.putInt(BundleKey.PAGE_NUM, pageNum);
	}  
	  
	@Override  
	protected void onRestoreInstanceState(Bundle savedInstanceState) {  
		super.onRestoreInstanceState(savedInstanceState);
		int pageNum = savedInstanceState.getInt(BundleKey.PAGE_NUM);
		assert mReaderView != null : Log.e(TAG, "mReaderView is null");
		mReaderView.movePage(pageNum - 1);
	}  

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_reader,
					container, false);
			return rootView;
		}
	}

}
