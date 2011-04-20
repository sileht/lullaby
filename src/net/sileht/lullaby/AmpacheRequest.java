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

import net.sileht.lullaby.objects.Album;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

public abstract class AmpacheRequest extends Handler {

	protected Context mContext;
	protected String[] mDirective;
	protected int mMax;
	protected boolean mQuick;
	protected boolean mUseCache;

	private ArrayList<Object> mCachedData;

	private boolean stop = false;

	private static final String TAG = "LullabyBackendRequest";

	public AmpacheRequest(Context context, String[] directive) {
		this(context, directive, false);
	}

	public AmpacheRequest(Context context, String[] directive, Boolean quick) {
		this(context, directive, quick, true);
	}

	public AmpacheRequest(Context context, String[] directive, Boolean quick,
			boolean useCache) {
		super();
		mContext = context;
		mDirective = directive;
		mQuick = quick;
		mUseCache = useCache;
		mCachedData = new ArrayList<Object>();
	}

	public void stop() {
		stop = true;
	}

	private String getCacheFilePath(String filename) {
		return Utils.getCacheFile(filename).getPath();
	}

	private String getCacheFilePath() {
		String filename = mDirective[0];
		if (!mDirective[1].equals("")) {
			filename += "-" + mDirective[1];
		}
		return getCacheFilePath(filename);
	}
	
	public boolean clearCache(){
		Utils.checkStorage();
		if (!Utils.mExternalStorageWriteable) {
			return false;
		}
		String cacheFilePath = getCacheFilePath();
		(new File(cacheFilePath)).delete();
		return true;
	}
	
	private boolean writeCache() {

		Utils.checkStorage();
		if (!Utils.mExternalStorageWriteable) {
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

	private boolean readCache() {
		boolean ret = false;
		// Quick hack if albums cache if present
		if (mDirective[0] == "artist_albums") {
			ret = readCache("albums");
			if (ret) {
				ArrayList<Object> nlist = new ArrayList<Object>();
				for (Object o : mCachedData) {
					Album a = (Album) o;
					if (a.artist_id.equals(mDirective[1])) {
						nlist.add((Object) a);
					}
				}
				mCachedData = nlist;
				// writeCache();
			} else {
				ret = readCache(null);
			}
		} else {
			ret = readCache(null);
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	private boolean readCache(String filename) {

		Utils.checkStorage();
		if (!Utils.mExternalStorageAvailable) {
			return false;
		}

		Log.d(TAG, "Reading cache start...");

		mCachedData = new ArrayList<Object>();

		String cacheFilePath = null;
		if (filename == null) {
			cacheFilePath = getCacheFilePath();
		} else {
			cacheFilePath = getCacheFilePath(filename);
		}

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
			(new File(cacheFilePath)).delete();
			return false;
		}
		Log.d(TAG, "Reading cache done.");

		return true;
	}

	public void send() {
		if (mUseCache && readCache()) {
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
		if (mContext != null) {
			ProgressBar p = (ProgressBar) ((Activity) mContext)
					.findViewById(R.id.progress);
			p.setVisibility(View.INVISIBLE);
		}
	}

	private void showProgress() {
		if (mContext != null) {
			ProgressBar p = (ProgressBar) ((Activity) mContext)
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
			if (mUseCache) {
				mCachedData.addAll((ArrayList<Object>) msg.obj);
			}
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
				if (mUseCache) {
					writeCache();
				}
				hideProgress();
			}
			break;
		case 100:
			hideProgress();
			if (mContext != null) {
				Toast.makeText(mContext,
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
			if (mContext != null) {
				Toast.makeText(mContext,
						"Communicator error:" + (String) msg.obj,
						Toast.LENGTH_LONG).show();
			}
			break;
		}
	}
}
