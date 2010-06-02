package net.sileht.lullaby.backend;

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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;

import net.sileht.lullaby.Lullaby;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.ImageView;

public class ArtworkBackend extends HandlerThread {

	private Handler mThreadHandler;

	private static final String TAG = "DroidZikArtwork";
	private final HashMap<Integer, Drawable> sArtCache;

	private final BitmapFactory.Options sBitmapOptionsCache;
	private final String mPatternReplace = "$1";
	private final Pattern mPattern =  Pattern.compile("[^=]*id=([^&]*)&.*");

	private final static int[] noCoverPixels = new int[] { -1582105, -2698282,
			-1582105, -2698282, -1582105, -2698282, -1582105, -2698282,
			-1582105, -2698282 };

	// private static final BitmapFactory.Options sBitmapOptions;

	// A really simple BitmapDrawable-like class, that doesn't do
	// scaling, dithering or filtering.
	private static class FastBitmapDrawable extends Drawable {
		private Bitmap mBitmap;

		public FastBitmapDrawable(Bitmap b) {
			mBitmap = b;
		}

		@Override
		public void draw(Canvas canvas) {
			canvas.drawBitmap(mBitmap, 0, 0, null);
		}

		@Override
		public int getOpacity() {
			return PixelFormat.OPAQUE;
		}

		@Override
		public void setAlpha(int alpha) {
		}

		@Override
		public void setColorFilter(ColorFilter cf) {
		}

	}

	private int mWidth;
	private int mHeight;
	private BitmapDrawable mDefaultDrawable;

	public Drawable getCachedArtwork(String url, BitmapDrawable defaultArtwork) {
		Drawable d = null;
		int id = getId(url);
		synchronized (sArtCache) {
			d = sArtCache.get(id);
		}
		if (d == null) {
			d = defaultArtwork;
			/*
			final Bitmap icon = defaultArtwork.getBitmap();
			
			int w = icon.getWidth();
			int h = icon.getHeight();
			*/
			Bitmap b = urlToBitmap(url);
			if (b != null) {
				d = new FastBitmapDrawable(b);
				synchronized (sArtCache) {
					// the cache may have changed since we checked
					Drawable value = sArtCache.get(id);
					if (value == null) {
						sArtCache.put(id, d);
					} else {
						d = value;
					}
				}
			}
		}
		return d;
	}

	public ArtworkBackend() {
		super("ArtworkBackend");
		sBitmapOptionsCache = new BitmapFactory.Options();
		// sBitmapOptions = new BitmapFactory.Options();
		sArtCache = new HashMap<Integer, Drawable>();
		setDaemon(true);
		start();
	}

	public void setCachedArtwork(ImageView iv, String url) {
		this.setCachedArtwork(iv, url, false);
	}

	private int getId(String url) {
		String sid = mPattern.matcher(url).replaceAll(mPatternReplace);
		try {
			int id = Integer.parseInt(sid);
			return id;
		} catch (Exception e) {
			return -1;
		}
	}

	public void setCachedArtwork(final ImageView iv, String url, boolean quick) {
		int id = getId(url);
		if (mThreadHandler != null && mDefaultDrawable != null) {
			if (sArtCache.containsKey(id)) {
				iv.setImageDrawable(sArtCache.get(id));
			} else {
				Message requestMsg = new Message();
				requestMsg.obj = (Object) url;
				requestMsg.replyTo = new Messenger(new SyncUIHandler(iv, id));
				if (quick) {
					mThreadHandler.sendMessageAtFrontOfQueue(requestMsg);
				} else {
					mThreadHandler.sendMessage(requestMsg);
				}
			}
		}
	}

	class SyncUIHandler extends Handler {
		final private ImageView mImageView;
		private int mId;

		public SyncUIHandler(ImageView iv, int id) {
			super();
			mImageView = iv;
			mId = id;
		}

		@Override
		public void handleMessage(Message msg) {
			final Bitmap b = (Bitmap) msg.obj;
			if (b != null) {
				Drawable d = new FastBitmapDrawable(b);
				sArtCache.put(mId, d);
				mImageView.setImageDrawable(d);
				// mImageView.postInvalidate();
			}
		}
	}

	public void setDrawable(BitmapDrawable defaultDrawable) {
		mDefaultDrawable = defaultDrawable;
		final Bitmap icon = mDefaultDrawable.getBitmap();
		// NOTE: There is in fact a 1 pixel border on the right side in the
		// ImageView
		// used to display this drawable. Take it into account now, so we
		// don't have to
		// scale later.
		mWidth = icon.getWidth() - 1;
		mHeight = icon.getHeight();
	}

	@Override
	protected void onLooperPrepared() {
		mThreadHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				Message reply = Message.obtain();
				reply.obj = urlToBitmap((String) msg.obj);
				try {
					msg.replyTo.send(reply);
				} catch (RemoteException e) {
					// Shit
				}

			}
		};
	}

	private Bitmap urlToBitmap(String url) {
		URL uri;
		try {
			uri = new URL(url.replaceAll("auth=[^&]+", "auth="
					+ Lullaby.comm.authToken));
		} catch (MalformedURLException e) {
			Log.i(TAG, "MalformedURLException: " + url, e);
			return null;
		}
		try {
			int sampleSize = 1;

			// Compute the closest power-of-two scale factor
			// and pass that to sBitmapOptionsCache.inSampleSize, which
			// will
			// result in faster decoding and better quality
			sBitmapOptionsCache.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(uri.openStream(), null,
					sBitmapOptionsCache);
			int nextWidth = sBitmapOptionsCache.outWidth >> 1;
			int nextHeight = sBitmapOptionsCache.outHeight >> 1;
			while (nextWidth > mWidth && nextHeight > mHeight) {
				sampleSize <<= 1;
				nextWidth >>= 1;
				nextHeight >>= 1;
			}

			sBitmapOptionsCache.inSampleSize = sampleSize;
			sBitmapOptionsCache.inJustDecodeBounds = false;
			Bitmap b = BitmapFactory.decodeStream(uri.openStream(), null,
					sBitmapOptionsCache);

			if (b != null) {

				int size = 10;
				int x = 25;
				int y = 25;
				if (sBitmapOptionsCache.outWidth >= x + size
						&& sBitmapOptionsCache.outWidth >= y) {
					int pixels[] = new int[size];
					b.getPixels(pixels, 0, size, x, y, size, 1);

					/**
					 * To find fingerprint
					 * System.out.println("pixel size : size="+size);
					 * System.out.println(url);
					 * System.out.print("int[] noCoverPixels = new int[]{");
					 * String sep = ""; for (int pixel : pixels) {
					 * System.out.print(sep + pixel ); sep = ", "; }
					 * System.out.println("}");
					 */

					if (Arrays.equals(pixels, noCoverPixels)) {
						return null;
					}

				}
				// finally rescale to exactly the size we need
				if (sBitmapOptionsCache.outWidth != mWidth
						|| sBitmapOptionsCache.outHeight != mHeight) {
					Bitmap tmp = Bitmap.createScaledBitmap(b, mWidth, mHeight,
							true);
					// Bitmap.createScaledBitmap() can return the same
					// bitmap
					if (tmp != b)
						b.recycle();
					b = tmp;
				}
				Log.d(TAG, "DL OK: " + url);
			}
			return b;
		} catch (IOException e) {
			Log.i(TAG, "IOException: " + url, e);
		}
		return null;
	}
}
