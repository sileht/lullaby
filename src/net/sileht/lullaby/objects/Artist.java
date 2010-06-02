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

public class Artist extends ampacheObject {
    public int albums = 0;
    public int tracks = 0;
    

    public boolean hasChildren() {
        return true;
    }
    
    public String extraString() {
        return albums+" albums";
    }
    
    public String getType() {
        return "Artist";
    }

    public String childString() {
        return "artist_albums";
    }
    
    public String[] allChildren() {
        String[] dir = {"artist_songs", this.id};
        return dir;
    }

    public Artist() {
    }

    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(albums);
        out.writeInt(tracks);
    }

    public Artist(Parcel in) {
        super.readFromParcel(in);
        albums = in.readInt();
        tracks = in.readInt();
    }

    @SuppressWarnings("unchecked")
	public static final Parcelable.Creator CREATOR
        = new Parcelable.Creator() {
                public Artist createFromParcel(Parcel in) {
                    return new Artist(in);
                }
                
                public Artist[] newArray(int size) {
                    return new Artist[size];
                }
            };
}

