package de.ub0r.android.andGMXsms;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * Main Activity.
 * 
 * @author flx
 */
public class AndGMXsms extends Activity {

	/** Static reference to running Activity. */
	static AndGMXsms me;
	/** Preference's name. */
	public static final String PREFS_NAME = "andGMXsmsPrefs";
	/** Preference's name: username. */
	private static final String PREFS_USER = "user";
	/** Preference's name: user's password. */
	private static final String PREFS_PASSWORD = "password";
	/** Preference's name: user's phonenumber. */
	private static final String PREFS_SENDER = "sender";
	/** Preferences: username. */
	public static String prefsUser;
	/** Preferences: user's password. */
	public static String prefsPassword;
	/** Preferences: user's phonenumber. */
	public static String prefsSender;
	/** Preferences ready? */
	public static boolean prefsReady = false;
	/** Remaining free sms. */
	public static String remFree = null;

	/** Length of a prefix. */
	private static final int PREFIX_LEN = 3;

	/** Public Dialog ref. */
	public static Dialog dialog = null;
	/** Dialog String. */
	public static String dialogString = null;

	/** Public Connector. */
	public static AsyncTask<String, Boolean, Boolean> connector;

	/** Dialog: about. */
	private static final int DIALOG_ABOUT = 0;

	/** Message for logging. **/
	public static final int MESSAGE_LOG = 0;
	/** Message for update free sms count. **/
	public static final int MESSAGE_FREECOUNT = 1;
	/** Message to send. */
	public static final int MESSAGE_SEND = 2;
	/** Message to bootstrap. */
	public static final int MESSAGE_BOOTSTRAP = 3;
	/** Message to open settings. */
	public static final int MESSAGE_SETTINGS = 4;

	/**
	 * Preferences: user's default prefix.
	 * 
	 * @return user's default prefix
	 */
	public static String prefsPrefix() {
		if (prefsSender.length() < PREFIX_LEN) {
			return prefsSender;
		}
		return AndGMXsms.prefsSender.substring(0, PREFIX_LEN);
	}

	/** Log. */
	private TextView log;
	/** Local log store. */
	private static String logString = "";
	/** MessageHandler. */
	Handler messageHandler;

	/**
	 * Called when the activity is first created.
	 * 
	 * @param savedInstanceState
	 *            default param
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// save ref to me.
		me = this;
		// inflate XML
		this.setContentView(R.layout.main);
		// save ref to log
		this.log = (TextView) this.findViewById(R.id.log);
		// register MessageHandler
		this.messageHandler = new AndGMXsms.MessageHandler();

		// Restore preferences
		SharedPreferences settings = this.getSharedPreferences(PREFS_NAME, 0);
		prefsUser = settings.getString(PREFS_USER, "");
		prefsPassword = settings.getString(PREFS_PASSWORD, "");
		prefsSender = settings.getString(PREFS_SENDER, "");

		// register Listener
		Button button = (Button) this.findViewById(R.id.composer);
		button.setOnClickListener(this.openComposer);
		button = (Button) this.findViewById(R.id.getfree);
		button.setOnClickListener(this.runGetFree);
	}

	/** Called on activity pause. */
	@Override
	protected final void onPause() {
		super.onPause();
		if (this.log != null) {
			logString = this.log.getText().toString();
		} else {
			logString = null;
		}
	}

	/** Called on activity resume. */
	@Override
	protected final void onResume() {
		super.onResume();
		// restore log
		if (this.log != null && logString != null) {
			this.log.setText(logString);
		}

		// set free sms count
		if (remFree != null) {
			TextView tw = (TextView) this.findViewById(R.id.freecount);
			tw.setText(this.getResources().getString(R.string.free_) + " "
					+ remFree);
		}

		// restart dialog
		if (dialogString != null) {
			if (dialog != null) {
				try {
					dialog.dismiss();
				} catch (Exception e) {
					// nothing to do
				}
			}
			dialog = ProgressDialog.show(this, null, dialogString, true);
		}

		// check prefs
		if (prefsUser.equals("") || prefsPassword.equals("")
				|| prefsSender.equals("")) {
			prefsReady = false;
			this.lognl(this.getResources().getString(
					R.string.log_empty_settings));
		} else {
			if (!prefsReady) {
				this.log.setText("");
			}
			prefsReady = true;
		}

		// enable/disable buttons
		Button button = (Button) this.findViewById(R.id.composer);
		button.setEnabled(prefsReady);
		button = (Button) this.findViewById(R.id.getfree);
		button.setEnabled(prefsReady);
	}

