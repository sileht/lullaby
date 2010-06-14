package net.sileht.lullaby;

import net.sileht.lullaby.R;
import net.sileht.lullaby.backend.ArtworkAsyncHelper;
import net.sileht.lullaby.objects.Song;
import net.sileht.lullaby.player.PlayerService;
import net.sileht.lullaby.player.PlayingPlaylist.REPEAT_MODE;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.gesture.GestureOverlayView;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class PlayingActivity extends Activity {

	// private static final int SWIPE_MAX_OFF_PATH = 250; // initial value
	private static final int SWIPE_MAX_OFF_PATH = 300;
	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;

	private static int mArtworkWidth = -1;
	private static int mArtWorkHeight = -1;

	private TextView mTrackName;
	private TextView mArtistName;
	private TextView mAlbumName;
	private ImageView artwork;
	private TextView mTimeView;
	private TextView mDurationView;
	private SeekBar mProgress;

	private ImageButton playpause;
	private ImageButton next;
	private ImageButton previous;
	private ImageButton repeat;
	private ImageButton shuffle;
	private ImageButton curplaylist;

	private GestureDetector gestureDetector;
	private View.OnTouchListener gestureListener;

	private boolean isCurrentlySeek = false;

	private static final String TAG = "LullabyPlayingActivity";

	// Bind Service Player
	private PlayerService mPlayer;
	private ServiceConnection mPlayerConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			mPlayer = ((PlayerService.PlayerBinder) service).getService();
			mPlayer.setPlayerListener(new MyPlayerListener());
			mPlayer.mPlaylist.load(PlayingActivity.this);
			setToggleButtonImage();
			setRepeatButtonImage();
			setPlayPauseButtonImage();
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			mPlayer = null;
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.audio_player);

		if (mArtworkWidth < 0) {
			Bitmap icon = ((BitmapDrawable) this.getResources().getDrawable(
					R.drawable.albumart_mp_unknown)).getBitmap();
			mArtworkWidth = icon.getWidth();
			mArtWorkHeight = icon.getHeight();
		}

		mTrackName = (TextView) findViewById(R.id.trackname);
		mArtistName = (TextView) findViewById(R.id.artistname);
		mAlbumName = (TextView) findViewById(R.id.albumname);

		artwork = (ImageView) findViewById(R.id.album);
		artwork.setImageResource(R.drawable.albumart_mp_unknown);

		curplaylist = (ImageButton) findViewById(R.id.curplaylist);
		curplaylist.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent();
				i.setClass(v.getContext(), MainActivity.class);
				startActivityFromChild((Activity) PlayingActivity.this, i, 0);
				finish();
			}
		});

		mTimeView = (TextView) findViewById(R.id.currenttime);
		mDurationView = (TextView) findViewById(R.id.totaltime);
		
		mProgress = (SeekBar) findViewById(android.R.id.progress);
		mProgress.setMax(1000);
		mProgress.setProgress(0);
		mProgress.setSecondaryProgress(0);
		mProgress.setOnSeekBarChangeListener(new MySeekBarListener());

		playpause = (ImageButton) findViewById(R.id.pause);
		playpause.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mPlayer.doPlaybackPauseResume();
			}
		});
		next = (ImageButton) findViewById(R.id.next);
		next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mPlayer.mPlaylist.playNext();
			}
		});
		previous = (ImageButton) findViewById(R.id.prev);
		previous.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mPlayer.mPlaylist.playPrevious();
			}
		});
		previous = (ImageButton) findViewById(R.id.prev);
		previous.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mPlayer.mPlaylist.playPrevious();
			}
		});

		repeat = (ImageButton) findViewById(R.id.repeat);
		repeat.setImageResource(R.drawable.ic_mp_repeat_off_btn);
		repeat.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mPlayer != null) {
					mPlayer.mPlaylist.toggleRepeat();
					setRepeatButtonImage();
				}
			}
		});

		shuffle = (ImageButton) findViewById(R.id.shuffle);
		shuffle.setImageResource(R.drawable.ic_mp_shuffle_off_btn);
		shuffle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mPlayer != null) {
					mPlayer.mPlaylist.toggleShuffle();
					setToggleButtonImage();
				}
			}
		});

		gestureDetector = new GestureDetector(new MyGestureDetector());
		gestureListener = new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (gestureDetector.onTouchEvent(event)) {
					return true;
				}
				return false;
			}
		};

		((GestureOverlayView) findViewById(R.id.gestures))
				.setOnTouchListener(gestureListener);

		(new AsyncUIUpdate()).start();
	}

	private void setRepeatButtonImage() {
		REPEAT_MODE rm = mPlayer.mPlaylist.getRepeatMode();
		switch (rm) {
		case Off:
			repeat.setImageResource(R.drawable.ic_mp_repeat_off_btn);
			break;
		case All:
			repeat.setImageResource(R.drawable.ic_mp_repeat_all_btn);
			break;
		case Once:
			repeat.setImageResource(R.drawable.ic_mp_repeat_once_btn);
			break;
		}
	}

	private void setToggleButtonImage() {
		if (mPlayer.mPlaylist.shuffleEnabled()) {
			shuffle.setImageResource(R.drawable.ic_mp_shuffle_on_btn);
		} else {
			shuffle.setImageResource(R.drawable.ic_mp_shuffle_off_btn);
		}
	}

	private void setPlayPauseButtonImage() {
		if (mPlayer.isPlaying()) {
			playpause.setImageResource(R.drawable.ic_media_pause);
		} else {
			playpause.setImageResource(R.drawable.ic_media_play);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		startService(new Intent(PlayingActivity.this, PlayerService.class));
		bindService(new Intent(PlayingActivity.this, PlayerService.class),
				mPlayerConnection, 0);
	}

	@Override
	protected void onDestroy() {
		try {
			unbindService(mPlayerConnection);
		} catch (Exception e) {
		}
		super.onDestroy();
	}

	private class MyPlayerListener extends PlayerService.PlayerListener {

		private Song mSong;

		public MyPlayerListener() {
			onPlayerStopped();
		}

		private void setCover() {
			if (mSong != null) {
				ArtworkAsyncHelper.updateArtwork(PlayingActivity.this, artwork,
						mSong.art, R.drawable.albumart_mp_unknown,
						mArtworkWidth, mArtWorkHeight, false);
			}
		}

		@Override
		public void onBuffering(int buffer) {
		}

		@Override
		public void onPlayerStopped() {
			mSong = null;
			mTrackName.setText("No Playing.");
			mArtistName.setText("");
			mAlbumName.setText("");
			artwork.setImageResource(R.drawable.albumart_mp_unknown);
		}

		@Override
		public void onNewSongPlaying(Song song) {
			mSong = song;
			mTrackName.setText(mSong.name);
			mArtistName.setText(mSong.artist);
			mAlbumName.setText(mSong.album);
			setCover();
		}

		@Override
		public void onTogglePlaying(boolean playing) {
			setPlayPauseButtonImage();

		}
	}

	private void setProgress() {
		if (isCurrentlySeek) {
			return;
		}
		int duration = 0;
		int position = -1;
		if (mPlayer != null) {
			position = mPlayer.getCurrentPosition();
			duration = mPlayer.getDuration();
		}
		if (duration > 0) {
			long pos = 1000L * position / duration;
			mProgress.setProgress((int) pos);
			int percent = mPlayer.getBuffer();
			mProgress.setSecondaryProgress(percent * 10);
			mTimeView.setText(Utils.stringForTime(position));
			mDurationView.setText(Utils.stringForTime(duration));
		} else {
			mProgress.setProgress(0);
			mProgress.setSecondaryProgress(0);
			mTimeView.setText(Utils.stringForTime(0));
			mDurationView.setText(Utils.stringForTime(0));
		}
	}

	private class AsyncUIUpdate extends Handler {

		public void start() {
			super.sendEmptyMessage(0);
		}

		@Override
		public void handleMessage(Message msg) {
			setProgress();
			msg = obtainMessage(0);
			sendMessageDelayed(msg, 1000);
		}
	}

	private class MySeekBarListener implements SeekBar.OnSeekBarChangeListener {

		@Override
		public void onStopTrackingTouch(SeekBar seek) {
			isCurrentlySeek = false;
		}

		@Override
		public void onStartTrackingTouch(SeekBar seek) {
			isCurrentlySeek = true;
		}

		@Override
		public void onProgressChanged(SeekBar seekbar, int progress,
				boolean fromUser) {
			if (fromUser && mPlayer != null && mPlayer.isSeekable()) {
				int duration = mPlayer.getDuration();
				int seek = (int) (progress * duration / 1000L);
				int preloaded = mPlayer.getBuffer() * duration;
				Log.d(TAG, "seek: " + Utils.stringForTime(seek) + " buf "
						+ mPlayer.getBuffer() + " preload: "
						+ Utils.stringForTime(preloaded));
				if (seek <= preloaded) {
					Log.d(TAG, "Seeking to " + Utils.stringForTime(seek));
					mPlayer.doSeekTo(seek);
				}
			}
		}
	}

	class MyGestureDetector extends SimpleOnGestureListener {
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			try {
				if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
					return false;
				// right to left swipe
				if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
						&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					Toast.makeText(PlayingActivity.this,
							"Playing previous song", Toast.LENGTH_SHORT).show();
					mPlayer.mPlaylist.playPrevious();
					return true;
				} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
						&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					Toast.makeText(PlayingActivity.this, "Playing next song",
							Toast.LENGTH_SHORT).show();
					mPlayer.mPlaylist.playNext();
					return true;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}
	}
}
