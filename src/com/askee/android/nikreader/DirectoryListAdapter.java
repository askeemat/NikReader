package com.askee.android.nikreader;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

class DirectoryData {
	public DirectoryData(String dirName, int numContent) {
		mDirName = dirName;
		mNumContent = numContent;
	}
	public String mDirName;
	public int mNumContent;
}

public class DirectoryListAdapter extends ArrayAdapter<DirectoryData>{
	private Context mContext;
	
	public DirectoryListAdapter(Context context, int resource) {
		super(context, resource);
		mContext = context;
	}

	@Override
	public View getView (int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (convertView == null) {
			convertView = (LinearLayout)inflater.inflate(R.layout.directory_list, null);
		};
		// Get text color.
		int color = Color.BLACK;
		if (this.isEnabled(position) == false) {
			color = Color.GRAY;
		}
		
		
		
		DirectoryData data = this.getItem(position);
		TextView textDirName = (TextView)convertView.findViewById(R.id.textDirName);
		textDirName.setText(data.mDirName);
		textDirName.setTextColor(color);
		
		TextView textNumContent = (TextView)convertView.findViewById(R.id.textNumContent);
		textNumContent.setText(String.valueOf(data.mNumContent) + " contents");
		textNumContent.setTextColor(color);
		
		return convertView;
	}
	
	@Override
	public boolean isEnabled(int position) {
		if (this.getItem(position).mNumContent == 0) {
			return false;
		}
		return true;
	}
	
}
