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

import android.os.Parcelable;
import android.os.Parcel;

public class Tag extends ampacheObject {
	public String artists = "";
	public String albums = "";
	public String extra = null;

	public String getType() {
		return "Tag";
	}

	public String extraString() {
		if (extra == null) {
			extra = artists + " artists - " + albums + " albums";
		}
		return extra;
	}

	public boolean hasChildren() {
		return true;
	}

	public String[] allChildren() {
		String[] dir = { "tag_songs", this.id };
		return dir;
	}

	public String childString() {
		return "tag_artists";
	}

	public Tag() {
	}

	public Tag(Parcel in) {
		super.readFromParcel(in);
	}

	@SuppressWarnings("unchecked")
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		public Tag createFromParcel(Parcel in) {
			return new Tag(in);
		}

		public Tag[] newArray(int size) {
			return new Tag[size];
		}
	};

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		id = (String) in.readObject();
		name = (String) in.readObject();
		artists = (String) in.readObject();
		albums = (String) in.readObject();
		extra = (String) in.readObject();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(id);
		out.writeObject(name);
		out.writeObject(artists);
		out.writeObject(albums);
		out.writeObject(extra);
	}

}
