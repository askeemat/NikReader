package com.askee.android.nikreader;

public interface IContentView {

	public abstract boolean movePage(int pageNum);

	public abstract boolean gotoNextPage();

	public abstract boolean gotoPrevPage();

	public abstract int getEntirePageNum();

	public abstract int getEntirePagePos();

}