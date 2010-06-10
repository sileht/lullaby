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
import java.util.HashMap;

import net.sileht.lullaby.R;
import net.sileht.lullaby.objects.Album;
import net.sileht.lullaby.objects.Artist;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AlphabetIndexer;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;

public class ArtistAlbumsActivity extends Activity {

	private static String TAG = "LullabyArtistAlbumsActivity";

	private MatrixCursor artistsData;
	private HashMap<String, MatrixCursor> albumsData;

	private SimpleCursorTreeAdapter mAdapter;
	private ExpandableListView mListView;

	private ViewUtils mVU = new ViewUtils(this);

	static class ViewHolder {
		TextView line1;
		TextView line2;
		ImageView play_indicator;
		ImageView icon;
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.list_expandable);

		mListView = (ExpandableListView) findViewById(R.id.list);
		AlbumClickListener albumCL = new AlbumClickListener();
		mListView.setOnChildClickListener(albumCL);
		mListView.setOnGroupExpandListener(albumCL);
		mListView.setOnItemLongClickListener(mVU);
		mListView.setOnCreateContextMenuListener(mVU);

		if (artistsData == null || albumsData == null) {
			// Tell them we're loading

			setProgressBarVisibility(true);

			artistsData = new MatrixCursor(ViewUtils.mArtistColumnName);
			albumsData = new HashMap<String, MatrixCursor>();
			startManagingCursor(artistsData);

			AmpacheRequest request = new AmpacheRequest((Activity) this,
					new String[] { "artists", "" }) {
				@SuppressWarnings("unchecked")
				@Override
				public void add_objects(ArrayList list) {
					for (Artist artist : (ArrayList<Artist>) list) {
						artistsData.newRow().add(artist.id).add(artist.name)
								.add(artist.albums).add(artist.tracks);
						artistsData.requery();
					}
				}
			};
			request.send();
		}
		mAdapter = getNewAdapter();
		mListView.setAdapter(mAdapter);

	}

	private class AlbumClickListener implements
			ExpandableListView.OnChildClickListener, ExpandableListView.OnGroupExpandListener {
		@Override
		public boolean onChildClick(ExpandableListView l, View v,
				int gposition, int cposition, long id) {

			Cursor gcursor = (Cursor) l.getItemAtPosition(gposition);
			Cursor cursor = (Cursor) albumsData.get(gcursor.getString(gcursor
					.getColumnIndexOrThrow(ViewUtils.ARTIST_ID)));

			String albumId = cursor.getString(cursor
					.getColumnIndexOrThrow(ViewUtils.ALBUM_ID));
			String album = cursor.getString(cursor
					.getColumnIndexOrThrow(ViewUtils.ALBUM_NAME));
			String artist = cursor.getString(cursor
					.getColumnIndexOrThrow(ViewUtils.ALBUM_ARTIST));

			mVU.startSongsActivity("album_songs", albumId, album + " - "
					+ artist);
			return false;
		}

		@Override
		public void onGroupExpand(int position) {
			mListView.setSelectedGroup(position);
		}
	}

	private SimpleCursorTreeAdapter getNewAdapter() {
		return new ArtistAlbumsAdapter(this, this, artistsData,
				R.layout.track_list_item_group, new String[] {}, new int[] {},
				R.layout.track_list_item_child, new String[] {}, new int[] {});
	}

	static class ArtistAlbumsAdapter extends SimpleCursorTreeAdapter implements
			SectionIndexer {

		private final BitmapDrawable mDefaultAlbumIcon;
		private final StringBuilder mBuffer = new StringBuilder();
		private final ArtistAlbumsActivity mCurrentActivity;

		private AlphabetIndexer mIndexer;

		public ArtistAlbumsAdapter(Context context,
				ArtistAlbumsActivity currentactivity, Cursor cursor,
				int glayout, String[] gfrom, int[] gto, int clayout,
				String[] cfrom, int[] cto) {

			super(context, cursor, glayout, gfrom, gto, clayout, cfrom, cto);

			Resources r = context.getResources();
			mCurrentActivity = currentactivity;
			mDefaultAlbumIcon = (BitmapDrawable) r
					.getDrawable(R.drawable.albumart_mp_unknown_list);
			// no filter or dither, it's a lot faster and we can't tell the
			// difference
			mDefaultAlbumIcon.setFilterBitmap(false);
			mDefaultAlbumIcon.setDither(false);

			mIndexer = new AlphabetIndexer(cursor, cursor
					.getColumnIndex(ViewUtils.ARTIST_NAME), r
					.getString(R.string.fast_scroll_numeric_alphabet));
		}

		@Override
		protected Cursor getChildrenCursor(Cursor groupCursor) {

			String id = groupCursor.getString(groupCursor
					.getColumnIndex(ViewUtils.ARTIST_ID));
			String name = groupCursor.getString(groupCursor
					.getColumnIndex(ViewUtils.ARTIST_NAME));

			MatrixCursor cur;
			if (mCurrentActivity.albumsData.containsKey(id)) {
				cur = mCurrentActivity.albumsData.get(id);
			} else {
				cur = new MatrixCursor(ViewUtils.mAlbumsColumnName);
				mCurrentActivity.startManagingCursor(cur);
				mCurrentActivity.albumsData.put(id, cur);

				AmpacheRequest request = new AmpacheRequest(
						(Activity) mCurrentActivity, new String[] {
								"artist_albums", id }) {
					@SuppressWarnings("unchecked")
					@Override
					public void add_objects(ArrayList list) {
						for (Album album : (ArrayList<Album>) list) {
							MatrixCursor cur = ((ArtistAlbumsActivity) this.mCurrentActivity).albumsData
									.get(this.mDirective[1]);
							cur.newRow().add(album.id).add(album.name).add(
									album.artist).add(album.tracks).add(
									album.art);
							cur.requery();
						}
					}
				};
				request.send();
				Log.v(TAG, "Request albums for : " + name + " (" + id + ")");
			}

			return (Cursor) cur;
		}

		@Override
		public View newGroupView(Context context, Cursor cursor,
				boolean isExpanded, ViewGroup parent) {
			View v = super.newGroupView(context, cursor, isExpanded, parent);
			ImageView iv = (ImageView) v.findViewById(R.id.icon);
			ViewGroup.LayoutParams p = iv.getLayoutParams();
			p.width = ViewGroup.LayoutParams.WRAP_CONTENT;
			p.height = ViewGroup.LayoutParams.WRAP_CONTENT;
			ViewHolder vh = new ViewHolder();
			vh.line1 = (TextView) v.findViewById(R.id.line1);
			vh.line2 = (TextView) v.findViewById(R.id.line2);
			vh.play_indicator = (ImageView) v.findViewById(R.id.play_indicator);
			vh.icon = (ImageView) v.findViewById(R.id.icon);
			vh.icon.setPadding(0, 0, 1, 0);
			v.setTag(vh);
			return v;
		}

		@Override
		public View newChildView(Context context, Cursor cursor,
				boolean isLastChild, ViewGroup parent) {
			View v = super.newChildView(context, cursor, isLastChild, parent);
			ViewHolder vh = new ViewHolder();
			vh.line1 = (TextView) v.findViewById(R.id.line1);
			vh.line2 = (TextView) v.findViewById(R.id.line2);
			vh.play_indicator = (ImageView) v.findViewById(R.id.play_indicator);
			vh.icon = (ImageView) v.findViewById(R.id.icon);
			vh.icon.setBackgroundDrawable(mDefaultAlbumIcon);
			vh.icon.setPadding(0, 0, 1, 0);
			v.setTag(vh);
			return v;
		}

		@Override
		public void bindGroupView(View view, Context context, Cursor cursor,
				boolean isexpanded) {

			ViewHolder vh = (ViewHolder) view.getTag();

			String artist = cursor.getString(cursor
					.getColumnIndexOrThrow(ViewUtils.ARTIST_NAME));
			String displayartist = artist;
			boolean unknown = artist == null;
			if (unknown) {
				displayartist = "Unknown";
			}
			vh.line1.setText(displayartist);

			int numalbums = cursor.getInt(cursor
					.getColumnIndexOrThrow(ViewUtils.ARTIST_ALBUMS));
			int numsongs = cursor.getInt(cursor
					.getColumnIndexOrThrow(ViewUtils.ARTIST_TRACKS));

			String songs_albums = "" + numalbums + " albums and " + numsongs
					+ " songs";

			vh.line2.setText(songs_albums);

			vh.play_indicator.setImageDrawable(null);
			/*
			 * long currentartistid = 0; long artistid = cursor.getLong(0); if
			 * (currentartistid == artistid && !isexpanded) {
			 * vh.play_indicator.setImageDrawable(mNowPlayingOverlay); } else {
			 * vh.play_indicator.setImageDrawable(null); }
			 */
		}

		@Override
		public void bindChildView(View view, Context context, Cursor cursor,
				boolean islast) {

			ViewHolder vh = (ViewHolder) view.getTag();

			String name = cursor.getString(cursor
					.getColumnIndexOrThrow(ViewUtils.ALBUM_NAME));
			String displayname = name;
			boolean unknown = name == null;
			if (unknown) {
				displayname = "Unknown";
			}
			vh.line1.setText(displayname);

			int numsongs = cursor.getInt(cursor
					.getColumnIndexOrThrow(ViewUtils.ALBUM_TRACKS));

			final StringBuilder builder = mBuffer;
			builder.delete(0, builder.length());

			if (numsongs == 1) {
				builder.append("1 song");
			} else {
				builder.append(numsongs + " songs");
			}

			vh.line2.setText(builder.toString());

			ImageView iv = vh.icon;

			// We don't actually need the path to the thumbnail file,
			// we just use it to see if there is album art or not
			String art = cursor.getString(cursor
					.getColumnIndexOrThrow(ViewUtils.ALBUM_ART));

			if (art == null || art.length() == 0) {
				iv.setBackgroundDrawable(mDefaultAlbumIcon);
				iv.setImageDrawable(null);
			} else {
				Lullaby.cover.setCachedArtwork(iv, art);

			}

			/*
			 * long currentalbumid = MusicUtils.getCurrentAlbumId(); long aid =
			 * cursor.getLong(0);
			 * 
			 * iv = vh.play_indicator; if (currentalbumid == aid) {
			 * iv.setImageDrawable(mNowPlayingOverlay); } else {
			 * 
			 * iv.setImageDrawable(null); }
			 */
			vh.play_indicator.setImageDrawable(null);
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
