package net.sileht.lullaby.objects;

/* Copyright (c) 2008 Kevin James Purdy <purdyk@onid.orst.edu>
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

import android.os.Parcelable;
import android.os.Parcel;

public class Album extends ampacheObject {
    public String artist = "";
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
    }

    public Album(Parcel in) {
        super.readFromParcel(in);
        artist = in.readString();
        tracks = in.readString();
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
}
