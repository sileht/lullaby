package net.sileht.lullaby.backend;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;

import net.sileht.lullaby.Lullaby;
import net.sileht.lullaby.Utils;

import org.apache.http.util.ByteArrayBuffer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

public class ArtworkAsyncHelper extends Handler {

	private final static boolean LOG_ARTWORK_FINGERPRINT = false;
	private final static boolean DBG = false;
	private final static String TAG = "LullabyArtworkAsyncHelper";

	private final static String mPatternReplace = "$1";
	private final static Pattern mPattern = Pattern
			.compile("[^=]*id=([^&]*)&.*");

	private static HashMap<String, Drawable> mArtworkCache = new HashMap<String, Drawable>();

	private final static int[] noCoverPixels = new int[] { -1582105, -2698282,
			-1582105, -2698282, -1582105, -2698282, -1582105, -2698282,
			-1582105, -2698282 };

	// static objects
	@SuppressWarnings("unused")
	private static ArtworkAsyncHelper sInstance;
	private static Handler sThreadHandler;

	static {
		sInstance = new ArtworkAsyncHelper();
	}

	private static final BitmapFactory.Options sBitmapOptionsCache = new BitmapFactory.Options();

	// private static final BitmapFactory.Options sBitmapOptions = new
	// BitmapFactory.Options();

	/**
	 * Interface for a WorkerHandler result return.
	 */
	public interface OnImageLoadCompleteListener {
		/**
		 * Called when the image load is complete.
		 * 
		 * @param imagePresent
		 *            true if an image was found
		 */
		public void onImageLoadComplete(int token, ImageView iView,
				boolean imagePresent);
	}

	private static final class WorkerArgs {
		@SuppressWarnings("unused")
		public Context context;
		public ImageView view;
		public String url;
		public int defaultResource;
		public int width;
		public int height;
		public Object result;
		public boolean useFastDrawable;
		public OnImageLoadCompleteListener listener;

	}

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

	/**
	 * Thread worker class that handles the task of opening the stream and
	 * loading the images.
	 */
	private class WorkerHandler extends Handler {
		public WorkerHandler(Looper looper) {
			super(looper);
		}

		public void handleMessage(Message msg) {
			WorkerArgs args = (WorkerArgs) msg.obj;

			switch (msg.arg1) {
			case 0:
				try {
					args.result = getDrawable((WorkerArgs) msg.obj);
				} catch (IOException e) {
					args.result = null;
				}
				if (args.result != null) {
					if (DBG)
						Log.d(TAG, "Loading image: " + msg.arg1 + " token: "
								+ msg.what + " image URI: " + args.url);
				} else {
					Log.d(TAG, "Problem with image: " + msg.arg1 + " token: "
							+ msg.what + " image URI: " + args.url
							+ ", using default image.");
				}
				break;
			default:
			}

			// send the reply to the enclosing class.
			Message reply = ArtworkAsyncHelper.this.obtainMessage(msg.what);
			reply.arg1 = msg.arg1;
			reply.obj = msg.obj;
			reply.sendToTarget();
		}

		private Drawable getDrawable(WorkerArgs args)
				throws IOException, MalformedURLException,
				FileNotFoundException {

			URL finalUrl = null;

			Utils.checkStorage();
			File f = Utils.getCacheFile("artwork", "" + getId(args.url));

			if (!Utils.mExternalStorageAvailable) {

				finalUrl = new URL(args.url);

			} else if (f.exists()) {

				finalUrl = new URL("file://" + f.getPath());

			} else if (Utils.mExternalStorageWriteable) {

				URL uri = new URL(args.url.replaceAll("auth=[^&]+", "auth="
						+ Lullaby.comm.authToken));
				InputStream is = uri.openStream();
				BufferedInputStream bis = new BufferedInputStream(is, 8000);
				FileOutputStream fos = new FileOutputStream(f);

				ByteArrayBuffer baf = new ByteArrayBuffer(50);
				int current = 0;
				while ((current = bis.read()) != -1) {
					baf.append((byte) current);
				}

				fos.write(baf.toByteArray());
				fos.close();
				bis.close();
				is.close();

				finalUrl = new URL("file://" + f.getPath());

			}

			// First pass to get attribut of cover
			int sampleSize = 1;
			sBitmapOptionsCache.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(finalUrl.openStream(), null,
					sBitmapOptionsCache);

			// Second pass to get resample cover
			int nextWidth = sBitmapOptionsCache.outWidth >> 1;
			int nextHeight = sBitmapOptionsCache.outHeight >> 1;
			while (nextWidth > args.width && nextHeight > args.height) {
				sampleSize <<= 1;
				nextWidth >>= 1;
				nextHeight >>= 1;
			}
			sBitmapOptionsCache.inSampleSize = sampleSize;
			sBitmapOptionsCache.inJustDecodeBounds = false;
			Bitmap b = BitmapFactory.decodeStream(finalUrl.openStream(), null,
					sBitmapOptionsCache);

			if (b == null) {
				return null;
			}

			// Third pass check if cover is really the good cover
			int size = 10;
			int x = 25;
			int y = 25;
			if (sBitmapOptionsCache.outWidth >= x + size
					&& sBitmapOptionsCache.outWidth >= y) {
				int pixels[] = new int[size];
				b.getPixels(pixels, 0, size, x, y, size, 1);

				if (LOG_ARTWORK_FINGERPRINT) {
					// To find fingerprint of the no cover
					System.out.println("pixel size : size=" + size);
					System.out.println(args.url);
					System.out.print("int[] noCoverPixels = new int[]{");
					String sep = "";
					for (int pixel : pixels) {
						System.out.print(sep + pixel);
						sep = ", ";
					}
					System.out.println("}");
				}

				if (Arrays.equals(pixels, noCoverPixels)) {
					return null;
				}

			}

			// finally rescale to exactly the size we need
			if (sBitmapOptionsCache.outWidth != args.width
					|| sBitmapOptionsCache.outHeight != args.height) {
				Bitmap tmp = Bitmap
						.createScaledBitmap(b, args.width, args.height, true);
				// Bitmap.createScaledBitmap() can return the same
				// bitmap
				if (tmp != b)
					b.recycle();
				b = tmp;
			}
			if (b == null) {
				return null;
			}
			if (args.useFastDrawable){
				return new FastBitmapDrawable(b);
			} else {
				return new BitmapDrawable(b);
			}
		}
	}

