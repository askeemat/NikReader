package com.askee.android.nikreader;

import android.graphics.Bitmap;

public interface IReader {

	public abstract boolean movePage(int absPageNum);

	public abstract boolean gotoNextPage();

	public abstract boolean gotoPrevPage();

	public abstract int getEntirePageNum();

	public abstract int getEntirePagePos();
	
	public abstract Bitmap getCurrentPage();
	
	public abstract Bitmap getThumbnail(int page);

	public abstract void setCallback(IReaderCallback callback);
}