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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import net.sileht.lullaby.R;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

public abstract class AmpacheRequest extends Handler {

	protected Activity mCurrentActivity;
	protected String[] mDirective;
	protected int mMax;
	protected boolean mQuick;

	private static boolean mExternalStorageAvailable = false;
	private static boolean mExternalStorageWriteable = false;

	private String mFilePrefix = "lullabycache";

	private boolean stop = false;

	private static final String TAG = "LullabyBackendRequest";

	public AmpacheRequest(Activity activity, String[] directive) {
		this(activity, directive, false);
	}

	public AmpacheRequest(Activity activity, String[] directive, Boolean quick) {
		super();
		mCurrentActivity = activity;
		mDirective = directive;
		mQuick = quick;
	}

	public void stop() {
		stop = true;
	}

	public boolean isCached() {
		checkStorage();
		if (!mExternalStorageAvailable) {
			return false;
		}
		return false;
	}

	private byte[] readObjs() {
		FileInputStream fis;
		byte[] b = null;

		try {
			fis = mCurrentActivity.openFileInput(mFilePrefix);
			fis.read(b);
			fis.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return b;

	}

	private boolean writeObjs(byte[] b) {
		if (mCurrentActivity == null) {
			return false;
		}
		FileOutputStream fos;
		try {
			fos = mCurrentActivity.openFileOutput(mFilePrefix,
					Context.MODE_PRIVATE);
			fos.write(b);
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private boolean closeCache() {
		return writeObjs("CLOSEDCACHE".getBytes());
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

	public void send() {
		if (isCached()) {

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

	private void cache_and_add_objects(ArrayList<Parcelable> list) {
		add_objects(list);
		for (Parcelable elem : list) {
			Parcel p = null;
			elem.writeToParcel(p, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
			if (p != null) {
				writeObjs(p.createByteArray());
			}
		}
	}

	public abstract void add_objects(ArrayList<Parcelable> list);

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

	public void handleMessage(Message msg) {
		if (stop)
			return;
		switch (msg.what) {
		case 1:
			cache_and_add_objects((ArrayList<Parcelable>) msg.obj);

			/* queue up the next inc */
			if (((ArrayList<?>) msg.obj).size() >= 100) {
				Message requestMsg = new Message();
				requestMsg.obj = mDirective;
				requestMsg.what = 1;
				requestMsg.arg1 = msg.arg1 + 100;
				requestMsg.replyTo = new Messenger(this);
				Lullaby.comm.incomingRequestHandler.sendMessage(requestMsg);

				showProgress();
			} else {
				closeCache();
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
