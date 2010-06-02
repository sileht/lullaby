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

public class Playlist extends ampacheObject {
    public String owner = "";
    public String tracks = "";
    public String extra = null;

    public String getType() {
        return "Playlist";
    }
    
    public String extraString() {
        if (extra == null) {
            extra = owner + " - " + tracks;
        }
        return extra;
    }

    public boolean hasChildren() {
        return true;
    }
    
    public String childString() {
        return "playlist_songs";
    }

    public String[] allChildren() {
        String[] dir = {"playlist_songs", this.id};
        return dir;
    }

    public Playlist() {
    }

    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeString(owner);
        out.writeString(tracks);
        out.writeString(extra);
    }

    public Playlist(Parcel in) {
        super.readFromParcel(in);
        owner = in.readString();
        tracks = in.readString();
        extra = in.readString();
    }

    @SuppressWarnings("unchecked")
	public static final Parcelable.Creator CREATOR
        = new Parcelable.Creator() {
                public Playlist createFromParcel(Parcel in) {
                    return new Playlist(in);
                }

                public Playlist[] newArray(int size) {
                    return new Playlist[size];
                }
            };
}
