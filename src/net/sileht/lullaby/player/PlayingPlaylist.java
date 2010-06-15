package net.sileht.lullaby.player;

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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.sileht.lullaby.AmpacheRequest;
import net.sileht.lullaby.objects.Song;
import android.content.Context;
import android.util.Log;
import android.widget.BaseAdapter;
import android.widget.Toast;

public class PlayingPlaylist {

	private ArrayList<Song> mPlaylist = new ArrayList<Song>();
	private ArrayList<Song> mPlaylistShuffle = new ArrayList<Song>();
	private int mCurrentPlayingPosition = -1;
	private int mCurrentShufflePosition = -1;

	private boolean mHideCurrentPosition = false;

	private Song mLastPlayed;

	public static enum REPEAT_MODE {
		Off, Once, All
	}

	private REPEAT_MODE mRepeat = REPEAT_MODE.Off;
	private boolean mSuffle = false;

	private BaseAdapter mAdapter;

	private PlayerService mPlayer;

	private static final String TAG = "LullabyPlayingPlaylist";

	public PlayingPlaylist(PlayerService player) {
		mPlayer = player;
	}

	public void setAdapter(BaseAdapter adapter) {
		mAdapter = adapter;
		mAdapter.notifyDataSetChanged();
	}

	private void updateAdapter() {
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
	}

	public boolean isEmpty() {
		return mPlaylist.isEmpty();
	}

	public int size() {
		return mPlaylist.size();
	}

	public Song get(int position) {
		return mPlaylist.get(position);
	}

	protected void setCurrentPosition(int pos) {
		if (pos > 0 && pos < mPlaylist.size()) {
			mCurrentPlayingPosition = pos;
		}
	}

	public int getCurrentPosition() {
		if (mHideCurrentPosition) {
			return -1;
		} else {
			return mCurrentPlayingPosition;
		}
	}

	public Song remove(int position) {
		if (position < mCurrentPlayingPosition) {
			mCurrentPlayingPosition -= 1;
		} else if (position == mCurrentPlayingPosition) {
			mHideCurrentPosition = true;
		}
		Song s = mPlaylist.remove(position);

		mPlaylistShuffle.remove(s);

		updateAdapter();
		return s;
	}

	public void play(int position) {
		mCurrentPlayingPosition = position - 1;
		mLastPlayed = getNextSong();
		mPlayer.playSong(mLastPlayed);
	}

	public Song playNextAutomatic() {

		Song song = null;
		if (mRepeat == REPEAT_MODE.Once && mLastPlayed != null) {
			song = mLastPlayed;
		} else {
			song = getNextSong();
		}
		if (song != null) {
			mLastPlayed = song;
			mPlayer.playSong(song);
		}
		return song;
	}

	public Song playNext() {
		Song song = getNextSong();
		if (song != null) {
			mLastPlayed = song;
			mPlayer.playSong(song);
		}
		return song;
	}

	public Song playPrevious() {
		Song song = getPreviousSong();
		if (song != null) {
			mLastPlayed = song;
			mPlayer.playSong(song);
		}
		return song;
	}

	private Song getPreviousSong() {

		if (mPlaylist.size() < 0) {
			mCurrentPlayingPosition = -1;
		} else {

			int pos = -1;
			int size = 0;

			if (shuffleEnabled()) {
				pos = mCurrentShufflePosition;
				size = mPlaylistShuffle.size();
			} else {
				pos = mCurrentPlayingPosition;
				size = mPlaylist.size();
			}

			// Get Previous Position
			if (pos - 1 > 0) {
				pos -= 1;
			} else if (mRepeat == REPEAT_MODE.All) {
				pos = size - 1;
			} else {
				pos = -1;
			}

			if (shuffleEnabled() && pos >= 0) {
				mCurrentShufflePosition = pos;
				Song s = mPlaylistShuffle.get(mCurrentShufflePosition);
				mCurrentPlayingPosition = mPlaylist.indexOf(s);
			} else {
				mCurrentPlayingPosition = pos;
			}
		}

		mHideCurrentPosition = false;
		updateAdapter();

		if (mCurrentPlayingPosition == -1) {
			return null;
		} else {
			return mPlaylist.get(mCurrentPlayingPosition);
		}
	}

	private Song getNextSong() {

		if (mPlaylist.size() < 0) {
			mCurrentPlayingPosition = -1;
		} else {

			int pos = -1;
			int size = 0;

			if (shuffleEnabled()) {
				pos = mCurrentShufflePosition;
				size = mPlaylistShuffle.size();
			} else {
				pos = mCurrentPlayingPosition;
				size = mPlaylist.size();
			}

			//  Get Next Position
			if (pos + 1 < size) {
				pos += 1;
			} else if (mRepeat == REPEAT_MODE.All) {
				pos = 0;
			} else {
				pos = -1;
			}

			if (shuffleEnabled()) {
				if (pos >= 0) {
					mCurrentShufflePosition = pos;
				} else {
					rebuildShuffleList();
					mCurrentShufflePosition = 0;
				}
				Song s = mPlaylistShuffle.get(mCurrentShufflePosition);
				mCurrentPlayingPosition = mPlaylist.indexOf(s);
			} else {
				mCurrentPlayingPosition = pos;
			}
		}

		mHideCurrentPosition = false;
		updateAdapter();

		if (mCurrentPlayingPosition == -1) {
			return null;
		} else {
			return mPlaylist.get(mCurrentPlayingPosition);
		}
	}

