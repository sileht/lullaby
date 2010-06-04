package net.sileht.lullaby.backend;

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
import java.util.ArrayList;

import net.sileht.lullaby.Lullaby;
import net.sileht.lullaby.MainActivity;
import net.sileht.lullaby.R;
import net.sileht.lullaby.objects.Song;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class Player extends Service{

	private MediaPlayer mPlayer;

	private static String TAG = "DroidZikPlayer";
	private Song mSong;
	private int mBuffering = -1;

	private Boolean mPlayAfterPrepared = true;

	private enum STATE {
		Idle, Initialised, Prepared, Started, Paused, Stopped
	}

	private STATE mState;

	private MyPhoneStateListener mPhoneStateListener;
	private MyMediaPlayerListener mMediaPlayerListener;

	private ArrayList<PlayerListener> mPlayerListeners;

	public PlayingPlaylist mPlaylist;
	
    private NotificationManager mNM;
    private Notification mNotification;


	public static abstract class PlayerListener {
		abstract public void onTogglePlaying(boolean playing);

		abstract public void onNewSongPlaying(Song song);

		abstract public void onBuffering(int buffer);
		abstract public void onPlayerStopped();
	}

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class PlayerBinder extends Binder {
    	public Player getService() {
            return Player.this;
        }
    }

    @Override
    public void onCreate() {
		
    	mPlaylist = new PlayingPlaylist(this);
    	
		mPhoneStateListener = new MyPhoneStateListener();

		mPlayerListeners = new ArrayList<PlayerListener>();

		mMediaPlayerListener = new MyMediaPlayerListener();

		mPlayer = new MediaPlayer();
		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mPlayer.setOnErrorListener(mMediaPlayerListener);
		mPlayer.setOnPreparedListener(mMediaPlayerListener);
		mPlayer.setOnCompletionListener(mMediaPlayerListener);
		mPlayer.setOnBufferingUpdateListener(mMediaPlayerListener);

		
		setState(STATE.Idle);
		
		showNotification();
		
        Toast.makeText(this, "Lullaby Player Service Start", Toast.LENGTH_SHORT).show();
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(1);

        Toast.makeText(this, "Lullaby Player Service Stopped", Toast.LENGTH_SHORT).show();
        

		Context ctx = getApplicationContext();
		TelephonyManager tmgr = (TelephonyManager) ctx
				.getSystemService(Context.TELEPHONY_SERVICE);
		tmgr.listen(mPhoneStateListener, 0);
		
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new PlayerBinder();


	
	public void showNotification(){
		mNotification = new Notification(R.drawable.icon, this.getString(R.string.app_name), System.currentTimeMillis());
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		updateNotification("Nop", "Nop");
		mNotificationManager.notify(1, mNotification);
		
	}

	public void updateNotification(CharSequence title, CharSequence text){
		Intent notificationIntent = new Intent(this, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		mNotification.setLatestEventInfo(this, title, text, contentIntent);
	}
	private void setState(STATE state) {
		mState = state;

		for (PlayerListener obj : mPlayerListeners) {
			obj.onTogglePlaying(isPlaying());
		}

		String st = "";
		switch (state) {
		case Idle:
			st = "Idle";
			break;
		case Initialised:
			st = "Initialised";
			break;
		case Prepared:
			st = "Prepared";
			break;
		case Started:
			st = "Started";
			break;
		case Paused:
			st = "Paused";
			break;
		case Stopped:
			st = "Stopped";
			break;
		default:
			st = "Unknown";
			break;
		}
		Log.v(TAG, "setState(" + st + ")");
	}

	private void updateBuffer(int buffer) {
		mBuffering = buffer;
		for (PlayerListener obj : mPlayerListeners) {
			obj.onBuffering(mBuffering);
		}
	}

	public int getBuffer() {
		return mBuffering;
	}

	public void playSong(Song song) {
		setState(STATE.Idle);

		String uri = song.url.replaceFirst(".ogg$", ".mp3").replaceFirst(".flac$", ".mp3").replaceFirst(".m4a$", ".mp3").replaceAll(
				"sid=[^&]+", "sid=" + Lullaby.comm.authToken);
		Log.v(TAG, "Playing uri: " + uri);

		if (mState == STATE.Prepared || mState == STATE.Started
				|| mState == STATE.Paused) {
			mPlayer.stop();
		}

		mPlayAfterPrepared = true;
		mSong = song;
		updateBuffer(-1);

		for (PlayerListener obj : mPlayerListeners) {
			obj.onNewSongPlaying(mSong);
		}

		mPlayer.reset();
		try {
			mPlayer.setDataSource(uri);
			setState(STATE.Initialised);
			mPlayer.prepareAsync();
		} catch (Exception blah) {
			return;
		}
	}

	public void doPauseResume() {
		if (mState == STATE.Started || mState == STATE.Paused) {
			if (mPlayer.isPlaying()) {
				mPlayer.pause();
				setState(STATE.Paused);
			} else {
				mPlayer.start();
				setState(STATE.Started);
			}
		} else if (mState == STATE.Initialised) {
			mPlayAfterPrepared = !mPlayAfterPrepared;
		} else if (mState == STATE.Prepared) {
			mPlayer.start();
			setState(STATE.Started);
		}
	}

	public void seekTo(int position) {
		if (mState == STATE.Prepared || mState == STATE.Started
				|| mState == STATE.Paused) {
			mPlayer.seekTo(position);
		}
	}

	public boolean isSeekable() {
		return mState == STATE.Prepared || mState == STATE.Started
				|| mState == STATE.Paused;
	}

	public boolean isPlaying() {
		return (mState == STATE.Initialised && mPlayAfterPrepared)
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
		if (mState == STATE.Initialised || mState == STATE.Prepared
				|| mState == STATE.Started || mState == STATE.Paused) {
			try {
				return Integer.parseInt(mSong.time) * 1000;
			} catch (Exception poo) {
			}
			if (mState != STATE.Initialised) {
				return mPlayer.getDuration();
			}
		}
		return 0;
	}

	public void setPlayerListener(PlayerListener StatusChangeObject) {
		mPlayerListeners.add(StatusChangeObject);
	}

	private class MyMediaPlayerListener implements
			MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
			MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnErrorListener {
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
			}
			mPlayAfterPrepared = true;
		}

		@Override
		public void onCompletion(MediaPlayer mp) {
			mSong = null;
			mPlayer.stop();
			setState(STATE.Stopped);

			Log.v(TAG, "Completion");
			Song song = mPlaylist.playNext();
			if (song == null) {
				for (PlayerListener obj : mPlayerListeners) {
					obj.onPlayerStopped();
				}
				mPlayer.stop();
				mPlayer.reset();
				setState(STATE.Stopped);
			}
		}

		@Override
		public void onBufferingUpdate(MediaPlayer mp, int buffer) {
			updateBuffer(buffer);
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
}
