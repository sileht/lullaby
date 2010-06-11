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

import net.sileht.lullaby.R;
import net.sileht.lullaby.backend.Player;
import net.sileht.lullaby.objects.Song;

import android.app.ActivityGroup;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActivityGroup {

	// private static final int SWIPE_MAX_OFF_PATH = 250; // initial value
	private static final int SWIPE_MAX_OFF_PATH = 300;
	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;

	private GestureDetector gestureDetector;
	View.OnTouchListener gestureListener;

	private BitmapDrawable mDefaultAlbumIcon;

	public boolean mSeekbarEnabled = false;

	private TextView line1;
	private TextView line2;
	private ImageButton playpause;
	private ImageButton next;
	private ImageButton previous;
	private ImageView artwork;
	private TextView mTimeView;
	private TextView mDurationView;
	private SeekBar mProgress;
	private FrameLayout bottombar;

	private boolean isCurrentlySeek = false;
	private boolean isConnectedToAmpache = false;
	private boolean isConnectionToAmpacheFailed = false;

	private boolean hasAlreadyCheckConfigured = false;
	private static final String TAG = "DroidZikMainActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.main);

		if (! Lullaby.comm.isConfigured()){
			startActivity(new Intent().setClass(this, SettingActivity.class));
		} else {
			Lullaby.comm.ping();
		}
		
		Resources res = getResources(); // Resource object to get Drawables

		mDefaultAlbumIcon = (BitmapDrawable) res
				.getDrawable(R.drawable.albumart_mp_unknown_list);
		// no filter or dither, it's a lot faster and we can't tell the
		// difference
		mDefaultAlbumIcon.setFilterBitmap(false);
		mDefaultAlbumIcon.setDither(false);

		Log.v(TAG, "Start cover thread");
		Lullaby.cover.setDrawable(mDefaultAlbumIcon);

		TabHost tabHost = (TabHost) findViewById(R.id.tabhost); // The activity
		// TabHost
		tabHost.setup(this.getLocalActivityManager());

		TabHost.TabSpec spec; // Resusable TabSpec for each tab
		Intent intent; // Reusable Intent for each tab

		intent = new Intent().setClass(this, PlayingPlaylistActivity.class);
		spec = tabHost.newTabSpec("playback").setIndicator("Playing",
				res.getDrawable(R.drawable.ic_tab_playback)).setContent(intent);
		tabHost.addTab(spec);

		//  Artist
		intent = new Intent().setClass(this, ArtistAlbumsActivity.class);
		spec = tabHost.newTabSpec("artists").setIndicator("Artists",
				res.getDrawable(R.drawable.ic_tab_artists)).setContent(intent);
		tabHost.addTab(spec);

		intent = new Intent().setClass(this, AlbumActivity.class);
		spec = tabHost.newTabSpec("albums").setIndicator("Albums",
				res.getDrawable(R.drawable.ic_tab_albums)).setContent(intent);
		tabHost.addTab(spec);

		/*intent = new Intent().setClass(this, SongsActivity.class);
		spec = tabHost.newTabSpec("songs").setIndicator("Songs",
				res.getDrawable(R.drawable.ic_tab_songs)).setContent(intent);
		tabHost.addTab(spec);*/

		intent = new Intent().setClass(this, PlaylistsActivity.class);
		spec = tabHost.newTabSpec("playlist").setIndicator("Playlists",
				res.getDrawable(R.drawable.ic_tab_playlists)).setContent(intent);
		tabHost.addTab(spec);

		tabHost.setCurrentTab(0);

		line1 = (TextView) findViewById(R.id.line1);
		line2 = (TextView) findViewById(R.id.line2);

		gestureDetector = new GestureDetector(new MyGestureDetector());
		gestureListener = new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (gestureDetector.onTouchEvent(event)) {
					return true;
				}
				return false;
			}
		};

		bottombar = (FrameLayout) findViewById(R.id.bottombar);
		bottombar.setOnTouchListener(gestureListener);
		tabHost.setOnTouchListener(gestureListener);

		artwork = (ImageView) findViewById(R.id.icon);
		artwork.setBackgroundDrawable(mDefaultAlbumIcon);
		artwork.setImageDrawable(null);

		playpause = (ImageButton) findViewById(R.id.pause);
		playpause.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Lullaby.mp.doPauseResume();
			}
		});
		next = (ImageButton) findViewById(R.id.next);
		next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Lullaby.pl.playNext();
			}
		});
		previous = (ImageButton) findViewById(R.id.previous);
		previous.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Lullaby.pl.playPrevious();
			}
		});

		mTimeView = (TextView) findViewById(R.id.time_current);
		mTimeView.setText(Utils.stringForTime(0));

		mDurationView = (TextView) findViewById(R.id.time);
		mDurationView.setText(Utils.stringForTime(0));

		mProgress = (SeekBar) findViewById(R.id.mediacontroller_progress);
		mProgress.setMax(1000);
		mProgress.setProgress(0);
		mProgress.setSecondaryProgress(0);
		mProgress.setOnSeekBarChangeListener(new MySeekBarListener());
		
		Lullaby.mp.setPlayerListener(new MyPlayerListener());

		(new AsyncUIUpdate()).start();

		Lullaby.pl.load(this);
	}
	
	protected void onResume(){
		super.onResume();
		Lullaby.mp.hideNotification();
	}

	protected void onPause(){
		Lullaby.mp.showNotification();
		super.onPause();
	}

	private void checkConnection() {
		boolean isConnectedToAmpacheTest = (Lullaby.comm.authToken != null && !Lullaby.comm.authToken
				.equals(""));

		if (!isConnectedToAmpache && isConnectedToAmpacheTest) {
			isConnectionToAmpacheFailed = false;
			Toast.makeText(this, "Connected to Ampache.", Toast.LENGTH_LONG)
					.show();
		}
		if (isConnectedToAmpache && !isConnectedToAmpacheTest) {
			Toast.makeText(this, "Ampache connection lost.", Toast.LENGTH_LONG)
					.show();
			isConnectionToAmpacheFailed = true;
		}
		if (Lullaby.comm.hasAlreadyTryHandshake
				&& !isConnectionToAmpacheFailed && !isConnectedToAmpache
				&& !isConnectedToAmpacheTest) {
			Toast.makeText(this,
					"Ampache connection failed.\nCheck your settings.",
					Toast.LENGTH_LONG).show();
			isConnectionToAmpacheFailed = true;
		}

		if (!hasAlreadyCheckConfigured && !Lullaby.comm.isConfigured()) {
			Toast.makeText(this,
					"Ampache not configured.\nCheck your settings.",
					Toast.LENGTH_LONG).show();
			hasAlreadyCheckConfigured = true;
		}

		isConnectedToAmpache = isConnectedToAmpacheTest;
	}

	@Override
	protected void onStop() {
		Lullaby.pl.save(this);
		super.onStop();
	}

	private void setProgress() {
		if (isCurrentlySeek) {
			return;
		}
		int position = Lullaby.mp.getCurrentPosition();
		int duration = Lullaby.mp.getDuration();
		if (duration > 0) {
			long pos = 1000L * position / duration;
			mProgress.setProgress((int) pos);
			int percent = Lullaby.mp.getBuffer();
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

	public void SeekExpanderClick(View v) {
		View seek = findViewById(R.id.seekbar);
		View control = findViewById(R.id.control);
		if (seek.getVisibility() == View.VISIBLE) {
			seek.setVisibility(View.GONE);
			control.invalidate();
		} else {
			seek.setVisibility(View.VISIBLE);
		}
	}

	private static final int MENU_SETTINGS = 0;
	private static final int MENU_CLEAR = MENU_SETTINGS + 1;
	private static final int MENU_SAVE = MENU_CLEAR + 1;
	private static final int MENU_LOAD = MENU_SAVE + 1;
	private static final int MENU_EXIT = MENU_LOAD + 1;
	private static final int MENU_SHUFFLE = MENU_EXIT + 1;
	private static final int MENU_REPEAT = MENU_SHUFFLE + 1 ;
	private static final int MENU_CLEARCACHE = MENU_REPEAT + 1 ;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_SHUFFLE, 0, "Shuffle").setIcon(
				R.drawable.ic_menu_shuffle);
		
		menu.add(0, MENU_REPEAT, 0, "Repeat").setIcon(
				R.drawable.ic_mp_repeat_off_btn);
		
		menu.add(0, MENU_CLEAR, 0, "Clear playlist").setIcon(
				android.R.drawable.ic_menu_close_clear_cancel);

		menu.add(0, MENU_SAVE, 0, "Save playlist").setIcon(
				android.R.drawable.ic_menu_save);
		menu.add(0, MENU_LOAD, 0, "Load playlist").setIcon(
				android.R.drawable.ic_menu_edit);

		menu.add(0, MENU_SETTINGS, 0, "Settings").setIcon(
				android.R.drawable.ic_menu_preferences);
		
		menu.add(0, MENU_CLEARCACHE, 0, "Clear cache").setIcon(
				android.R.drawable.ic_menu_close_clear_cancel);

		menu.add(0, MENU_EXIT, 0, "Exit").setIcon(
				android.R.drawable.ic_menu_close_clear_cancel);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent = null;
		switch (item.getItemId()) {
		case MENU_REPEAT:
			Lullaby.pl.toggleRepeat();
			if (Lullaby.pl.isRepeat()){
				item.setIcon(
						R.drawable.ic_mp_repeat_all_btn);
			} else {
				item.setIcon(
						R.drawable.ic_mp_repeat_off_btn);
			}
			break;
		case MENU_SHUFFLE:
			Lullaby.pl.shuffle();
			break;
		case MENU_SETTINGS:
			intent = new Intent().setClass(this, SettingActivity.class);
			break;
		case MENU_CLEAR:
			Lullaby.pl.clear();
			break;
		case MENU_SAVE:
			Lullaby.pl.save(this);
			break;
		case MENU_LOAD:
			Lullaby.pl.load(this);
			break;
		case MENU_CLEARCACHE:
			(new File(Environment.getExternalStorageDirectory(),
					"Android/data/com.sileht.lullaby/cache")).delete();
			break;
		case MENU_EXIT:
			finish();
			break;
		default:
			return false;
		}
		if (intent != null) {
			startActivity(intent);
			return true;
		}
		return false;
	}

	@Override
	public boolean onSearchRequested() {
		startSearch(null, false, null, false);
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {

			finish();
			return super.onKeyDown(keyCode, event);

		}
		return false;
	}

	private class MyPlayerListener extends Player.PlayerListener {

		private int mBuffering = -1;
		private Song mSong;
		
		public MyPlayerListener(){
			onPlayerStopped();
		}

		private void setLine1() {
			if (mSong != null) {
				String l1 = mSong.name;
				if (mBuffering >= 0 && mBuffering < 100) {
					l1 = l1 + " (" + mBuffering + "%)";
				}
				line1.setText(l1);
			}
		}

		private void setLine2() {
			if (mSong != null) {
				line2.setText(mSong.album + " - " + mSong.artist);
			}
		}

		private void setCover() {
			if (mSong != null) {
				artwork.setBackgroundDrawable(mDefaultAlbumIcon);
				artwork.setImageDrawable(null);
				Lullaby.cover.setCachedArtwork(artwork, mSong.art);
			}
		}

		@Override
		public void onBuffering(int buffer) {
			mBuffering = buffer;
			setLine1();
		}
		
		@Override
		public void onPlayerStopped() {
			mSong = null;
			line1.setText("No Playing.");
			line2.setText("");
			artwork.setBackgroundDrawable(mDefaultAlbumIcon);
			artwork.setImageDrawable(null);
		}

		@Override
		public void onNewSongPlaying(Song song) {
			mSong = song;
			setLine1();
			setLine2();
			setCover();
		}

		@Override
		public void onTogglePlaying(boolean playing) {
			// mSeekbarEnabled = playing;
			if (playing) {
				playpause.setImageResource(android.R.drawable.ic_media_pause);
			} else {
				playpause.setImageResource(android.R.drawable.ic_media_play);
			}
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
			if (fromUser && Lullaby.mp.isSeekable()) {
				int duration = Lullaby.mp.getDuration();
				int seek = (int) (progress * duration / 1000L);
				int preloaded = Lullaby.mp.getBuffer()  * duration;
				Log.d(TAG, "seek: " + Utils.stringForTime(seek) + " buf " + Lullaby.mp.getBuffer() + " preload: " + Utils.stringForTime(preloaded));
				if (seek <= preloaded){
					Log.d(TAG, "Seeking to " + Utils.stringForTime(seek));
					Lullaby.mp.seekTo(seek);
				}
			}
		}
	}

	private class AsyncUIUpdate extends Handler {

		public void start() {
			super.sendEmptyMessage(0);
		}

		@Override
		public void handleMessage(Message msg) {
			setProgress();
			checkConnection();
			msg = obtainMessage(0);
			sendMessageDelayed(msg, 1000);
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
					Toast.makeText(MainActivity.this, "Playing previous song",
							Toast.LENGTH_SHORT).show();
					Lullaby.pl.playPrevious();
					return true;
				} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
						&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					Toast.makeText(MainActivity.this, "Playing next song",
							Toast.LENGTH_SHORT).show();
					Lullaby.pl.playNext();
					return true;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}
	}
}
