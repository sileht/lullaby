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
import java.util.ArrayList;

import net.sileht.lullaby.R;
import net.sileht.lullaby.objects.Song;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AlphabetIndexer;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class SongsActivity extends Activity {

	private MatrixCursor songsData;

	private SimpleCursorAdapter mAdapter;

	private Boolean mStandAloneActivity = false;

	static class ViewHolder {
		TextView line1;
		TextView line2;
		ImageView play_indicator;
		ImageView icon;
		TextView duration;
	}

	private AmpacheRequest request;
	ViewUtils mViewUtils;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String[] directive = new String[] { "songs", "" };

		Intent intent = getIntent();
		String queryAction = intent.getAction();

		if (intent != null && intent.hasExtra("type") && intent.hasExtra("id")
				&& intent.hasExtra("title")) {
			String type = (String) intent.getExtras().get("type");
			String id = (String) intent.getExtras().get("id");
			String title = (String) intent.getExtras().get("title");
			directive = new String[] { type, id };
			setTitle(title);
			mStandAloneActivity = true;
		} else if (Intent.ACTION_SEARCH.equals(queryAction)) {
			String searchKeywords = intent.getStringExtra(SearchManager.QUERY);
			directive = new String[] { "search_songs", searchKeywords };
			setTitle("Search result for '" + searchKeywords + "'");
		}

		setContentView(R.layout.list_classic);

		ListView lv = (ListView) findViewById(R.id.list);
		
		mViewUtils = new ViewUtils(this);
		lv.setOnItemClickListener(mViewUtils);
		lv.setOnItemLongClickListener(mViewUtils);

		if (songsData == null) {
			// Tell them we're loading

			setProgressBarVisibility(true);

			songsData = new MatrixCursor(ViewUtils.mSongsColumnName);
			startManagingCursor(songsData);
			request = new AmpacheRequest((Activity) this, directive) {
				@SuppressWarnings("unchecked")
				@Override
				public void add_objects(ArrayList list) {
					for (Song song : (ArrayList<Song>) list) {
						// SONG_ID,SONG_NAME, SONG_SONG, SONG_ARTIST, SONG_URL,
						// SONG_DURATION, SONG_EXTRA
						songsData.newRow().add(song.id).add(song.name).add(
								song.album).add(song.artist).add(song.url).add(
								song.time)
								.add(song.album + " - " + song.artist);
						songsData.requery();
					}
				}
			};
			request.send();
		}
		mAdapter = getNewAdapter();
		lv.setAdapter(mAdapter);
	}


	protected void onStop(){
		request.stop();
		mViewUtils.onStop();
		super.onStop();
	}
	

	private SimpleCursorAdapter getNewAdapter() {
		int r;
		if (!mStandAloneActivity) {
			r = R.layout.track_list_item_child;
		} else {
			r = R.layout.track_list_item_mini;
		}
		return new SongsAdapter(this, this, songsData, r, new String[] {},
				new int[] {});
	}

	static class SongsAdapter extends SimpleCursorAdapter implements
			SectionIndexer {

		private AlphabetIndexer mIndexer;
		private SongsActivity mCurrentActivity;

		public SongsAdapter(Context context, SongsActivity activity,
				Cursor cursor, int layout, String[] from, int[] to) {
			super(context, layout, cursor, from, to);

			mCurrentActivity = activity;
			Resources r = context.getResources();
			mIndexer = new AlphabetIndexer(cursor, cursor
					.getColumnIndex(ViewUtils.SONG_NAME), r
					.getString(R.string.fast_scroll_numeric_alphabet));
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View v = super.newView(context, cursor, parent);
			ViewHolder vh = new ViewHolder();
			vh.line1 = (TextView) v.findViewById(R.id.line1);
			vh.line2 = (TextView) v.findViewById(R.id.line2);
			vh.play_indicator = (ImageView) v.findViewById(R.id.play_indicator);
			vh.icon = (ImageView) v.findViewById(R.id.icon);
			vh.duration = (TextView) v.findViewById(R.id.duration);
			v.setTag(vh);
			return v;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {

			String name = cursor.getString(cursor
					.getColumnIndexOrThrow(ViewUtils.SONG_NAME));

			String extra = cursor.getString(cursor
					.getColumnIndexOrThrow(ViewUtils.SONG_EXTRA));

			String time = cursor.getString(cursor
					.getColumnIndexOrThrow(ViewUtils.SONG_DURATION));

			String displayname = name;
			boolean unknown = name == null;
			if (unknown) {
				displayname = "Unknown";
			}

			ViewHolder vh = (ViewHolder) view.getTag();
			vh.line1.setText(displayname);
			if (!mCurrentActivity.mStandAloneActivity) {
				vh.line2.setText(extra);
			} else {
				vh.line2.setVisibility(View.GONE);
			}
			vh.icon.setImageDrawable(null);
			vh.play_indicator.setImageDrawable(null);
			vh.duration.setText(Utils.stringForTime(time));

		}

		@Override
		public Object[] getSections() {
			return mIndexer.getSections();
		}

		@Override
		public int getPositionForSection(int sectionIndex) {
			return mIndexer.getPositionForSection(sectionIndex);
		}

		@Override
		public int getSectionForPosition(int position) {
			return 0;
		}
	}
}
