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
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Formatter;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import android.content.Context;
import android.net.SSLCertificateSocketFactory;
import android.os.Environment;
import android.preference.PreferenceManager;

public class Utils {

	public static boolean mExternalStorageAvailable = false;
	public static boolean mExternalStorageWriteable = false;

	private static StringBuilder mFormatBuilder = new StringBuilder();
	private static Formatter mFormatter = new Formatter(mFormatBuilder, Locale
			.getDefault());

	public static File getCacheRootDir() {
		File f = new File(Environment.getExternalStorageDirectory(),
				"Android/data/net.sileht.lullaby/cache");
		f.mkdirs();
		return f;
	}

	public static File getCacheFile(String filename) {
		return new File(getCacheRootDir(), filename);
	}

	public static File getCacheFile(String dir, String filename) {
		File f = new File(getCacheRootDir(), dir);
		f.mkdirs();
		return new File(f, filename);
	}

	public static void checkStorage() {
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need
			// to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
	}

	public static String stringForTime(String timeMs) {
		int time;
		try {
			time = Integer.parseInt(timeMs) * 1000;
		} catch (Exception e) {
			time = 0;
		}
		return stringForTime(time);
	}
	
	public static void setSSLCheck(Context ctx){
		if (PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean("InsecureSSL", true)){
			HttpsURLConnection.setDefaultSSLSocketFactory(SSLCertificateSocketFactory.getInsecure(-1,null));
			HttpsURLConnection.setDefaultHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		} else {
			HttpsURLConnection.setDefaultSSLSocketFactory(SSLCertificateSocketFactory.getDefault(-1,null));
			HttpsURLConnection.setDefaultHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
		}
	}

	public static String stringForTime(int timeMs) {
		int totalSeconds = timeMs / 1000;

		int seconds = totalSeconds % 60;
		int minutes = (totalSeconds / 60) % 60;
		int hours = totalSeconds / 3600;

		mFormatBuilder.setLength(0);
		if (hours > 0) {
			return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds)
					.toString();
		} else {
			return mFormatter.format("%02d:%02d", minutes, seconds).toString();
		}
	}

	public boolean save(Context ctx, String name, Object obj) {
		try {
			FileOutputStream pout = ctx.openFileOutput(name, 0);
			(new ObjectOutputStream(pout)).writeObject(obj);
			pout.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public Object load(Context ctx, String name) {

		try {
			FileInputStream pin = ctx.openFileInput(name);
			Object objs = (new ObjectInputStream(pin)).readObject();
			pin.close();
			return objs;
		} catch (FileNotFoundException e) {
			return null;
		} catch (Exception e) {
			return null;
		}
	}
	
	public static void copy(File src, File dst) throws IOException {
	    InputStream in = new FileInputStream(src);
	    OutputStream out = new FileOutputStream(dst);

	    // Transfer bytes from in to out
	    byte[] buf = new byte[1024];
	    int len;
	    while ((len = in.read(buf)) > 0) {
	        out.write(buf, 0, len);
	    }
	    in.close();
	    out.close();
	}
}
