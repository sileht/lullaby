package net.sileht.lullaby.backend;

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

import java.net.*;
import java.io.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import java.util.ArrayList;

import net.sileht.lullaby.objects.*;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import java.lang.Integer;
import java.lang.Long;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

public class AmpacheBackend extends HandlerThread {

	public String authToken = "";
	public String lastErr;

	private XMLReader reader;

	private SharedPreferences prefs;

	public Handler incomingRequestHandler;
	public Boolean stop = false;

	public Boolean hasAlreadyTryHandshake = false;
	public Boolean hasSettingsIncorrect = false;

	private static final String TAG = "DroidZikAmpacheConnector";

	public AmpacheBackend(Context context) throws Exception {
		super("AmpacheBackend");
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");
		reader = XMLReaderFactory.createXMLReader();
		setDaemon(true);
		start();
	}
	
	public void force_auth_request(){
		hasSettingsIncorrect = false;
		authToken = "";
		ping();
	}
	
	public void ping() {
		if (incomingRequestHandler != null) {
			Message msgauth = new Message();
			msgauth.obj = new String[] { "ping", "" };
			incomingRequestHandler.sendMessageAtFrontOfQueue(msgauth);
		}
	}

	public boolean isConfigured() {
		return (prefs.getString("server_username_preference", "") != "" && prefs
				.getString("server_url_preference", "") != "");
	}

