package net.sileht.lullaby;

/* Copyright (c) 20010 ABAAKOUK Mehdi  <theli48@gmail.com>
 *
 * +------------------------------------------------------------------------+
 * | This program is free software; you can redistribute it and/or          |
 * | modify it under the terms of the GNU General Public License            |
 * | as published by the Free Software Foundation; either version 2         |
 * | of the License, or (at your option) any later version.                 |
 * |                                                                        |
 * | This program is distributed in the hope that it will be useful,        |
 * | but WITHOUT ANY WARRANTY; without even the implied warranty of         |
 * | MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          |
 * | GNU General Public License for more details.                           |
 * |                                                                        |
 * | You should have received a copy of the GNU General Public License      |
 * | along with this program; if not, write to the Free Software            |
 * | Foundation, Inc., 59 Temple Place - Suite 330,                         |
 * | Boston, MA  02111-1307, USA.                                           |
 * +------------------------------------------------------------------------+
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import net.sileht.lullaby.R;

import android.app.Activity;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

public abstract class AmpacheRequest extends Handler {

	protected Activity mCurrentActivity;
	protected String[] mDirective;
	protected int mMax;
	protected boolean mQuick;

	private ArrayList<Object> mCachedData;

	private static boolean mExternalStorageAvailable = false;
	private static boolean mExternalStorageWriteable = false;

	private boolean stop = false;

	private static final String TAG = "LullabyBackendRequest";

	public AmpacheRequest(Activity activity, String[] directive) {
		this(activity, directive, false);
	}

	public AmpacheRequest(Activity activity, String[] directive, Boolean quick) {
		this(activity, directive, quick, true);
	}

	public AmpacheRequest(Activity activity, String[] directive, Boolean quick,
			boolean useCache) {
		super();
		mCurrentActivity = activity;
		mDirective = directive;
		mQuick = quick;
		mCachedData = new ArrayList<Object>();
	}

	public void stop() {
		stop = true;
	}

	private String getCacheFilePath() {
		String filename = mDirective[0];
		if (!mDirective[1].equals("")) {
			filename += "-" + mDirective[1];
		}
		File f = new File(Environment.getExternalStorageDirectory(),
				"Android/data/com.sileht.lullaby/cache");
		f.mkdirs();
		return f.getPath() + "/"  + filename;
	}
	
	static private void checkStorage() {
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			AmpacheRequest.mExternalStorageAvailable = AmpacheRequest.mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			AmpacheRequest.mExternalStorageAvailable = true;
			AmpacheRequest.mExternalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need
			// to know is we can neither read nor write
			AmpacheRequest.mExternalStorageAvailable = AmpacheRequest.mExternalStorageWriteable = false;
		}
	}

	private boolean writeCache() {
		
		checkStorage();
		if (!mExternalStorageWriteable) {
			return false;
		}
		
		Log.d(TAG, "Writing cache start...");

		String cacheFilePath = getCacheFilePath();

		FileOutputStream fos;
		try {
			fos = new FileOutputStream(cacheFilePath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}

		Log.d(TAG, "Writing cache...");
		try {
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(mCachedData);
			oos.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
			(new File(cacheFilePath)).delete();
			return false;
		}
		Log.d(TAG, "Writing cache done");
		return true;
	}

	@SuppressWarnings("unchecked")
	private boolean readCache() {

		checkStorage();
		if (!mExternalStorageAvailable) {
			return false;
		}

		Log.d(TAG, "Reading cache start...");
		
		mCachedData = new ArrayList<Object>();
		String cacheFilePath = getCacheFilePath();
		FileInputStream fis;

		try {
			fis = new FileInputStream(cacheFilePath);
		} catch (FileNotFoundException e) {
			Log.d(TAG, "Reading cache not exists.");
			return false;
		}
		
		Log.d(TAG, "Reading cache...");
		try {
			ObjectInputStream ois = new ObjectInputStream(fis);
			mCachedData = (ArrayList<Object>) ois.readObject();
			ois.close();
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
			(new File(cacheFilePath)).delete();
			return false;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			(new File(cacheFilePath)).delete();
			return false;
		}
		Log.d(TAG, "Reading cache done.");
		return true;
	}

	public void send() {
		if (readCache()) {
			add_objects(mCachedData);
		} else {
			Message requestMsg = new Message();
			requestMsg.what = 1;
			requestMsg.arg1 = 0;
			requestMsg.obj = mDirective;
			requestMsg.replyTo = new Messenger(this);
			Log.d(TAG, "Request send for " + mDirective[0]);
			if (mQuick) {
				Lullaby.comm.incomingRequestHandler
						.sendMessageAtFrontOfQueue(requestMsg);
			} else {
				Lullaby.comm.incomingRequestHandler.sendMessage(requestMsg);
			}
			showProgress();
		}
	}

	public abstract void add_objects(ArrayList<?> list);

	private void hideProgress() {
		if (mCurrentActivity != null) {
			ProgressBar p = (ProgressBar) mCurrentActivity
					.findViewById(R.id.progress);
			p.setVisibility(View.INVISIBLE);
		}
	}

	private void showProgress() {
		if (mCurrentActivity != null) {
			ProgressBar p = (ProgressBar) mCurrentActivity
					.findViewById(R.id.progress);
			p.setVisibility(View.VISIBLE);
		}
	}

	@SuppressWarnings("unchecked")
	public void handleMessage(Message msg) {
		if (stop)
			return;
		switch (msg.what) {
		case 1:
			mCachedData.addAll((ArrayList<Object>) msg.obj);
			add_objects((ArrayList<?>) msg.obj);

			/* queue up the next inc */
			if (((ArrayList<Object>) msg.obj).size() >= 100) {
				Message requestMsg = new Message();
				requestMsg.obj = mDirective;
				requestMsg.what = 1;
				requestMsg.arg1 = msg.arg1 + 100;
				requestMsg.replyTo = new Messenger(this);
				Lullaby.comm.incomingRequestHandler.sendMessage(requestMsg);

				showProgress();
			} else {
				writeCache();
				hideProgress();
			}
			break;
		case 100:
			hideProgress();
			if (mCurrentActivity != null) {
				Toast.makeText(mCurrentActivity,
						"Ampache not configured.\nCheck your settings.",
						Toast.LENGTH_LONG).show();
			}
			Message requestMsg = new Message();
			requestMsg.obj = mDirective;
			requestMsg.what = 1;
			requestMsg.arg1 = msg.arg1;
			requestMsg.replyTo = new Messenger(this);
			Lullaby.comm.incomingRequestHandler.sendMessage(requestMsg);
			break;
		case 9999:
			/* handle an error */
			if (mCurrentActivity != null) {
				Toast.makeText(mCurrentActivity,
						"Communicator error:" + (String) msg.obj,
						Toast.LENGTH_LONG).show();
			}
			break;
		}
	}
}