	private static int getId(String url) {
		String sid = mPattern.matcher(url).replaceAll(mPatternReplace);
		try {
			int id = Integer.parseInt(sid);
			return id;
		} catch (Exception e) {
			return -1;
		}
	}

	private static String getHash(WorkerArgs w) {
		return w.width + "x" + w.height + ":" + getId(w.url);
	}

	/**
	 * Private constructor for static class
	 */
	private ArtworkAsyncHelper() {
		HandlerThread thread = new HandlerThread("ArtworkAsyncHelper");
		thread.start();
		sThreadHandler = new WorkerHandler(thread.getLooper());
	}

	public static final void updateArtwork(Context context,
			ImageView imageView, String url, int placeholderImageResource,
			int width, int height, boolean useFastDrawable) {
		updateArtwork(context, imageView, url, placeholderImageResource, width,
				height, useFastDrawable, null);

	}

	public static final void updateArtwork(Context context,
			ImageView imageView, String url, int placeholderImageResource,
			int width, int height,boolean useFastDrawable, OnImageLoadCompleteListener listener) {

		// in case the source caller info is null, the URI will be null as well.
		// just update using the placeholder image in this case.
		if (url == null) {
			if (DBG)
				Log
						.d(TAG,
								"target image is null, juif (DBG) Log.isplay placeholder.");
			imageView.setVisibility(View.VISIBLE);
			imageView.setImageResource(placeholderImageResource);
			return;
		}
		// setup arguments
		WorkerArgs args = new WorkerArgs();
		args.context = context;
		args.view = imageView;
		args.url = url;
		args.defaultResource = placeholderImageResource;
		args.width = width;
		args.height = height;
		args.listener = listener;

		if (mArtworkCache.containsKey(getHash(args))) {
			Drawable d = mArtworkCache.get(getHash(args));
			if (d != null) {
				args.view.setImageDrawable(d);
			} else if (placeholderImageResource != -1) {
				imageView.setImageResource(placeholderImageResource);
			}
		} else {
			// setup message arguments
			Message msg = sThreadHandler.obtainMessage(0);
			msg.arg1 = 0;
			msg.obj = args;

			if (DBG)
				Log.d(TAG, "Begin loading image: " + args.url
						+ ", displaying default image for now.");

			// set the default image first, when the query is complete, we will
			// replace the image with the correct one.
			if (placeholderImageResource != -1) {
				imageView.setImageResource(placeholderImageResource);
			}

			// notify the thread to begin working
			sThreadHandler.sendMessage(msg);
		}
	}

	/**
	 * Called when loading is done.
	 */
	@Override
	public void handleMessage(Message msg) {
		WorkerArgs args = (WorkerArgs) msg.obj;
		switch (msg.arg1) {
		case 0:
			boolean imagePresent = false;
			// if the image has been loaded then display it, otherwise set
			// default.
			// in either case, make sure the image is visible.

			mArtworkCache.put(getHash(args), (Drawable) args.result);
			if (args.result != null) {
				args.view.setImageDrawable((Drawable) args.result);
				imagePresent = true;
			} else if (args.defaultResource != -1) {
				args.view.setImageResource(args.defaultResource);
			}
			// notify the listener if it is there.
			if (args.listener != null) {
				if (DBG)
					Log.d(TAG, "Notifying listener: "
							+ args.listener.toString() + " image: " + args.url
							+ " completed");
				args.listener.onImageLoadComplete(msg.what, args.view,
						imagePresent);
			}

			break;
		default:
		}
	}
}
