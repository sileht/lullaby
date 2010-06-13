package net.sileht.lullaby.backend;
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

import net.sileht.lullaby.AmpacheRequest;
import net.sileht.lullaby.objects.Song;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import android.widget.BaseAdapter;

public class PlayingPlaylist {

	private ArrayList<Song> mPlaylist = new ArrayList<Song>();
	private int mCurrentPlayingPosition = -1;
	private boolean mHideCurrentPosition = false;
	
	private Song lastPlayed;


	public enum REPEAT_MODE {
		Off,Once,All
	}
	
	private REPEAT_MODE mRepeat = REPEAT_MODE.Off;
	private boolean mSuffle = false;
	
	private BaseAdapter mAdapter;
	
	private PlayerService mPlayer;

	private static final String TAG = "DroidZikPlayingPlaylist";

	
	public PlayingPlaylist(PlayerService player){
		mPlayer = player;
	}
	
	public boolean toggleShuffle() {
		mSuffle = !mSuffle;
		return mSuffle;
	}
	public boolean shuffleEnabled(){
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

	public void shuffle() {
		if (!mPlaylist.isEmpty()) {
			if (mCurrentPlayingPosition >= 0) {
				Song s = mPlaylist.remove(mCurrentPlayingPosition);
				Collections.shuffle(mPlaylist);
				mPlaylist.add(0, s);
				mCurrentPlayingPosition = 0;
			} else {
				Collections.shuffle(mPlaylist);				
			}
			updateAdapter();
		}
	}

	public boolean isEmpty() {
		return mPlaylist.isEmpty();
	}
	
	public REPEAT_MODE getRepeatMode(){
		return mRepeat;
	}

	public int size() {
		return mPlaylist.size();
	}

	public Song get(int position) {
		return mPlaylist.get(position);
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
		updateAdapter();
		return s;
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

	public void play(int position) {
		mCurrentPlayingPosition = position - 1;
		lastPlayed = getNextSong();
		mPlayer.playSong(lastPlayed);
	}


	public Song playNext() {
		Song song = getNextSong();
		if (song != null) {
			lastPlayed = song;
			mPlayer.playSong(song);
		}
		return song;
	}

	public Song playPrevious() {
		Song song = getPreviousSong();
		if (song != null) {
			lastPlayed = song;
			mPlayer.playSong(song);
		}
		return song;
	}
	
	private Song getPreviousSong() {
		if (mRepeat == REPEAT_MODE.Once) {
			if (lastPlayed != null){
				return lastPlayed;
			}
		}
		
		if (mPlaylist.size() < 0) {
			mCurrentPlayingPosition = -1;
		} else if (mCurrentPlayingPosition - 1 > 0) {
			mCurrentPlayingPosition -= 1;
		} else if (mRepeat == REPEAT_MODE.All) {
			mCurrentPlayingPosition = mPlaylist.size() - 1;
		} else {
			mCurrentPlayingPosition = -1;
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
		if (mRepeat == REPEAT_MODE.Once) {
			if (lastPlayed != null){
				return lastPlayed;
			}
		}
		
		if (mPlaylist.size() < 0) {
			mCurrentPlayingPosition = -1;
		} else if (mCurrentPlayingPosition + 1 < mPlaylist.size()) {
			mCurrentPlayingPosition += 1;
		} else if (mRepeat == REPEAT_MODE.All) {
			mCurrentPlayingPosition = 0;
		} else {
			mCurrentPlayingPosition = -1;
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

	public void appendSongs(ArrayList<Song> songs) {
		boolean startplaying = (mPlaylist.size() == 0);

		Log.d(TAG, "" + songs.size() + " added to the playlist");

		mPlaylist.addAll(songs);

		updateAdapter();
		if (startplaying) {
			mPlayer.playSong(getNextSong());
		}
	}

	public void clear() {
		mCurrentPlayingPosition = -1;
		mPlaylist.clear();
		updateAdapter();
	}

	public void save(Context ctx) {
		try {
			FileOutputStream pout = ctx.openFileOutput("playlist", 0);
			(new ObjectOutputStream(pout)).writeObject(mPlaylist);
			pout.close();
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(ctx, "Failed to save playlist",
					Toast.LENGTH_LONG).show();
		}
	}

	@SuppressWarnings("unchecked")
	public void load(Context ctx) {

		try {
			FileInputStream pin = ctx.openFileInput("playlist");

			Object objs = (new ObjectInputStream(pin)).readObject();
			if (objs != null && objs instanceof ArrayList) {
				if (((ArrayList) objs).size() > 0
						&& ((ArrayList) objs).get(0) instanceof Song) {
					Log.v(TAG, "Playlist loaded.");
					mPlaylist = (ArrayList<Song>) objs;
					updateAdapter();
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
		} catch (FileNotFoundException e){
			// No playlist for now
		} catch (Exception e) {
			Toast.makeText(ctx, "Failed to load playlist.",
					Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}
}
