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

import net.sileht.lullaby.objects.Song;
import net.sileht.lullaby.player.PlayerService;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class PlayingPlaylistActivity extends ListActivity implements
		View.OnCreateContextMenuListener, PlayerService.OnStatusListener {

	private PlayingPlaylistAdapter mAdapter;
	private ListView mView;

	// private static final String TAG = "LullabyPlayingPlaylist";

	// Bind Service Player       
	private boolean mIsBound;
	private PlayerService mPlayer;
	private ServiceConnection mPlayerConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			mPlayer = ((PlayerService.PlayerBinder) service).getService();
			mPlayer.mPlaylist.setAdapter(mAdapter);
			mPlayer
					.setOnPlayerListener((PlayerService.OnStatusListener) PlayingPlaylistActivity.this);
		}

		public void onServiceDisconnected(ComponentName className) {
			mPlayer = null;
		}
	};

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.playing_playlist_activity);

		if (mAdapter == null) {
			mAdapter = new PlayingPlaylistAdapter(this);
		}
		setListAdapter(mAdapter);

		mView = getListView();
		mView.setOnCreateContextMenuListener(this);

		((TouchInterceptor) mView)
				.setDropListener(new TouchInterceptor.DropListener() {
					public void drop(int from, int to) {
						mPlayer.mPlaylist.move(from, to);
					}
				});
		doBindService();
	}

	void doBindService() {
		Context c = this.getParent();
		//c.startService(new Intent(c, PlayerService.class));
		c.bindService(new Intent(c, PlayerService.class), mPlayerConnection,
				Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	void doUnbindService() {
		if (mIsBound) {
			Context c = this.getParent();
			// Detach our existing connection.
			c.unbindService(mPlayerConnection);
			mIsBound = false;
		}
	}

	@Override
	protected void onDestroy() {
		doUnbindService();
		super.onDestroy();
	}

	public void onBuffering(int buffer) {
	}

	public void onTogglePlaying(boolean playing) {
	}

	public void onStatusChange() {
		int position = mPlayer.mPlaylist.getCurrentPosition();
		mView.setSelectionFromTop(position, 0);
	}

	public void onTick(int position, int duration, int buffer) {
	}

	static class PlayingPlaylistAdapter extends BaseAdapter {
		private int mResource;
		private LayoutInflater mInflater;
		private PlayingPlaylistActivity mContext;

		public PlayingPlaylistAdapter(PlayingPlaylistActivity context) {
			super();
			mContext = context;
			mResource = R.layout.edit_track_list_item;
			mInflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View v = mInflater.inflate(mResource, parent, false);

			Song s = mContext.mPlayer.mPlaylist.get(position);

			ImageView iv = (ImageView) v.findViewById(R.id.icon);
			iv.setVisibility(View.VISIBLE);
			iv.setImageResource(R.drawable.ic_mp_move);

			TextView line1 = (TextView) v.findViewById(R.id.line1);
			TextView line2 = (TextView) v.findViewById(R.id.line2);
			TextView duration = (TextView) v.findViewById(R.id.duration);
			ImageView play_indicator = (ImageView) v
					.findViewById(R.id.play_indicator);

			line1.setText(s.name);
			line2.setText(s.album + " - " + s.artist);
			duration.setText(Utils.stringForTime(s.time));

			if ((mContext.mPlayer.mPlaylist.getCurrentPosition() == position)) {
				play_indicator
						.setImageResource(R.drawable.indicator_ic_mp_playing_list);
				play_indicator.setVisibility(View.VISIBLE);
			} else {
				play_indicator.setVisibility(View.GONE);
			}
			return v;
		}

		public int getCount() {
			if (mContext.mPlayer == null) {
				return 0;
			} else {
				return mContext.mPlayer.mPlaylist.size();
			}
		}

		public Object getItem(int position) {
			if (mContext.mPlayer == null) {
				return null;
			} else {
				return mContext.mPlayer.mPlaylist.get(position);
			}
		}

		public long getItemId(int position) {
			return position;
		}
	}

	private final static int MENU_PLAY_SELECTION = 0;
	private final static int MENU_DELETE_ITEM = 1;

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		mPlayer.mPlaylist.play(position);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuinfo) {
		menu.add(0, MENU_PLAY_SELECTION, 0, getResources().getString(R.string.play)).setIcon(
				android.R.drawable.ic_media_play);
		menu.add(0, MENU_DELETE_ITEM, 0,  getResources().getString(R.string.delete)).setIcon(
				android.R.drawable.ic_menu_delete);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo menuinfo = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();
		if (mPlayer != null) {
			switch (item.getItemId()) {
			case MENU_PLAY_SELECTION:
				mPlayer.mPlaylist.play(menuinfo.position);
				return true;
			case MENU_DELETE_ITEM:
				mPlayer.mPlaylist.remove(menuinfo.position);
				return true;
			}
		}
		return super.onContextItemSelected(item);
	}
}
