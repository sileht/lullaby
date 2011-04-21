package net.sileht.lullaby.player;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class StreamCacher implements Runnable {
	final static private String TAG = "LullabyStreamCacher";
	private ServerSocket mSocket;
	private Boolean mStopServer = false;

	private Context mContext;

	private Thread mThread;

	public StreamCacher(Context context) {
		mContext = context;
		try {
			mSocket = new ServerSocket(0, 0,
					InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }));
			mSocket.setSoTimeout(5000);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int getPort() {
		return mSocket.getLocalPort();
	}

	public void start() {
		mThread = new Thread(this);
		mThread.start();
	}

	public void stop() {
		mStopServer = true;
		try {
			mThread.join(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while (!mStopServer) {
			try {
				Socket client = mSocket.accept();
				if (client == null) {
					continue;
				}
				processCachingAndStreaming(client);
			} catch (SocketTimeoutException e) {
				// Do nothing
			} catch (IOException e) {
				Log.e(TAG, "Error connecting to client", e);
			}
		}
	}

	private void processCachingAndStreaming(Socket client) {

		InputStream ris = null;
		InputStream cis = null;
		BufferedOutputStream fos = null;
		String firstLine;
		String uri;

		File cacheFile = new File(mContext.getExternalCacheDir(), "stream.mp3");
		if (cacheFile.exists()) {
			cacheFile.delete();
		}

		try {
			cis = client.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					cis));
			firstLine = reader.readLine();

			if (firstLine == null) {
				Log.i(TAG, "Proxy client closed connection without a request.");
				return;
			}

			StringTokenizer st = new StringTokenizer(firstLine);

			@SuppressWarnings("unused")
			String method = st.nextToken();
			uri = st.nextToken().substring(1);

			HttpURLConnection connection = (HttpURLConnection) (new URL(uri))
					.openConnection();

			connection.setDoOutput(true);
			connection.connect();

			Log.i(TAG, "===== PROXY STATUS AND HEADERS =====");
			// Forward status and headers
			String status = "HTTP/1.1 " + connection.getResponseCode() + " "
					+ connection.getResponseMessage() + "\n";
			Log.i(TAG, status);
			client.getOutputStream().write(status.getBytes());

			for (Entry<String, List<String>> e : connection.getHeaderFields()
					.entrySet()) {
				if (e.getKey().equals("Accept-Ranges")) {
					continue;
				}
				String header = e.getKey() + ": "
						+ TextUtils.join(";", e.getValue()) + "\n";
				Log.v(TAG, header);
				client.getOutputStream().write(header.getBytes());
			}
			client.getOutputStream().write("\n".getBytes());

			ris = connection.getInputStream();
			final int nbBytesTotal = connection.getContentLength();
			fos = new BufferedOutputStream(new FileOutputStream(cacheFile));

			byte[] b = new byte[50 * 1024];
			int nbBytesReadIncremental = 1;
			int nbBytesRead;
			while ((nbBytesRead = ris.read(b)) != -1) {
				if (mStopServer) {
					return;
				}

				if (nbBytesReadIncremental % (10 * 50 * 1024) == 0) {
					Log.i(TAG, "Progress: " + nbBytesReadIncremental + "/"
							+ nbBytesTotal);
				}

				// Write to cache and to client
				fos.write(b, 0, nbBytesRead);
				nbBytesReadIncremental += nbBytesRead;
				client.getOutputStream().write(b, 0, nbBytesRead);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} finally {
			if (ris != null) {
				try {
					ris.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (cis != null) {
				try {
					cis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		String oid = uri.replaceFirst(".*oid=([^&]+).*", "$1");
		File newCacheFile = new File(mContext.getExternalCacheDir(), "stream-"
				+ oid + ".mp3");
		if (newCacheFile.exists()) {
			newCacheFile.delete();
		}
		cacheFile.renameTo(newCacheFile);
		Log.i(TAG, "Finalize Cachefile: " + newCacheFile);
	}
}
