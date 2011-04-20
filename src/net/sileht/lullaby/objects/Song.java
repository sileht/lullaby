package net.sileht.lullaby.objects;

/* Copyright (c) 2008 Kevin James Purdy <purdyk@onid.orst.edu>
 *  Copyright (c) 2010 ABAAKOUK Mehdi  <theli48@gmail.com>
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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import android.os.Parcel;
import android.os.Parcelable;

public class Song extends ampacheObject {
	public String artist = "";
	public String art = "";
	public String url = "";
	public String album = "";
	public String genre = "";
	public String time = "";
	public String extra = null;

	public String getType() {
		return "Song";
	}

	public String extraString() {
		if (extra == null) {
			extra = artist + " - " + album;
		}

		return extra;
	}

	public String childString() {
		return "";
	}

	public boolean hasChildren() {
		return false;
	}

	public String[] allChildren() {
		return null;
	}

	public Song() {
	}

	public void writeToParcel(Parcel out, int flags) {
		super.writeToParcel(out, flags);
		out.writeString(artist);
		out.writeString(art);
		out.writeString(url);
		out.writeString(album);
		out.writeString(genre);
		out.writeString(time);
		out.writeString(extra);
	}

	public Song(Parcel in) {
		super.readFromParcel(in);
		artist = in.readString();
		art = in.readString();
		url = in.readString();
		album = in.readString();
		genre = in.readString();
		time = in.readString();
		extra = in.readString();
	}

	@SuppressWarnings("unchecked")
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		public Song createFromParcel(Parcel in) {
			return new Song(in);
		}

		public Song[] newArray(int size) {
			return new Song[size];
		}
	};

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		id = (String) in.readObject();
		name = (String) in.readObject();
		artist = (String) in.readObject();
		art = (String) in.readObject();
		url = (String) in.readObject();
		album = (String) in.readObject();
		time = (String) in.readObject();
		genre = (String) in.readObject();
		extra = (String) in.readObject();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(id);
		out.writeObject(name);
		out.writeObject(artist);
		out.writeObject(art);
		out.writeObject(url);
		out.writeObject(album);
		out.writeObject(time);
		out.writeObject(genre);
		out.writeObject(extra);
	}

}
