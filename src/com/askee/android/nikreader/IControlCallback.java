package com.askee.android.nikreader;

public interface IControlCallback {
	public void swipeLeftToRight();
	public void swipeRightToLeft();
	public void movePage(int pageNum);
	public void pinchout();
	public int getCurretPage();
	public int getWholePageNum();
}