	public void move(int from, int to) {
		mPlaylist.add(to, mPlaylist.remove(from));
		if (from < mCurrentPlayingPosition && to >= mCurrentPlayingPosition) {
			mCurrentPlayingPosition -= 1;
		} else if (from > mCurrentPlayingPosition
				&& to <= mCurrentPlayingPosition) {
			mCurrentPlayingPosition += 1;
		} else if (from == mCurrentPlayingPosition) {
			mCurrentPlayingPosition = to;
		}
		updateAdapter();
	}

	public void appendSongs(Context context, String[] directive) {
		AmpacheRequest request = new AmpacheRequest(context, directive, true) {
			@SuppressWarnings("unchecked")
			@Override
			public void add_objects(ArrayList list) {
				appendSongs(list);
			}
		};
		request.send();
	}

	private static class Shuffler {
		private int mPrevious;
		private Random mRandom = new Random();

		public int nextInt(int interval) {
			int ret;
			do {
				ret = mRandom.nextInt(interval);
			} while (ret == mPrevious && interval > 1);
			mPrevious = ret;
			return ret;
		}
	};

	public void appendSongs(ArrayList<Song> songs) {
		boolean startplaying = (mPlaylist.size() == 0);

		Log.d(TAG, "" + songs.size() + " added to the playlist");

		mPlaylist.addAll(songs);
		if (shuffleEnabled()) {
			Shuffler r = new Shuffler();
			for (Song s : songs) {
				int max = mPlaylistShuffle.size() - mCurrentShufflePosition - 1;
				mPlaylistShuffle.add(mCurrentShufflePosition + r.nextInt(max),
						s);
			}
		}

		updateAdapter();
		if (startplaying) {
			playNext();
		}
	}

	public void clear() {
		mCurrentPlayingPosition = -1;
		mCurrentShufflePosition = -1;
		mPlaylist.clear();
		mPlaylistShuffle.clear();
		updateAdapter();
	}

	public boolean toggleShuffle() {
		mSuffle = !mSuffle;
		if (mSuffle) {
			rebuildShuffleList();
		}
		return mSuffle;
	}

	public boolean shuffleEnabled() {
		return mSuffle;
	}

	public REPEAT_MODE toggleRepeat() {
		switch (mRepeat) {
		case Off:
			mRepeat = REPEAT_MODE.All;
			break;
		case All:
			mRepeat = REPEAT_MODE.Once;
			break;
		case Once:
			mRepeat = REPEAT_MODE.Off;
			break;
		default:
			break;
		}
		return mRepeat;
	}

	private void rebuildShuffleList() {
		mCurrentShufflePosition = -1;
		mPlaylistShuffle.clear();
		mPlaylistShuffle.addAll(mPlaylist);
		Collections.shuffle((List<Song>) mPlaylistShuffle);
	}

	public REPEAT_MODE getRepeatMode() {
		return mRepeat;
	}

	public void save(Context ctx) {
		try {
			FileOutputStream pout = ctx.openFileOutput("playlist", 0);
			(new ObjectOutputStream(pout)).writeObject(mPlaylist);
			pout.close();
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(ctx, "Failed to save playlist", Toast.LENGTH_LONG)
					.show();
		}
	}

	@SuppressWarnings("unchecked")
	public boolean load(Context ctx) {

		try {
			FileInputStream pin = ctx.openFileInput("playlist");

			Object objs = (new ObjectInputStream(pin)).readObject();
			if (objs != null && objs instanceof ArrayList) {
				if (((ArrayList) objs).size() > 0
						&& ((ArrayList) objs).get(0) instanceof Song) {
					Log.v(TAG, "Playlist loaded.");
					mPlaylist = (ArrayList<Song>) objs;
					rebuildShuffleList();
					updateAdapter();
					pin.close();
					return true;
				} else {
					Log
							.v(TAG,
									"Loaded object doesn't seems to be a valid playlist.");
				}
			} else {
				Log.v(TAG,
						"Loaded object doesn't seems to be a valid playlist.");
			}
			pin.close();
		} catch (FileNotFoundException e) {
		} catch (Exception e) {
			Toast.makeText(ctx, "Failed to load playlist.", Toast.LENGTH_LONG)
					.show();
			e.printStackTrace();
		}
		return false;
	}
}
