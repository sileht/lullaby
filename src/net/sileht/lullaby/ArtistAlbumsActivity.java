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

import net.sileht.lullaby.backend.ArtworkAsyncHelper;
import net.sileht.lullaby.objects.Album;
import net.sileht.lullaby.objects.Artist;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AlphabetIndexer;
import android.widget.ExpandableListView;
import android.widget.FilterQueryProvider;
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

	private ViewUtils mViewUtils;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.list_expandable);

		mListView = (ExpandableListView) findViewById(R.id.list);
		AlbumClickListener albumCL = new AlbumClickListener();
		mViewUtils = new ViewUtils(this);
		mListView.setOnChildClickListener(albumCL);
		mListView.setOnGroupExpandListener(albumCL);
		mListView.setOnItemLongClickListener(mViewUtils);
		mListView.setOnCreateContextMenuListener(mViewUtils);

		if (artistsData == null || albumsData == null) {
			// Tell them we're loading

			setProgressBarVisibility(true);

			artistsData = new MatrixCursor(ViewUtils.mArtistColumnName);
			albumsData = new HashMap<String, MatrixCursor>();
			startManagingCursor(artistsData);

			mAdapter = getNewAdapter();

			AmpacheRequest request = new AmpacheRequest(this,
					new String[] { "artists", "" }) {
				@SuppressWarnings("unchecked")
				@Override
				public void add_objects(ArrayList list) {
					for (Artist artist : (ArrayList<Artist>) list) {
						artistsData.newRow().add(artist.id).add(artist.name)
								.add(artist.albums).add(artist.tracks);
					}
					artistsData.requery();
				}
			};
			request.send();
		} else {
			mAdapter = getNewAdapter();
		}
		mListView.setAdapter(mAdapter);

	}

	@Override
	protected void onStart() {
		super.onStart();
		mViewUtils.onStart();
	}

	@Override
	protected void onStop() {
		mViewUtils.onStop();
		super.onStop();
	}

	private class AlbumClickListener implements
			ExpandableListView.OnChildClickListener,
			ExpandableListView.OnGroupExpandListener {
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

			mViewUtils.startSongsActivity("album_songs", albumId, album + " - "
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
			SectionIndexer, FilterQueryProvider {

		private static int mArtworkWidth = -1;
		private static int mArtWorkHeight = -1;

		private final StringBuilder mBuffer = new StringBuilder();
		private final ArtistAlbumsActivity mCurrentActivity;

		private AlphabetIndexer mIndexer;
		private Resources mRessource;
		private Cursor mCursor;

		static class ViewHolder {
			TextView line1;
			TextView line2;
			ImageView play_indicator;
			ImageView icon;
			int index;
		}

		public ArtistAlbumsAdapter(Context context,
				ArtistAlbumsActivity currentactivity, Cursor cursor,
				int glayout, String[] gfrom, int[] gto, int clayout,
				String[] cfrom, int[] cto) {

			super(context, cursor, glayout, gfrom, gto, clayout, cfrom, cto);

			mCursor = cursor;
			mRessource = context.getResources();
			mCurrentActivity = currentactivity;

			if (mArtworkWidth < 0) {
				Bitmap icon = ((BitmapDrawable) mRessource
						.getDrawable(R.drawable.albumart_mp_unknown_list))
						.getBitmap();
				mArtworkWidth = icon.getWidth();
				mArtWorkHeight = icon.getHeight();
			}

			setFilterQueryProvider(this);
			mIndexer = new AlphabetIndexer(mCursor, mCursor
					.getColumnIndex(ViewUtils.ARTIST_NAME), mRessource
					.getString(R.string.fast_scroll_numeric_alphabet));
		}

		/**
		 * Reload data to view cover only after a certain period and prevent
		 * reloading to much list for displaying a cover
		 */
		private final static long ARTWORK_RELOAD_PERIOD = 3000;
		private Handler mArtworkHandler = new Handler();
		private Runnable mArtworkTask = new Runnable() {
			@Override
			public void run() {
				notifyDataSetChanged();
			}
		};

		private void reloadArtworkIfNeeded() {
			mArtworkHandler.removeCallbacks(mArtworkTask);
			mArtworkHandler.postDelayed(mArtworkTask, ARTWORK_RELOAD_PERIOD);
		}

		@Override
		public boolean hasStableIds() {
			return true;
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
						mCurrentActivity, new String[] {
								"artist_albums", id }) {
					@SuppressWarnings("unchecked")
					@Override
					public void add_objects(ArrayList list) {
						for (Album album : (ArrayList<Album>) list) {
							MatrixCursor cur = ((ArtistAlbumsActivity) this.mContext).albumsData
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
			vh.icon.setBackgroundResource(R.drawable.albumart_mp_unknown_list);
			vh.icon.setPadding(0, 0, 1, 0);
			vh.icon.setImageDrawable(null);
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

			// We don't actually need the path to the thumbnail file,
			// we just use it to see if there is album art or not
			String art = cursor.getString(cursor
					.getColumnIndexOrThrow(ViewUtils.ALBUM_ART));

			vh.icon.setImageDrawable(null);
			if (art != null && !art.equals("")) {
				ArtworkAsyncHelper.updateArtwork(view.getContext(), vh.icon,
						art, -1, mArtworkWidth, mArtWorkHeight, true,
						new ArtworkAsyncHelper.OnImageLoadCompleteListener() {

							@Override
							public void onImageLoadComplete(int token,
									ImageView iView, boolean imagePresent) {
								reloadArtworkIfNeeded();
							}
						});

			}
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

		@Override
		public Cursor runQuery(CharSequence text) {
			MatrixCursor nc = new MatrixCursor(ViewUtils.mArtistColumnName);
			mCursor.moveToFirst();
			do {
				if (mCursor.getString(1).startsWith((String) text)) {
					MatrixCursor.RowBuilder rb = nc.newRow();
					for (int i = 0; i < mCursor.getColumnCount(); i++) {
						rb = rb.add(mCursor.getString(i));
					}
				}
			} while (mCursor.moveToNext());

			mIndexer = new AlphabetIndexer(nc, nc
					.getColumnIndex(ViewUtils.ARTIST_NAME), mRessource
					.getString(R.string.fast_scroll_numeric_alphabet));
			return nc;
		}
	}

}
