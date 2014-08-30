package com.askee.android.nikreader;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.Log;

import com.artifex.mupdfdemo.MuPDFCore;


public class PdfHopperPreRender extends PdfHopper {

	private static final String TAG = MainActivity.TAG;
	private static final int MIN_RANGE_CACHE = -4;
	private static final int MAX_RANGE_CACHE = 4;

	private AtomicBoolean mIsThreadWork;
	private boolean mStopRendering;
	private List<PageBmpInfo> mPageList;
	private LinkedList<PageBmpInfo> mRenderQueue;
	private Timer mTimer;
	
	public PdfHopperPreRender(Context context, List<String> files) {
		super(context, files);
		mStopRendering = false;
		mIsThreadWork = new AtomicBoolean(false);
		
		mPageList = new ArrayList<PageBmpInfo>();
		mRenderQueue = new LinkedList<PageBmpInfo>();
		mTimer = new Timer();
	}

	private class PageBmpInfo {
		private Bitmap mPageBmp;
		private int mPageNumber;
		
		@SuppressWarnings("unused")
		PageBmpInfo(Bitmap bmp, int pageNum) {
			mPageBmp = bmp;
			mPageNumber = pageNum;
		}
		
		PageBmpInfo(int pageNum) {
			mPageNumber = pageNum;
		}
		
		public Bitmap getBitmap() {
			return mPageBmp;
		}
		public void setBitmap(Bitmap bmp) {
			mPageBmp = bmp;
		}

		public int getPageNumber() {
			return mPageNumber;
		}
	}
	
