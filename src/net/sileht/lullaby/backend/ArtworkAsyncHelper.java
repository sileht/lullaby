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
import java.util.regex.Pattern;

import org.apache.http.util.ByteArrayBuffer;

import net.sileht.lullaby.Lullaby;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

public class ArtworkAsyncHelper extends Handler {

	private final static String TAG = "LullabyArtworkAsyncHelper";
	private final static String mPatternReplace = "$1";
	private final static Pattern mPattern = Pattern
			.compile("[^=]*id=([^&]*)&.*");

	private final static int[] noCoverPixels = new int[] { -1582105, -2698282,
			-1582105, -2698282, -1582105, -2698282, -1582105, -2698282,
			-1582105, -2698282 };

	// static objects
	private static Handler sThreadHandler;
	private static ArtworkAsyncHelper sInstance;

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
		public Context context;
		public ImageView view;
		public String url;
		public int defaultResource;
		public int width;
		public int height;
		public Object result;
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
					args.result = getDrawable(args.url, args.width, args.height);
				} catch (IOException e) {
					args.result = null;
				}
				if (args.result != null) {
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

		private int getId(String url) {
			String sid = mPattern.matcher(url).replaceAll(mPatternReplace);
			try {
				int id = Integer.parseInt(sid);
				return id;
			} catch (Exception e) {
				return -1;
			}
		}

		private File getCacheFilePath(String filename) {
			File f = new File(Environment.getExternalStorageDirectory(),
					"Android/data/com.sileht.lullaby/cache/artwork/");
			f.mkdirs();
			return new File(f, filename);
		}

		private Drawable getDrawable(String url, int mWidth, int mHeight)
				throws IOException, MalformedURLException,
				FileNotFoundException {
			File f = getCacheFilePath("" + getId(url));
			if (!f.exists()) {
				URL uri = new URL(url.replaceAll("auth=[^&]+", "auth="
						+ Lullaby.comm.authToken));
				InputStream is = uri.openStream();
				BufferedInputStream bis = new BufferedInputStream(is);
				ByteArrayBuffer baf = new ByteArrayBuffer(50);
				int current = 0;
				while ((current = bis.read()) != -1) {
					baf.append((byte) current);
				}

				/* Convert the Bytes read to a String. */
				FileOutputStream fos = new FileOutputStream(f);
				fos.write(baf.toByteArray());
				fos.close();
			}
			URL uri = new URL("file://" + f.getPath());
			int sampleSize = 1;

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
				if (b == null) {
					return null;
				}
				Log.d(TAG, "DL OK: " + url);
			}
			return new BitmapDrawable(b);
		}
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
			int width, int height) {
		updateArtwork(context, imageView, url, placeholderImageResource, width,
				height, null);

	}

	public static final void updateArtwork(Context context,
			ImageView imageView, String url, int placeholderImageResource,
			int width, int height, OnImageLoadCompleteListener listener) {

		// in case the source caller info is null, the URI will be null as well.
		// just update using the placeholder image in this case.
		if (url == null) {
			Log.d(TAG, "target image is null, just display placeholder.");
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

		// setup message arguments
		Message msg = sThreadHandler.obtainMessage(0);
		msg.arg1 = 0;
		msg.obj = args;

		Log.d(TAG, "Begin loading image: " + args.url
				+ ", displaying default image for now.");

		// set the default image first, when the query is complete, we will
		// replace the image with the correct one.
		if (placeholderImageResource != -1) {
			// imageView.setVisibility(View.VISIBLE);
			imageView.setImageResource(placeholderImageResource);
		} else {
			// imageView.setVisibility(View.INVISIBLE);
		}

		// notify the thread to begin working
		sThreadHandler.sendMessage(msg);
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
			if (args.result != null) {
				// args.view.setVisibility(View.VISIBLE);
				args.view.setImageDrawable((Drawable) args.result);
				imagePresent = true;
			} else if (args.defaultResource != -1) {
				// args.view.setVisibility(View.VISIBLE);
				args.view.setImageResource(args.defaultResource);
			}
			// notify the listener if it is there.
			if (args.listener != null) {
				Log.d(TAG, "Notifying listener: " + args.listener.toString()
						+ " image: " + args.url + " completed");
				args.listener.onImageLoadComplete(msg.what, args.view,
						imagePresent);
			}

			break;
		default:
		}
	}

}
