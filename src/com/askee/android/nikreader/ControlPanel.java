package com.askee.android.nikreader;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class ControlPanel {
	private static final String TAG = MainActivity.TAG;

	private LinearLayout mBaseLayout;
	@SuppressWarnings("unused")
	private Context mContext;
	private IControlCallback mCallback;
	
	// UI component
	ImageButton mLeftButton;
	ImageButton mRightButton;
	SeekBar mPageSeekBar;
	EditText mCurrPageEdit;
	TextView mWholePage;
	
	public ControlPanel(Context context, IControlCallback callback) {
		mContext = context;
		LayoutInflater inflater = LayoutInflater.from(context);
		mBaseLayout = (LinearLayout)inflater.inflate(R.layout.control_panel, null);
		mBaseLayout.setGravity(Gravity.BOTTOM);
		mCallback = callback;
		
		// UI init
		// Left button
		mLeftButton = (ImageButton)mBaseLayout.findViewById(R.id.button_left);
		mLeftButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mCallback.swipeLeftToRight();
			}
		});
		
		// Right button
		mRightButton = (ImageButton)mBaseLayout.findViewById(R.id.button_right);
		mRightButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mCallback.swipeRightToLeft();
			}
		});
		
		// Current page edit text
		mCurrPageEdit = (EditText)mBaseLayout.findViewById(R.id.edit_curr_page);
		mCurrPageEdit.setText(String.valueOf(mCallback.getCurretPage()));
		
		// Whole page text box
		mWholePage = (TextView)mBaseLayout.findViewById(R.id.txt_whole_page);
		mWholePage.setText("/" + String.valueOf(mCallback.getWholePageNum()));
		
		// Seek bar
		mPageSeekBar = (SeekBar)mBaseLayout.findViewById(R.id.seek_page);
		mPageSeekBar.setMax(mCallback.getWholePageNum() - 1);
		mPageSeekBar.setProgress(mCallback.getCurretPage() - 1);
		mPageSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				mCurrPageEdit.setText(String.valueOf(progress + 1));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// Nothing to do.
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				mCallback.movePage(seekBar.getProgress());
				updatePageInfo();
			}
			
		});	
	}
	
	public LinearLayout getControlPanel() {
		return mBaseLayout;
	}
	
	public void updatePageInfo() {
		Log.i(TAG, "updatePageInfo");
		int page = mCallback.getCurretPage();
		mCurrPageEdit.setText(String.valueOf(page));
		mPageSeekBar.setProgress(page - 1);
	}
}
