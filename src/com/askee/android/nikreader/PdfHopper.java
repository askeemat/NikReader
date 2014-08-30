package com.askee.android.nikreader;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.Log;

import com.artifex.mupdfdemo.MuPDFCore;
import com.artifex.mupdfdemo.OutlineItem;

public class PdfHopper implements IReader {

	private static final String TAG = MainActivity.TAG;
	
	protected List<MuPDFCore> mCoreList;
	private List<String> mPdfFiles;
	private int mEntirePagePos;
	private int mEntirePageNum;
	protected IReaderCallback mCallback;
	private Context mContext;
	
	public PdfHopper(Context context, List<String> files) {
		mCoreList = new ArrayList<MuPDFCore>();
		mEntirePagePos = 0;
		mEntirePageNum = 0;
		mContext = context;
		openFiles(files);
	}
	
	private boolean openFiles(List<String> files) {
		mPdfFiles = files;
		for (String file : mPdfFiles) {
			try {
				MuPDFCore core = new MuPDFCore(mContext, file);
				mEntirePageNum += core.countPages();
				mCoreList.add(core);
				if (core.hasOutline() == true) {
					Log.i(TAG, "Outline found");
					OutlineItem[] items = core.getOutline();
					if (items != null) {
						for (OutlineItem item : items) {
							Log.i(TAG, "number of items=" + String.valueOf(items.length));
							Log.i(TAG, "title=" + item.title + ", page=" + 
									String.valueOf(item.page) + ", level=" + String.valueOf(item.level));
						}
					}
				}
				else {
					Log.i(TAG, "Outline not found.");
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return true;
	}
	
	
	@Override
	public boolean movePage(int absPageNum) {
		if (0 <= absPageNum && absPageNum < mEntirePageNum) {
			mEntirePagePos = absPageNum;
			return true;
		}
		return false;
	}
	
	@Override
	public boolean gotoNextPage() {
		return movePage(mEntirePagePos + 1);
	}
	
	@Override
	public boolean gotoPrevPage() {
		return movePage(mEntirePagePos - 1);
	}
	
	protected class PdfPosition {
		int mPdfNum;
		int mPage;
		PdfPosition(int pdfNum, int page) {
			mPdfNum = pdfNum;
			mPage = page;
		}
		@Override
		public String toString() {
			return ("CurrentPosition : PDF number=" +
					String.valueOf(mPdfNum) + ", Page number=" +
					String.valueOf(mPage));
		}
	}
	
	public PdfPosition getCurrentPdfPos() {
		return convertPageNumToPos(mEntirePagePos);
		
	}
	
	protected PdfPosition convertPageNumToPos(int pagePos) {
		int count = 0;
		for (MuPDFCore core : mCoreList) {
			int subtPageNum = pagePos - core.countPages();
			if (subtPageNum < 0) {
				break;
			}
			else {
				pagePos = subtPageNum;
			}
			count++;
		}
		return new PdfPosition(count, pagePos);

	}

	@Override
	public Bitmap getCurrentPage() {
		int pageWidth, pageHeight;
		
		PdfPosition pos = getCurrentPdfPos();
		MuPDFCore currCore = mCoreList.get(pos.mPdfNum);
		PointF pageSize = currCore.getPageSize(pos.mPage);
		pageWidth = (int)pageSize.x;
		pageHeight = (int)pageSize.y;
		Bitmap bmp = currCore.drawPage(pos.mPage, pageWidth, pageHeight, 0, 0, pageWidth, pageHeight);
		mCallback.onCurrentPageAvailable();
		return bmp;
	}
	

	@Override
	public int getEntirePageNum() {
		return this.mEntirePageNum;
	}
	

	@Override
	public int getEntirePagePos() {
		return this.mEntirePagePos + 1;
	}


	@Override
	public void setCallback(IReaderCallback callback) {
		mCallback = callback;
	}


	@Override
	public Bitmap getThumbnail(int page) {
		// TODO Implement this method.
		Log.i(TAG, "getThumbnail is not supported");
		return null;
	}
}
