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

public class Album extends ampacheObject {
    public String artist = "";
    public String artist_id = "";
    public String tracks = "";
    public String extra = null;
    public String art = null;

    public String getType() {
        return "Album";
    }

    public String extraString() {
        if (extra == null) {
            extra = artist + " - " + tracks + " tracks";
        }
        return extra;
    }

    public String childString() {
        return "album_songs";
    }

    public boolean hasChildren() {
	return true;
    }

    public String[] allChildren() {
        String[] dir = {"album_songs", this.id};
        return dir;
    }

    public Album() {
    }

    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeString(artist);
        out.writeString(tracks);
        out.writeString(artist_id);
    }

    public Album(Parcel in) {
        super.readFromParcel(in);
        artist = in.readString();
        tracks = in.readString();
        artist_id = in.readString();
    }

    @SuppressWarnings("unchecked")
	public static final Parcelable.Creator CREATOR
        = new Parcelable.Creator() {
                public Album createFromParcel(Parcel in) {
                    return new Album(in);
                }

                public Album[] newArray(int size) {
                    return new Album[size];
                }
            };

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
        id = (String) in.readObject();
        name = (String) in.readObject();
        artist = (String) in.readObject();
        art = (String) in.readObject();
        tracks = (String) in.readObject();
        extra = (String) in.readObject();	
        artist_id = (String) in.readObject();		
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(id);
        out.writeObject(name);
        out.writeObject(artist);
        out.writeObject(art);
        out.writeObject(tracks);
        out.writeObject(extra);
        out.writeObject(artist_id);		
	}
}
