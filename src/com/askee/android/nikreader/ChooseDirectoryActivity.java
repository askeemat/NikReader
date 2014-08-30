package com.askee.android.nikreader;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

public class ChooseDirectoryActivity extends Activity {

	private static final String TAG = MainActivity.TAG;
	
	private final String mExternalSdPath;	
	private ListView mListViewDirectory;
	private List<DirectoryData> mDirectoryList;
	
	public final class IntentKey {
		public static final String DIR_PATH = "directory_path";
	}
	
	public ChooseDirectoryActivity() {
		super();
		mExternalSdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_choose_directory);
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		
		DirectoryListAdapter adapter = new DirectoryListAdapter(this, 0);
		mDirectoryList = getDirectoryList();
		if (mDirectoryList != null) {
			adapter.addAll(mDirectoryList);
		}
		mListViewDirectory = (ListView)this.findViewById(R.id.list_directory);
		mListViewDirectory.setAdapter(adapter);
		mListViewDirectory.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				onDirectoryListItemClick(parent, view, position, id);
			}
		});
		
		
	}
	
	private void onDirectoryListItemClick(AdapterView<?> parent, View view,
			int position, long id) {
		Log.i(TAG, "onDirectoryListItemClick");
		DirectoryData data = mDirectoryList.get(position);
		Intent intent = new Intent(this.getApplicationContext(), ReaderActivity.class);
		String dirPath = mExternalSdPath + "/" + data.mDirName;
		intent.putExtra(IntentKey.DIR_PATH, dirPath);
		this.startActivity(intent);
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.choose_directory, menu);
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

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(
					R.layout.fragment_choose_directory, container, false);
			return rootView;
		}
	}
	
	private final List<DirectoryData> getDirectoryList() {
		List<DirectoryData> list = new ArrayList<DirectoryData>();
		File sdDir = new File(this.mExternalSdPath);
		if (!(sdDir.canExecute() && sdDir.canRead())) {
			Toast.makeText(this, "Cannot access to SD card.", Toast.LENGTH_SHORT).show();
			return null;
		}
		Log.i(TAG, "sdDir = " + sdDir.toString());
		for (File f : sdDir.listFiles()) {
			if (f.isDirectory() == true && f.canExecute() == true && f.canRead() == true) {
				String[] dirNameList = f.list(new FilenameFilter() {

					@Override
					public boolean accept(File dir, String filename) {
						if (filename.toLowerCase(Locale.ENGLISH).endsWith(".pdf")) { 
							return true;
						}
						return false;
					}
					
				});
				int numContent = dirNameList.length;
				list.add(new DirectoryData(f.getName(), numContent));
			}
		}
		return list;
	}

}
