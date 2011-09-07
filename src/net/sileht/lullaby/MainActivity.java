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

import net.sileht.lullaby.backend.ArtworkAsyncHelper;
import net.sileht.lullaby.objects.Song;
import net.sileht.lullaby.player.PlayerService;
import android.app.ActivityGroup;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.gesture.GestureOverlayView;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActivityGroup implements
		PlayerService.OnStatusListener {

	static final String TAG = "LullabyMainActivity";

	// private static String PREFS_NAME = "LullabyPrefs";

	// private static final int SWIPE_MAX_OFF_PATH = 250; // initial value
	private static final int SWIPE_MAX_OFF_PATH = 300;
	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;

	private static int mArtworkWidth = -1;
	private static int mArtWorkHeight = -1;

	private GestureDetector gestureDetector;
	private View.OnTouchListener gestureListener;

	private TextView line1;
	private TextView line2;
	private ImageButton playpause;
	private ImageView artwork;
	private GestureOverlayView bottombar;
	private TabHost mTabHost;

	// private static final String TAG = "LullabyMainActivity";

	// Bind Service Player
	private boolean mIsBound;
	private PlayerService mPlayer;
	private ServiceConnection mPlayerConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			mPlayer = ((PlayerService.PlayerBinder) service).getService();
			mPlayer.setOnPlayerListener((PlayerService.OnStatusListener) MainActivity.this);

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

		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

		Utils.setSSLCheck(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("InsecureSSL", false));

		if (mArtworkWidth < 0) {
			Bitmap icon = ((BitmapDrawable) this.getResources().getDrawable(
					R.drawable.albumart_mp_unknown_list)).getBitmap();
			mArtworkWidth = icon.getWidth();
			mArtWorkHeight = icon.getHeight();
		}

		setContentView(R.layout.main);

		if (!Lullaby.comm.isConfigured()) {
			startActivity(new Intent().setClass(this, SettingActivity.class));
		} else {
			Lullaby.comm.ping();
		}

		Resources res = getResources(); // Resource object to get Drawables

		mTabHost = (TabHost) findViewById(R.id.tabhost); // The activity
		// TabHost
		mTabHost.setup(this.getLocalActivityManager());

		TabHost.TabSpec spec; // Resusable TabSpec for each tab
		Intent intent; // Reusable Intent for each tab

		intent = new Intent().setClass(this, PlayingPlaylistActivity.class);
		spec = mTabHost
				.newTabSpec("playback")
				.setIndicator(getResources().getString(R.string.playing),
						res.getDrawable(R.drawable.ic_tab_playback))
				.setContent(intent);
		mTabHost.addTab(spec);

		//  Artist
		intent = new Intent().setClass(this, ArtistAlbumsActivity.class);
		spec = mTabHost
				.newTabSpec("artists")
				.setIndicator(getResources().getString(R.string.artists),
						res.getDrawable(R.drawable.ic_tab_artists))
				.setContent(intent);
		mTabHost.addTab(spec);

		intent = new Intent().setClass(this, AlbumActivity.class);
		spec = mTabHost
				.newTabSpec("albums")
				.setIndicator(getResources().getString(R.string.albums),
						res.getDrawable(R.drawable.ic_tab_albums))
				.setContent(intent);
		mTabHost.addTab(spec);

		intent = new Intent().setClass(this, PlaylistsActivity.class);
		spec = mTabHost
				.newTabSpec("playlist")
				.setIndicator(getResources().getString(R.string.playlists),
						res.getDrawable(R.drawable.ic_tab_playlists))
				.setContent(intent);
		mTabHost.addTab(spec);

		mTabHost.setCurrentTab(0);

		line1 = (TextView) findViewById(R.id.line1);
		line2 = (TextView) findViewById(R.id.line2);

		artwork = (ImageView) findViewById(R.id.icon);
		artwork.setImageResource(R.drawable.albumart_mp_unknown_list);

		playpause = (ImageButton) findViewById(R.id.pause);
		playpause.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mPlayer.doPlaybackPauseResume();
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

		bottombar = (GestureOverlayView) findViewById(R.id.bottombar);
		bottombar.setOnTouchListener(gestureListener);
		bottombar.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent();
				i.setClass(v.getContext(), PlayingActivity.class);
				i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				startActivity(i);
			}
		});
		doBindService();
		updateBottomBar();
	}

	void doBindService() {
		// c.startService(new Intent(c, PlayerService.class));
		bindService(new Intent(this, PlayerService.class), mPlayerConnection,
				Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	void doUnbindService() {
		if (mIsBound) {
			// Detach our existing connection.
			unbindService(mPlayerConnection);
			mIsBound = false;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		doUnbindService();
	}

	@Override
	public boolean onSearchRequested() {
		startSearch(null, false, null, false);
		return true;
	}

	@Override
	public void onBuffering(int buffer) {
		updateBottomBar();
	}

	@Override
	public void onStatusChange() {
		updateBottomBar();
	}

	@Override
	public void onTogglePlaying(boolean playing) {
		if (playing) {
			playpause.setImageResource(R.drawable.ic_media_pause);
		} else {
			playpause.setImageResource(R.drawable.ic_media_play);
		}
	}

	@Override
	public void onTick(int position, int duration, int buffer) {
	}

	private void updateBottomBar() {
		if (mPlayer != null) {
			Song s = mPlayer.getSong();
			int buffer = mPlayer.getBuffer();
			if (s != null) {
				String l1 = s.name;
				if (buffer >= 0 && buffer < 100) {
					l1 = l1 + " (" + buffer + "%)";
				}
				line1.setText(l1);
				line2.setText(s.album + " - " + s.artist);
				ArtworkAsyncHelper.updateArtwork(MainActivity.this, artwork,
						s.art, R.drawable.albumart_mp_unknown_list,
						mArtworkWidth, mArtWorkHeight, false);
				return;
			}
		}
		
		line1.setText(getResources().getString(R.string.no_playing));
		line2.setText("");
		artwork.setImageResource(R.drawable.albumart_mp_unknown_list);

	}

	private static final int MENU_SETTINGS = 0;
	private static final int MENU_CLEAR = MENU_SETTINGS + 1;
	private static final int MENU_SAVE = MENU_CLEAR + 1;
	private static final int MENU_LOAD = MENU_SAVE + 1;
	private static final int MENU_EXIT = MENU_LOAD + 1;
	private static final int MENU_CLEARCACHE = MENU_EXIT + 1;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Resources r = getResources();
		super.onCreateOptionsMenu(menu);
		if (mTabHost.getCurrentTab() == 0) {
			menu.add(0, MENU_CLEAR, 0,
					r.getString(R.string.Menu_clear_playlist)).setIcon(
					android.R.drawable.ic_menu_close_clear_cancel);
			/*
			 * menu.add(0, MENU_SAVE, 0,
			 * r.getString(R.string.Menu_save_playlist)).setIcon(
			 * android.R.drawable.ic_menu_save); menu.add(0, MENU_LOAD, 0,
			 * r.getString(R.string.Menu_load_playlist)).setIcon(
			 * android.R.drawable.ic_menu_edit);
			 */
		}
		menu.add(0, MENU_SETTINGS, 0, r.getString(R.string.Menu_settings))
				.setIcon(android.R.drawable.ic_menu_preferences);

		menu.add(0, MENU_CLEARCACHE, 0, r.getString(R.string.Menu_refresh))
				.setIcon(android.R.drawable.ic_menu_close_clear_cancel);

		menu.add(0, MENU_EXIT, 0, r.getString(R.string.Menu_exit)).setIcon(
				android.R.drawable.ic_menu_close_clear_cancel);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent = null;
		switch (item.getItemId()) {
		case MENU_SETTINGS:
			intent = new Intent().setClass(this, SettingActivity.class);
			break;
		case MENU_CLEAR:
			if (mPlayer != null)
				mPlayer.mPlaylist.clear();
			break;
		case MENU_SAVE:
			if (mPlayer != null)
				mPlayer.mPlaylist.save(this);
			break;
		case MENU_LOAD:
			if (mPlayer != null)
				mPlayer.mPlaylist.load(this);
			break;
		case MENU_CLEARCACHE:
			showDialog(0);

			return true;
		case MENU_EXIT:
			onDestroy();
			stopService(new Intent(MainActivity.this, PlayerService.class));
			finish();
			return true;
		default:
			return false;
		}
		if (intent != null) {
			startActivity(intent);
			return true;
		}
		return false;
	}

	protected Dialog onCreateDialog(int id) {
		Resources r = getResources();
		final CharSequence[] items = {
				r.getString(R.string.Menu_dialog_refresh_view),
				r.getString(R.string.Menu_dialog_refresh_all) };

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(r.getString(R.string.Menu_dialog_refresh_title));
		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				clearCache((item == 1));
			}
		});
		return builder.create();
	}

	private void clearCache(boolean all) {

		File root = Utils.getCacheRootDir();
		for (File f : root.listFiles()) {
			if (!all && f.toString().startsWith("stream-")) {
				continue;
			}
			f.delete();
		}

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean("clear_artists", true);
		editor.putBoolean("clear_albums", true);
		editor.putBoolean("clear_playlist", true);
		editor.commit();
		Log.v(TAG, "setPref:clear=" + mTabHost.getCurrentTabTag());

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
					Toast.makeText(
							MainActivity.this,
							getResources().getString(R.string.playing_previous),
							Toast.LENGTH_SHORT).show();
					mPlayer.mPlaylist.playPrevious();
					return true;
				} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
						&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					Toast.makeText(
							MainActivity.this,
							getResources().getString(R.string.playing_previous),
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
