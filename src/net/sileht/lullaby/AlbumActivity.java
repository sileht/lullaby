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

import net.sileht.lullaby.backend.ArtworkAsyncHelper;
import net.sileht.lullaby.objects.Album;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AlphabetIndexer;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class AlbumActivity extends Activity implements
		SharedPreferences.OnSharedPreferenceChangeListener {

	static final String TAG = "LullabyAlbumActivity";

	private MatrixCursor albumsData;
	private SimpleCursorAdapter mAdapter;

	private AmpacheRequest mRequest;

	private ViewUtils mViewUtils;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.list_classic);
		ListView lv = (ListView) findViewById(R.id.list);
		mViewUtils = new ViewUtils(this);
		lv.setOnItemClickListener(mViewUtils);
		lv.setOnItemLongClickListener(mViewUtils);
		lv.setOnCreateContextMenuListener(mViewUtils);

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		if (prefs.getBoolean("clear_albums", false)) {
			clear();
		}
		setup();

		mViewUtils.doBindService();
		prefs.registerOnSharedPreferenceChangeListener(this);

	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Log.v(TAG, "getPref:" + key);
		if (sharedPreferences.getBoolean("clear_albums", false)) {
			clear();
			setup();
		}
	}

	private void clear() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean("clear_albums", false);
		editor.commit();
		if (albumsData != null)
			stopManagingCursor(albumsData);
		albumsData = null;
		if (mRequest != null)
			mRequest.stop();
	}

	private void setup() {

		if (albumsData == null) {
			// Tell them we're loading

			setProgressBarVisibility(true);

			albumsData = new MatrixCursor(ViewUtils.mAlbumsColumnName);
			startManagingCursor(albumsData);

			mRequest = new AmpacheRequest(this, new String[] { "albums", "" }) {
				@SuppressWarnings("unchecked")
				@Override
				public void add_objects(
						@SuppressWarnings("rawtypes") ArrayList list) {
					for (Album album : (ArrayList<Album>) list) {
						albumsData.newRow().add(album.id).add(album.name)
								.add(album.artist).add(album.tracks)
								.add(album.art);
					}
					albumsData.requery();
				}
			};
			mRequest.send();
		}
		mAdapter = new AlbumsAdapter(this, albumsData);
		ListView lv = (ListView) findViewById(R.id.list);
		lv.setAdapter(mAdapter);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mViewUtils.doUnbindService();
	}

	static class AlbumsAdapter extends SimpleCursorAdapter implements
			SectionIndexer, FilterQueryProvider {

		private final StringBuilder mBuffer = new StringBuilder();

		private AlphabetIndexer mIndexer;
		private Resources mRessource;
		private Cursor mCursor;

		private static int mArtworkWidth = -1;
		private static int mArtWorkHeight = -1;

		static class ViewHolder {
			TextView line1;
			TextView line2;
			ImageView play_indicator;
			ImageView icon;
		}

		public AlbumsAdapter(Context context, Cursor cursor) {
			super(context, R.layout.track_list_item_child, cursor,
					new String[] {}, new int[] {});

			mCursor = cursor;
			mRessource = context.getResources();

			if (mArtworkWidth < 0) {
				Bitmap icon = ((BitmapDrawable) mRessource
						.getDrawable(R.drawable.albumart_mp_unknown_list))
						.getBitmap();
				mArtworkWidth = icon.getWidth();
				mArtWorkHeight = icon.getHeight();
			}

			setFilterQueryProvider(this);
			mIndexer = new AlphabetIndexer(mCursor,
					mCursor.getColumnIndex(ViewUtils.ALBUM_NAME),
					mRessource.getString(R.string.fast_scroll_numeric_alphabet));
		}

		/**
		 * Reload data to view cover only after a certain period and prevent
		 * reloading to much list for displaying a cover
		 */
		private final static long ARTWORK_RELOAD_PERIOD = 3000;
		private Handler mArtworkHandler = new Handler();
		private Runnable mArtworkTask = new Runnable() {
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
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View v = super.newView(context, cursor, parent);
			ViewHolder vh = new ViewHolder();
			vh.line1 = (TextView) v.findViewById(R.id.line1);
			vh.line2 = (TextView) v.findViewById(R.id.line2);
			vh.play_indicator = (ImageView) v.findViewById(R.id.play_indicator);
			vh.play_indicator.setImageDrawable(null);
			vh.icon = (ImageView) v.findViewById(R.id.icon);
			vh.icon.setBackgroundResource(R.drawable.albumart_mp_unknown_list);
			vh.icon.setPadding(0, 0, 1, 0);
			vh.icon.setImageDrawable(null);
			v.setTag(vh);
			return v;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {

			ViewHolder vh = (ViewHolder) view.getTag();

			String name = cursor.getString(cursor
					.getColumnIndexOrThrow(ViewUtils.ALBUM_NAME));

			int numsongs = cursor.getInt(cursor
					.getColumnIndexOrThrow(ViewUtils.ALBUM_TRACKS));

			String artist = cursor.getString(cursor
					.getColumnIndexOrThrow(ViewUtils.ALBUM_ARTIST));
			String displayname = name;
			boolean unknown = name == null;
			if (unknown) {
				displayname = mRessource.getString(R.string.unknown);
			}
			vh.line1.setText(displayname);

			final StringBuilder builder = mBuffer;
			builder.delete(0, builder.length());

			if (numsongs == 1) {
				builder.append("1 " + mRessource.getString(R.string.song));
			} else {
				builder.append(numsongs + " "
						+ mRessource.getString(R.string.songs));
			}

			vh.line2.setText(artist + " - " + builder.toString());

			String art = cursor.getString(cursor
					.getColumnIndexOrThrow(ViewUtils.ALBUM_ART));

			vh.icon.setImageDrawable(null);
			if (art != null && !art.equals("")) {
				ArtworkAsyncHelper.updateArtwork(view.getContext(), vh.icon,
						art, -1, mArtworkWidth, mArtWorkHeight, true,
						new ArtworkAsyncHelper.OnImageLoadCompleteListener() {

							public void onImageLoadComplete(int token,
									ImageView iView, boolean imagePresent) {
								reloadArtworkIfNeeded();
							}
						});
			}
		}

		public Object[] getSections() {
			return mIndexer.getSections();
		}

		public int getPositionForSection(int sectionIndex) {
			return mIndexer.getPositionForSection(sectionIndex);
		}

		public int getSectionForPosition(int position) {
			return 0;
		}

		public Cursor runQuery(CharSequence text) {
			MatrixCursor nc = new MatrixCursor(ViewUtils.mAlbumsColumnName);
			mCursor.moveToFirst();
			do {
				if (mCursor.getString(1).toLowerCase()
						.startsWith(((String) text).toLowerCase())) {
					MatrixCursor.RowBuilder rb = nc.newRow();
					for (int i = 0; i < mCursor.getColumnCount(); i++) {
						rb = rb.add(mCursor.getString(i));
					}
				}
			} while (mCursor.moveToNext());

			mIndexer = new AlphabetIndexer(nc,
					nc.getColumnIndex(ViewUtils.ALBUM_NAME),
					mRessource.getString(R.string.fast_scroll_numeric_alphabet));
			return nc;
		}
	}
}