	@Override
	protected void onLooperPrepared() {

		incomingRequestHandler = new Handler() {
			public void waitAndsend(Message msg) {
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
				}
				this.sendMessage(msg);
			}

			private AmpacheHandler getHandlerFor(String action) {
				if (action.equals("ping")) {
					return (AmpacheHandler) new AmpacheHandler();
				} else if (action.equals("handshake")) {
					return (AmpacheHandler) new AmpacheAuthParser();
				} else if (action.equals("artists")) {
					return (AmpacheHandler) new AmpacheArtistParser();
				} else if (action.equals("albums")) {
					return (AmpacheHandler) new AmpacheAlbumParser();
				} else if (action.equals("artist_albums")) {
					return (AmpacheHandler) new AmpacheAlbumParser();
				} else if (action.equals("artist_songs")) {
					return (AmpacheHandler) new AmpacheSongParser();
				} else if (action.equals("album_songs")) {
					return (AmpacheHandler) new AmpacheSongParser();
				} else if (action.equals("playlist_songs")) {
					return (AmpacheHandler) new AmpacheSongParser();
				} else if (action.equals("tag_artists")) {
					return (AmpacheHandler) new AmpacheArtistParser();
				} else if (action.equals("albums")) {
					return (AmpacheHandler) new AmpacheAlbumParser();
				} else if (action.equals("playlists")) {
					return (AmpacheHandler) new AmpachePlaylistParser();
				} else if (action.equals("song")) {
					return (AmpacheHandler) new AmpacheSongParser();
				} else if (action.equals("songs")) {
					return (AmpacheHandler) new AmpacheSongParser();
				} else if (action.equals("tags")) {
					return (AmpacheHandler) new AmpacheTagParser();
				} else if (action.equals("search_songs")) {
					return (AmpacheHandler) new AmpacheSongParser();
				} else {
					return null;
				}
			}

			private String getHandshakeArgs() {
				MessageDigest md;
				try {
					md = MessageDigest.getInstance("SHA-256");
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
					return "";
				}
				/* Get the current time, and convert it to a string */
				String time = Long.toString((new Date()).getTime() / 1000);

				/* build our passphrase hash */
				md.reset();

				/* first hash the password */
				String pwHash = prefs.getString("server_password_preference",
						"");
				md.update(pwHash.getBytes(), 0, pwHash.length());
				String preHash = time + asHex(md.digest());

				/* then hash the timestamp in */
				md.reset();
				md.update(preHash.getBytes(), 0, preHash.length());
				String hash = asHex(md.digest());

				/* request server auth */
				String user = prefs.getString("server_username_preference", "");

				return "auth=" + hash + "&timestamp=" + time
						+ "&version=350001&user=" + user;
			}

			public String getUrlFor(String action, String filter, int offset) {

				String range = "&offset=" + offset + "&limit=100";
				String append = "";

				if (action.equals("handshake")) {
					append += "&" + getHandshakeArgs();
				} else {
					append += "&auth=" + authToken;
				}

				if (action.equals("ping")) {
					range = "";
				} else if (action.equals("handshake")) {
					range = "";
				} else if (action.equals("artist_albums")) {
					append += "&filter=" + filter;
				} else if (action.equals("artist_songs")) {
					append += "&filter=" + filter;
				} else if (action.equals("album_songs")) {
					append += "&filter=" + filter;
				} else if (action.equals("playlist_songs")) {
					append += "&filter=" + filter;
				} else if (action.equals("tag_artists")) {
					append += "&filter=" + filter;
				} else if (action.equals("search_songs")) {
					append += "&filter=" + filter;
				} else if (action.equals("song")) {
					append += "&filter=" + filter;
				}

				return prefs.getString("server_url_preference", "")
						+ "/server/xml.server.php?action=" + action + append + range;

			}

			private AmpacheHandler makeRequest(String url, String action) {
				Log.d(TAG, "Ampache connector try open " + url);
				AmpacheHandler hand = getHandlerFor(action);
				/* now we fetch */
				InputSource dataIn = null;
				try {
					dataIn = new InputSource(new URL(url).openStream());
				} catch (MalformedURLException e) {
					authToken = "";
					hasSettingsIncorrect = true;
					return null;
				} catch (IOException e) {
					authToken = "";
					return null;
				}

				/* all done loading data, now to parse */
				reader.setContentHandler(hand);

				try {
					reader.parse(dataIn);
				} catch (Exception e) {
					e.printStackTrace();
				}

				return hand;
			}

			private Boolean makeAuthentification() {
				Log.d(TAG, "Ampache handshake request.");

				authToken = "";
				String url = getUrlFor("handshake", "", 0);
				AmpacheHandler hand = makeRequest(url, "handshake");
				if (hand == null) {
					authToken = "";
					Log.d(TAG, "Ampache handshake failed.");
					return false;
				} else if (hand.error != null) {
					hasAlreadyTryHandshake = true;
					authToken = "";
					Log
							.d(TAG, "Ampache handshake failed. (" + hand.error
									+ ")");
					return false;
				} else {
					hasAlreadyTryHandshake = true;
					authToken = ((AmpacheAuthParser) hand).token;
					Log.d(TAG, "Ampache connected.");
					return true;
				}
			}

			public void handleMessage(Message msg) {

				if (!isConfigured()) {
					authToken = "";
					Log.v(TAG, "Backend not configured, retrying in 1s.");
					Message msgreply = Message.obtain(msg);
					msgreply.what = 100;
					this.waitAndsend(msgreply);
					return;
				}

				Message reply = new Message();

				String[] directive = (String[]) msg.obj;
				String action = directive[0];
				String filter = directive[1];
				int offset = msg.arg1;

				Log.d(TAG, "Backend handle new message: " + action);


				if (authToken.equals("")) {
					if(!makeAuthentification()){
						this.waitAndsend(Message.obtain(msg));
						return;
					}
				}
				
				String url = getUrlFor(action, filter, offset);
				AmpacheHandler hand = makeRequest(url, action);

				if (hand == null) {
					Log.e(TAG, "Ampache fail to open: " + url + ", retrying");
					authToken = "";
					this.sendMessage(Message.obtain(msg));
					return;
				}

				if (hand.error != null) {
					Log.d(TAG, "Ampache return a error (" + hand.error + ")");
					if (hand.errorCode == 401) {
						authToken = "";
						this.sendMessage(Message.obtain(msg));
						return;
					} else {
						reply.what = 9999;
						reply.obj = hand.error;
					}
				} else {
					Log.d(TAG, "Reply to message object size: "
							+ hand.data.size());

					reply.what = msg.what;
					reply.obj = hand.data;
					reply.arg1 = offset;
				}

				if (msg.what != 0 ) {
					try {
						msg.replyTo.send(reply);
					} catch (Exception poo) {
						// well shit, that sucks doesn't it
					}
				}
			}
		};
	}

	private class AmpacheHandler extends DefaultHandler {
		public ArrayList<ampacheObject> data = new ArrayList<ampacheObject>();
		public String error = null;
		public int errorCode = 0;
		protected CharArrayWriter contents = new CharArrayWriter();

		public void startDocument() throws SAXException {

		}

		public void endDocument() throws SAXException {

		}

		public void characters(char[] ch, int start, int length)
				throws SAXException {
			contents.write(ch, start, length);
		}

		public void startElement(String namespaceURI, String localName,
				String qName, Attributes attr) throws SAXException {

			if (localName.equals("error"))
				errorCode = Integer.parseInt(attr.getValue("code"));
			contents.reset();
		}

		public void endElement(String namespaceURI, String localName,
				String qName) throws SAXException {
			if (localName.equals("error")) {
				error = contents.toString();
			}
		}

	}

	private class AmpacheAuthParser extends AmpacheHandler {
		public String token = "";

		public void endElement(String namespaceURI, String localName,
				String qName) throws SAXException {

			super.endElement(namespaceURI, localName, qName);

			if (localName.equals("auth")) {
				token = contents.toString();
			}
		}
	}

	private class AmpacheArtistParser extends AmpacheHandler {
		private Artist current;

		public void startElement(String namespaceURI, String localName,
				String qName, Attributes attr) throws SAXException {

			super.startElement(namespaceURI, localName, qName, attr);

			if (localName.equals("artist")) {
				current = new Artist();
				current.id = attr.getValue("id");
			}
		}

		public void endElement(String namespaceURI, String localName,
				String qName) throws SAXException {

			super.endElement(namespaceURI, localName, qName);

			if (localName.equals("name")) {
				current.name = contents.toString();
			}

			if (localName.equals("albums")) {
				current.albums = Integer.parseInt(contents.toString());
			}

			if (localName.equals("songs")) {
				current.tracks = Integer.parseInt(contents.toString());
			}

			if (localName.equals("artist")) {
				data.add(current);
			}

		}
	}

	private class AmpacheAlbumParser extends AmpacheHandler {
		private Album current;

		public void startElement(String namespaceURI, String localName,
				String qName, Attributes attr) throws SAXException {

			super.startElement(namespaceURI, localName, qName, attr);

			if (localName.equals("album")) {
				current = new Album();
				current.id = attr.getValue("id");
			}
		}

		public void endElement(String namespaceURI, String localName,
				String qName) throws SAXException {

			super.endElement(namespaceURI, localName, qName);

			if (localName.equals("name")) {
				current.name = contents.toString();
			}

			if (localName.equals("artist")) {
				current.artist = contents.toString();
			}

			if (localName.equals("tracks")) {
				current.tracks = contents.toString();
			}
			if (localName.equals("art")) {
				current.art = contents.toString();
			}

			if (localName.equals("album")) {
				data.add(current);
			}
		}
	}

	private class AmpacheTagParser extends AmpacheHandler {
		private Tag current;

		public void startElement(String namespaceURI, String localName,
				String qName, Attributes attr) throws SAXException {

			super.startElement(namespaceURI, localName, qName, attr);

			if (localName.equals("tag")) {
				current = new Tag();
				current.id = attr.getValue("id");
			}
		}

		public void endElement(String namespaceURI, String localName,
				String qName) throws SAXException {

			super.endElement(namespaceURI, localName, qName);

			if (localName.equals("name")) {
				current.name = contents.toString();
			}
			if (localName.equals("albums")) {
				current.albums = contents.toString();
			}
			if (localName.equals("artists")) {
				current.artists = contents.toString();
			}
			if (localName.equals("tag")) {
				data.add(current);
			}
		}
	}

	private class AmpachePlaylistParser extends AmpacheHandler {
		private Playlist current;

		public void startElement(String namespaceURI, String localName,
				String qName, Attributes attr) throws SAXException {

			super.startElement(namespaceURI, localName, qName, attr);

			if (localName.equals("playlist")) {
				current = new Playlist();
				current.id = attr.getValue("id");
			}
		}

		public void endElement(String namespaceURI, String localName,
				String qName) throws SAXException {

			super.endElement(namespaceURI, localName, qName);

			if (localName.equals("name")) {
				current.name = contents.toString();
			}

			if (localName.equals("owner")) {
				current.owner = contents.toString();
			}

			if (localName.equals("items")) {
				current.tracks = contents.toString();
			}

			if (localName.equals("playlist")) {
				data.add(current);
			}
		}
	}

	private class AmpacheSongParser extends AmpacheHandler {
		private Song current;

		public void startElement(String namespaceURI, String localName,
				String qName, Attributes attr) throws SAXException {

			super.startElement(namespaceURI, localName, qName, attr);

			if (localName.equals("song")) {
				current = new Song();
				current.id = attr.getValue("id");
			}
		}

		public void endElement(String namespaceURI, String localName,
				String qName) throws SAXException {

			super.endElement(namespaceURI, localName, qName);

			if (localName.equals("song")) {
				data.add(current);
			}

			if (localName.equals("title")) {
				current.name = contents.toString();
			}

			if (localName.equals("artist")) {
				current.artist = contents.toString();
			}

			if (localName.equals("art")) {
				current.art = contents.toString();
			}

			if (localName.equals("url")) {
				current.url = contents.toString();
			}

			if (localName.equals("album")) {
				current.album = contents.toString();
			}

			if (localName.equals("genre")) {
				current.genre = contents.toString();
			}
			if (localName.equals("time")) {
				current.time = contents.toString();
			}
		}

	}

	private final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

	public String asHex(byte[] buf) {
		char[] chars = new char[2 * buf.length];
		for (int i = 0; i < buf.length; ++i) {
			chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
			chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
		}
		return new String(chars);
	}
}
