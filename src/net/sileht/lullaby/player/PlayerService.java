package net.sileht.lullaby.player;

/* Copyright (c) 20010 ABAAKOUK Mehdi  <theli48@gmail.com>
 * For the PhoneStateListener:
 *  Copyright (c) 2008 Kevin James Purdy <purdyk@onid.orst.edu>
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
import java.util.ArrayList;

import net.sileht.lullaby.AmpacheRequest;
import net.sileht.lullaby.Lullaby;
import net.sileht.lullaby.PlayingActivity;
import net.sileht.lullaby.R;
import net.sileht.lullaby.objects.Song;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

public class PlayerService extends Service implements
		MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
		MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnErrorListener {

	private static String TAG = "LullabyPlayer";

	private Song mSong;
	private MediaPlayer mPlayer;
	private int mBuffering = -1;
	private StreamCacher mStreamCacher;

	private boolean mPlayAfterPrepared = false;

	private enum STATE {
		Idle, Initialised, Preparing, Prepared, Started, Paused, Stopped
	}

	private STATE mState;

	private MyPhoneStateListener mPhoneStateListener;

	private ArrayList<OnStatusListener> mPlayerListeners;

	private Handler mTickHandler = new Handler();

	private Runnable mTickTask = new Runnable() {
		@Override
		public void run() {
			for (OnStatusListener obj : mPlayerListeners) {
				obj.onTick(getCurrentPosition(), getDuration(), getBuffer());
			}
			mTickHandler.postDelayed(this, 100);
		}
	};

	public PlayingPlaylist mPlaylist;

	public static interface OnStatusListener {
		public void onBuffering(int buffer);

		public void onTogglePlaying(boolean playing);

		public void onStatusChange();

		public void onTick(int position, int duration, int buffer);
	}

	@Override
	public void onCreate() {

		Context ctx = getApplicationContext();
		TelephonyManager tmgr = (TelephonyManager) ctx
				.getSystemService(Context.TELEPHONY_SERVICE);
		tmgr.listen(mPhoneStateListener, 0);

		mStreamCacher = new StreamCacher(ctx);
		mStreamCacher.start();
		
		mPlaylist = new PlayingPlaylist(this);

		mPhoneStateListener = new MyPhoneStateListener();

		mPlayerListeners = new ArrayList<OnStatusListener>();

		mPlayer = new MediaPlayer();
		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mPlayer.setOnErrorListener(this);
		mPlayer.setOnPreparedListener(this);
		mPlayer.setOnCompletionListener(this);
		mPlayer.setOnBufferingUpdateListener(this);

		setState(STATE.Idle);

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		String id = settings.getString("song_id", null);
		int pos = settings.getInt("playlist_pos", -1);

		if (mPlaylist.load(ctx) && pos != -1) {
			mPlaylist.setCurrentPosition(pos);
		}

		if (id != null && !id.equals("")) {
			AmpacheRequest request = new AmpacheRequest(null, new String[] {
					"song", id }, true, false) {
				@Override
				public void add_objects(@SuppressWarnings("rawtypes") ArrayList list) {
					if (!list.isEmpty() && mSong == null) {
						setSong((Song) list.get(0));
					}
				}
			};
			request.send();
		}
		Log.v(TAG, "Lullaby Player Service Start");
		mTickHandler.postDelayed(mTickTask, 100);

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		mStreamCacher.stop();
		
		doPlaybackStop();

		stopForeground(true);

		Context ctx = getApplicationContext();
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = settings.edit();
		if (mSong != null) {
			editor.putString("song_id", mSong.id);
		} else {
			editor.putString("song_id", null);
		}
		mPlaylist.save(ctx);
		int pos = mPlaylist.getCurrentPosition();
		editor.putInt("playlist_pos", pos);

		// Save pref
		editor.commit();

		Log.d(TAG, "Service Destroyed");
	}

	private void setState(STATE state) {

		boolean isPreviouslyPlaying = isPlaying();
		STATE pm = mState;
		mState = state;
		boolean isPlaying = isPlaying();

		Log.d(TAG, "ps:" + isPreviouslyPlaying + " - pm:" + pm + " | s:"
				+ isPlaying + " - " + mState);

		if (isPlaying != isPreviouslyPlaying) {
			for (OnStatusListener obj : mPlayerListeners) {
				obj.onTogglePlaying(isPlaying);
			}
			if (isPlaying) {
				if (mSong != null) {
					RemoteViews views = new RemoteViews(this.getPackageName(),
							R.layout.statusbar);
					views.setImageViewResource(R.id.icon,
							R.drawable.status_icon);
					views.setTextViewText(R.id.trackname, mSong.name);
					views.setTextViewText(R.id.artistalbum, mSong.album + " - "
							+ mSong.artist);

					Notification n = new Notification();
					n.icon = R.drawable.status_icon;
					n.tickerText = "Playing " + mSong.name;
					n.flags |= Notification.FLAG_ONGOING_EVENT;
					n.contentView = views;
					Intent i = new Intent(this, PlayingActivity.class);
					i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
					n.contentIntent = PendingIntent.getActivity(this, 0, i, 0);
					startForeground(1, n);

				}
			} else {
				stopForeground(true);
			}
		}
		Log.d(TAG, "setState(" + mState + ")");
	}

	public boolean isSeekable() {
		return mState == STATE.Prepared || mState == STATE.Started
				|| mState == STATE.Paused;
	}

	public boolean isPlaying() {
		return (mPlayAfterPrepared && (mState == STATE.Initialised || mState == STATE.Preparing))
				|| mState == STATE.Started;
	}

	public int getCurrentPosition() {
		if (mState == STATE.Prepared || mState == STATE.Started
				|| mState == STATE.Paused) {
			return mPlayer.getCurrentPosition();
		} else {
			return 0;
		}
	}

	public int getDuration() {
		if (mState == STATE.Initialised || mState == STATE.Preparing
				|| mState == STATE.Prepared || mState == STATE.Started
				|| mState == STATE.Paused) {
			try {
				return Integer.parseInt(mSong.time) * 1000;
			} catch (Exception poo) {
			}
			if (mState != STATE.Initialised && mState != STATE.Preparing) {
				return mPlayer.getDuration();
			}
		}
		return 0;
	}

	public int getBuffer() {
		return mBuffering;
	}

	public Song getSong() {
		return mSong;
	}

	public void setOnPlayerListener(OnStatusListener statusChangeObject) {
		mPlayerListeners.add(statusChangeObject);
		if (mSong != null) {
			statusChangeObject.onStatusChange();
			statusChangeObject.onTogglePlaying(isPlaying());
		}
	}

	private void setSong(Song song) {

		setState(STATE.Idle);

		String uri = song.url.replaceFirst(".ogg$", ".mp3").replaceFirst(
				".flac$", ".mp3").replaceFirst(".m4a$", ".mp3").replaceAll(
				"sid=[^&]+", "sid=" + Lullaby.comm.authToken);

		Context ctx = getApplicationContext();
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		if (settings.getString("server_url_preference", "").startsWith(
				"https://")) {
			uri = uri.replaceFirst("^http:", "https:");
		}


		if (mState == STATE.Prepared || mState == STATE.Started
				|| mState == STATE.Paused) {
			mPlayer.stop();
		}

		String oid = uri.replaceFirst(".*oid=([^&]+).*", "$1");
		File cacheFile = new File(getExternalCacheDir(), "stream-"+oid+".mp3");
		if (cacheFile.exists()){
			Log.v(TAG, "Playing uri: " + uri +" (cached: "+cacheFile.toString()+")");
			uri = cacheFile.toString();
			onBufferingUpdate(mPlayer, 100);
		} else {
			Log.v(TAG, "Playing uri: " + uri);
			uri = "http://127.0.0.1:"+mStreamCacher.getPort()+"/"+uri;
			onBufferingUpdate(mPlayer, -1);
		}
		
		mSong = song;


		for (OnStatusListener obj : mPlayerListeners) {
			obj.onStatusChange();
		}

		mPlayer.reset();
		try {
			mPlayer.setDataSource(uri);
			setState(STATE.Initialised);
		} catch (Exception blah) {
			return;
		}
	}

	

	protected void playSong(Song song) {
		Lullaby.comm.ping();
		setSong(song);
		mPlayAfterPrepared = true;
		mPlayer.prepareAsync();
		setState(STATE.Preparing);
	}

	public void doPlaybackPauseResume() {
		if (mState == STATE.Started || mState == STATE.Paused) {
			if (mPlayer.isPlaying()) {
				mPlayer.pause();
				setState(STATE.Paused);
			} else {
				mPlayer.start();
				setState(STATE.Started);
			}
		} else if (mState == STATE.Preparing) {
			mPlayAfterPrepared = !mPlayAfterPrepared;
		} else if (mState == STATE.Prepared) {
			mPlayer.start();
			setState(STATE.Started);
		} else if (mState == STATE.Initialised) {
			mPlayAfterPrepared = true;
			mPlayer.prepareAsync();
			setState(STATE.Preparing);
		} else {
			mPlaylist.playNextAutomatic();
		}
	}

	public void doPlaybackStop() {
		mPlayAfterPrepared = false;
		for (OnStatusListener obj : mPlayerListeners) {
			obj.onStatusChange();
		}
		setState(STATE.Stopped);
		mPlayer.stop();
		mPlayer.reset();
	}

	public void doSeekTo(int position) {
		if (mState == STATE.Prepared || mState == STATE.Started
				|| mState == STATE.Paused) {
			mPlayer.seekTo(position);
		}
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Log.e(TAG, "Player error (" + what + "," + extra + ")");
		return false;
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		setState(STATE.Prepared);
		if (mPlayAfterPrepared) {
			mPlayer.start();
			setState(STATE.Started);
		}/*
		 * else { mPlayer.pause(); setState(STATE.Paused); }
		 */
		mPlayAfterPrepared = false;
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		mPlayer.stop();
		setState(STATE.Stopped);
		mSong = null;

		Log.v(TAG, "Completion");
		Song song = mPlaylist.playNextAutomatic();
		if (song == null) {
			doPlaybackStop();
		}
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int buffer) {
		mBuffering = buffer;
		for (OnStatusListener obj : mPlayerListeners) {
			obj.onBuffering(mBuffering);
		}
	}

	// Handle phone calls
	private class MyPhoneStateListener extends PhoneStateListener {
		private Boolean mResumeAfterCall = false;

		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			Context ctx = getApplicationContext();
			if (state == TelephonyManager.CALL_STATE_RINGING) {
				AudioManager audioManager = (AudioManager) ctx
						.getSystemService(Context.AUDIO_SERVICE);
				int ringvolume = audioManager
						.getStreamVolume(AudioManager.STREAM_RING);
				if (ringvolume > 0) {
					mResumeAfterCall = (mPlayer.isPlaying() || mResumeAfterCall);
					mPlayer.pause();
				}
			} else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
				// pause the music while a conversation is in progress
				mResumeAfterCall = (mPlayer.isPlaying() || mResumeAfterCall);
				mPlayer.pause();
			} else if (state == TelephonyManager.CALL_STATE_IDLE) {
				// start playing again
				if (mResumeAfterCall) {
					// resume playback only if music was playing
					// when the call was answered
					mPlayer.start();
					mResumeAfterCall = false;
				}
			}
		}
	}

	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class PlayerBinder extends Binder {
		public PlayerService getService() {
			return PlayerService.this;
		}
	}

	// This is the object that receives interactions from clients. See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new PlayerBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

}