	/** Save prefs. */
	final void saveSettings() {
		// save user preferences
		SharedPreferences settings = this.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PREFS_USER, prefsUser);
		editor.putString(PREFS_PASSWORD, prefsPassword);
		editor.putString(PREFS_SENDER, prefsSender);
		// commit changes
		editor.commit();
	}

	/** Listener for launching Composer. */
	private OnClickListener openComposer = new OnClickListener() {
		public void onClick(final View v) {
			AndGMXsms.this.startActivity(new Intent(AndGMXsms.this,
					Composer.class));
		}
	};

	/** Listener for launching a get-free-sms-count-thread. */
	private OnClickListener runGetFree = new OnClickListener() {
		public void onClick(final View v) {
			connector = new Connector().execute((String) null);
		}
	};

	/**
	 * Open menu.
	 * 
	 * @param menu
	 *            menu to inflate
	 * @return ok/fail?
	 */
	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = this.getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	/**
	 * Handles item selections.
	 * 
	 * @param item
	 *            menu item
	 * @return done?
	 */
	public final boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_clearlog: // clear logs
			TextView tw = (TextView) this.findViewById(R.id.log);
			tw.setText("");
			logString = "";
			return true;
		case R.id.item_about: // start about dialog
			this.showDialog(DIALOG_ABOUT);
			return true;
		case R.id.item_settings: // start settings activity
			this.startActivity(new Intent(this, Settings.class));
			return true;
		default:
			return false;
		}
	}

	/**
	 * Called to create dialog.
	 * 
	 * @param id
	 *            Dialog id
	 * @return dialog
	 */
	@Override
	protected final Dialog onCreateDialog(final int id) {
		Dialog myDialog;
		switch (id) {
		case DIALOG_ABOUT:
			myDialog = new Dialog(this);
			myDialog.setContentView(R.layout.about);
			myDialog.setTitle(this.getResources().getString(R.string.about_)
					+ " v"
					+ this.getResources().getString(R.string.app_version));
			Button button = (Button) myDialog.findViewById(R.id.btn_donate);
			button.setOnClickListener(new OnClickListener() {
				public void onClick(final View view) {
					Uri uri = Uri.parse(AndGMXsms.this
							.getString(R.string.donate_url));
					AndGMXsms.this.startActivity(new Intent(Intent.ACTION_VIEW,
							uri));
				}
			});
			break;
		default:
			myDialog = null;
		}
		return myDialog;
	}

	/**
	 * Log text.
	 * 
	 * @param text
	 *            text
	 */
	public final void log(final String text) {
		this.log.append(text);
		logString += text;
	}

	/**
	 * Log text + \n.
	 * 
	 * @param text
	 *            text
	 */
	public final void lognl(final String text) {
		this.log.append(text + "\n");
		logString += text + "\n";
	}

	/**
	 * AndGMXsms's MessageHandler.
	 * 
	 * @author flx
	 */
	private class MessageHandler extends Handler {

		/**
		 * Handles incoming messages.
		 * 
		 * @param msg
		 *            message
		 */
		@Override
		public final void handleMessage(final Message msg) {
			switch (msg.what) {
			case MESSAGE_LOG:
				String l = (String) msg.obj;
				AndGMXsms.this.lognl(l);
				return;
			case MESSAGE_FREECOUNT:
				AndGMXsms.remFree = (String) msg.obj;
				TextView tw = (TextView) AndGMXsms.this
						.findViewById(R.id.freecount);
				tw.setText(AndGMXsms.this.getResources().getString(
						R.string.free_)
						+ " " + AndGMXsms.remFree);
				return;
			case MESSAGE_SEND:
				AndGMXsms.connector = new Connector()
						.execute((String[]) msg.obj);
				return;
			case MESSAGE_BOOTSTRAP:
				AndGMXsms.connector = new Connector()
						.execute((String[]) msg.obj);
				return;
			case MESSAGE_SETTINGS:
				AndGMXsms.this.startActivity(new Intent(AndGMXsms.this,
						Settings.class));
			default:
				return;
			}
		}
	}

}
