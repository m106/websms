/*
 * Copyright (C) 2010 Felix Bechstein
 * 
 * This file is part of WebSMS.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */
package de.ub0r.android.websms;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import de.ub0r.android.websms.connector.common.Connector;
import de.ub0r.android.websms.connector.common.ConnectorSpec;

/**
 * Preferences.
 * 
 * @author flx
 */
public class Preferences extends PreferenceActivity implements
		SharedPreferences.OnSharedPreferenceChangeListener {
	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.prefs);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onResume() {
		super.onResume();
		WebSMS.doPreferences = true;
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		p.registerOnSharedPreferenceChangeListener(this);
		PreferenceCategory pc = (PreferenceCategory) this
				.findPreference("settings_connectors");
		final ConnectorSpec[] css = WebSMS.getConnectors(
				ConnectorSpec.CAPABILITIES_PREFS, // .
				ConnectorSpec.STATUS_INACTIVE);
		String id;
		Preference cp;
		String action;
		for (ConnectorSpec cs : css) {
			if (cs.getPackage() == null) {
				continue;
			}
			id = cs.getID();
			if (pc.findPreference(id) != null) {
				// FIXME: this is broken!
				continue;
			}
			cp = new Preference(this);
			cp.setKey(id);
			cp.setTitle(cs.getPrefsTitle());
			action = cs.getPackage() + Connector.ACTION_PREFS;
			cp.setIntent(new Intent(action));
			pc.addPreference(cp);
			Log.d("WebSMS.prefs", "added: " + action);
		}
	}

	/**
	 *{@inheritDoc}
	 */
	public final void onSharedPreferenceChanged(final SharedPreferences prefs,
			final String key) {
		if (key.equals(WebSMS.PREFS_SENDER)) {
			// check for wrong sender format. people can't read..
			final String p = prefs.getString(WebSMS.PREFS_SENDER, "");
			if (!p.startsWith("+")) {
				Toast.makeText(this, R.string.log_wrong_sender,
						Toast.LENGTH_LONG).show();
			}
		}
		if (key.equals(WebSMS.PREFS_DEFPREFIX)) {
			final String p = prefs.getString(WebSMS.PREFS_DEFPREFIX, "");
			if (!p.startsWith("+")) {
				Toast.makeText(this, R.string.log_wrong_defprefix,
						Toast.LENGTH_LONG).show();
			}
		}
	}
}
