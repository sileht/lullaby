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

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;

public class SettingActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);
		
		findPreference("InsecureSSL").setOnPreferenceChangeListener(
				new OnPreferenceChangeListener() {
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						Utils.setSSLCheck((Boolean) newValue);
						return true;
					}
				});
	}

	protected void onStop() {
		Log.v("LullabySetting","Exit settings, relogin to ampache;");
		Lullaby.comm.force_auth_request();
		/* we want to tell other activities that we need to reload */
		super.onStop();
	}

}
