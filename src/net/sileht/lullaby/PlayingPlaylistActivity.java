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

import net.sileht.lullaby.R;
import net.sileht.lullaby.backend.Player;
import net.sileht.lullaby.objects.Song;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
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
		View.OnCreateContextMenuListener {

	private PlayingPlaylistAdapter mAdapter;
	private ListView mView;

	//private static final String TAG = "LullabyPlayingPlaylist";

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.playing_playlist_activity);

		if (mAdapter == null) {
			mAdapter = new PlayingPlaylistAdapter(this);
		}
		setListAdapter(mAdapter);

		Lullaby.pl.setAdapter(mAdapter);

		mView = getListView();
		mView.setOnCreateContextMenuListener(this);

		((TouchInterceptor) mView)
				.setDropListener(new TouchInterceptor.DropListener() {
					public void drop(int from, int to) {
						Lullaby.pl.move(from, to);
					}
				});

		Lullaby.mp.setPlayerListener(new MyPlayerListener());
	}

	private final static int MENU_PLAY_SELECTION = 0;
	private final static int MENU_DELETE_ITEM = 1;

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuinfo) {
		menu.add(0, MENU_PLAY_SELECTION, 0, "Play").setIcon(android.R.drawable.ic_media_play);
		menu.add(0, MENU_DELETE_ITEM, 0, "Delete").setIcon(android.R.drawable.ic_menu_delete);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo menuinfo = (AdapterView.AdapterContextMenuInfo) item
				.getMenuInfo();

		switch (item.getItemId()) {
		case MENU_PLAY_SELECTION:
			Lullaby.pl.play(menuinfo.position);
			return true;
		case MENU_DELETE_ITEM:
			Lullaby.pl.remove(menuinfo.position);
			return true;
		}
		return super.onContextItemSelected(item);

	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Lullaby.pl.play(position);
	}

	static class PlayingPlaylistAdapter extends BaseAdapter {
		private int mResource;
		private LayoutInflater mInflater;

		public PlayingPlaylistAdapter(Context context) {
			super();

			mResource = R.layout.edit_track_list_item;
			mInflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = mInflater.inflate(mResource, parent, false);

			Song s = Lullaby.pl.get(position);

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

			if ((Lullaby.pl.getCurrentPosition() == position)) {
				play_indicator
						.setImageResource(R.drawable.indicator_ic_mp_playing_list);
				play_indicator.setVisibility(View.VISIBLE);
			} else {
				play_indicator.setVisibility(View.GONE);
			}
			return v;
		}

		@Override
		public int getCount() {
			return Lullaby.pl.size();
		}

		@Override
		public Object getItem(int position) {
			return Lullaby.pl.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
	}

	private class MyPlayerListener extends Player.PlayerListener {

		@Override
		public void onBuffering(int buffer) {
			return;
		}

		@Override
		public void onNewSongPlaying(Song song) {
			int position = Lullaby.pl.getCurrentPosition();
			mView.setSelectionFromTop(position,0);			
		}

		@Override
		public void onTogglePlaying(boolean playing) {
			return;
		}

		@Override
		public void onPlayerStopped() {
			return;			
		}
		
	}
}