	private boolean startBackgroundThread(long delay) {
		Log.i(TAG, "Start startBackgroundThread()");
		if (mIsThreadWork.get() == false) {
			mIsThreadWork.set(true);
			mTimer.schedule(new TimerTask() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					threadFunc();
					mIsThreadWork.set(false);
					Log.i(TAG, "Background task finished");
				}
				
			}, 	delay);
			return true;
		}
		else {
			Log.i(TAG,"Background thread is already scheduled.");
		}
		return false;
	}

	
	/**
	 * discardUnusedPageCache()
	 * This method is used to remove old unused page cache.
	 * This method should be called only from getCurrentPage().
	 */
	private void discardUnusedPageCache() {
		int currPage = this.getEntirePagePos();
		int minPageRange = currPage + MIN_RANGE_CACHE;
		if (minPageRange < 0) {
			minPageRange = 0;
		}
		int maxPageRange = currPage + MAX_RANGE_CACHE;
		if (maxPageRange >= this.getEntirePageNum()) {
			maxPageRange = this.getEntirePageNum() - 1;
		}
		
		for (int i = 0; i < mPageList.size(); i++) {
			PageBmpInfo page = mPageList.get(i);
			if (page.getPageNumber() < minPageRange || maxPageRange < page.getPageNumber()) {
				page.getBitmap().recycle();
				mPageList.remove(page);
			}
		}
	}
	
	
	/**
	 * Generate bitmap data of specified page.
	 * This method should be called only from rendering thread(threadLoop).
	 * parameter page should be set page number and this method generates 
	 * the page image corresponding to specified page number.
	 * @param page the page number in currently open PDF files.
	 */
	private boolean getPageImage(PageBmpInfo page) {
		Log.i(TAG, "Start getPageImage() : page number=" + page.getPageNumber());
		for (PageBmpInfo p : mPageList) {
			if (page.getPageNumber() == p.getPageNumber()) {
				return false;
			}
		}
		
		PdfPosition pos = this.convertPageNumToPos(page.getPageNumber());
		MuPDFCore currCore = mCoreList.get(pos.mPdfNum);
		PointF pageSize = currCore.getPageSize(pos.mPage);
		// Start rendering.
		Bitmap bmp = currCore.drawPage(pos.mPage, (int)pageSize.x, (int)pageSize.y, 0, 0, (int)pageSize.x, (int)pageSize.y);
		page.setBitmap(bmp);
		return true;
	}
	
	
	/**
	 * Register page information to rendering result buffer.
	 * This method should be called only from threadLoop().
	 * @param page
	 */
	private void registerPageList(PageBmpInfo page) {
		for (PageBmpInfo p : mPageList) {
			if (page.getPageNumber() == p.getPageNumber()) {
				p.getBitmap().recycle();
				mPageList.remove(p);
				mPageList.add(page);
				return;
			}
		}
		mPageList.add(page);
	}
	
	/**
	 * Schedule rendering task.
	 * This method should be called only from getCurrentPage() context.
	 * @param page
	 */
	private void addRenderTask(PageBmpInfo page) {
		for (PageBmpInfo p : mPageList) {
			if (page.getPageNumber() == p.getPageNumber()) {
				return;
			}
		}
		Log.i(TAG, "addRenderTask : page=" + page.getPageNumber());
		mRenderQueue.offerLast(page);
	}
	
	/**
	 * Schedule rendering task with most high priority.
	 * This method should be called only from getCurrentPage() context.
	 * @param page
	 */
	@SuppressWarnings("unused")
	private void interruptAddRenderTask(PageBmpInfo page) {
		Log.i(TAG, "interruptAddRenderTask : page=" + page.getPageNumber());
		mRenderQueue.offerFirst(page);
	}
	
	private void schedulePageGeneration() {
		int currPage = this.getEntirePagePos();
		int minRangePage = currPage + MIN_RANGE_CACHE;
		if (minRangePage < 0) {
			minRangePage = 0;
		}
		int maxRangePage = currPage + MAX_RANGE_CACHE;
		if (maxRangePage >= this.getEntirePageNum()) {
			maxRangePage = this.getEntirePageNum() - 1;
		}
		for (int i = currPage; i <= maxRangePage; i++) {
			addRenderTask(new PageBmpInfo(i));
		}
		for (int i = currPage - 1; i >= minRangePage; i--) {
			addRenderTask(new PageBmpInfo(i));
		}
	}
	
	
	@SuppressWarnings("unused")
	private void clearRenderQueue() {
		mRenderQueue.clear();
	}
	
	
	public synchronized boolean isCurrentPageAvailable() {
		for (PageBmpInfo p : mPageList) {
			if (p.getPageNumber() == this.getEntirePagePos() &&
					p.getBitmap() != null) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public synchronized Bitmap getCurrentPage() {
		PageBmpInfo page = null;
		StringBuffer sb = new StringBuffer();
		sb.append("Start getCurrentPage() : available page=");
		for (PageBmpInfo p : mPageList) {
			sb.append(p.getPageNumber());
			sb.append(", ");
		}
		Log.i(TAG, sb.toString());
		
		// Suspend current background rendering.
		//stopRendering();
		// Search page.
		for (PageBmpInfo p : mPageList) {
			if (p.getPageNumber() == this.getEntirePagePos()) {
				page = p;
				break;
			}
		}
		// Release unused page.
		discardUnusedPageCache();
		
		if (page != null) {
			Log.i(TAG, "Page cache available.");
			schedulePageGeneration();
			startBackgroundThread(3000);
			return page.getBitmap();
		}
		else {
			Log.i(TAG, "Page not found.");
			this.addRenderTask(new PageBmpInfo(this.getEntirePagePos()));
			schedulePageGeneration();
			startBackgroundThread(0);
			return null;
		}
	
	}
	
	@Override
	public Bitmap getThumbnail(int page) {
		synchronized(this) {
			PageBmpInfo pageBmpInfo = null;
			// Search the page user want and this is found in the page cache,
			// this method return this bitmap.
			for (PageBmpInfo p : mPageList) {
				if (p.getPageNumber() == page) {
					pageBmpInfo = p;
					break;
				}
			}
			if (pageBmpInfo != null) {
				return pageBmpInfo.getBitmap();
			}
			else {
				PdfPosition pos = this.convertPageNumToPos(page);
				MuPDFCore currCore = mCoreList.get(pos.mPdfNum);
				PointF pageSize = currCore.getPageSize(pos.mPage);
				return currCore.drawPage(pos.mPage, (int)pageSize.x / 4, (int)pageSize.y / 4, 0, 0, 
						(int)pageSize.x / 4, (int)pageSize.y / 4);
			}
		}
	}
	
	@SuppressWarnings("unused")
	private void stopRendering() {
		Log.i(TAG, "Start stopRendering()");
		if (mIsThreadWork.get() == true) {
			mStopRendering = true;
		}
		else {
			mStopRendering = false;
		}
	}
	
	private void threadFunc() {
		Log.i(TAG, "Start threadFunc()");
		mStopRendering = false;
		while (true) {
			synchronized(this) {
				if (mStopRendering == true) {
					mStopRendering = false;
					Log.i(TAG, "Stop ackground thread.");
					return;
				}
				PageBmpInfo page = mRenderQueue.poll();
				if (page != null) {
					if (getPageImage(page) == true) {
						registerPageList(page);
						if (page.getPageNumber() == this.getEntirePagePos()) {
							if (mCallback != null) {
								// Notify the completion of page rendering.
								mCallback.onCurrentPageAvailable();
								// Wait 2000msec to prompt drawing the page.
								try {
									Thread.sleep(2000);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					}
				}
				else {
					return;
				}
			}
			// Sleep this thread and we expect that context switch happens.
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
