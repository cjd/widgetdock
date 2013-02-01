package com.adebenham.widgetdock;

import java.util.ArrayList;

import com.adebenham.widgetdock.Preferences;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.TableLayout.LayoutParams;

import com.adebenham.widgetdock.R;

public class WidgetDock extends Activity {

	static final String TAG = "WidgetDock";

	private AppWidgetManager mAppWidgetManager;
	private AppWidgetHost mAppWidgetHost;
	private AudioManager mAudioManager;
	private ViewGroup mainlayout;
	private ArrayList<AppWidgetHostView> widgets = new ArrayList<AppWidgetHostView>();
	private WidgIdProvider datasource;
	private RelativeLayout welcomeLayout;
	private int originalPhoneMode;
	private float originalBrightness;

	/**
	 * Called on the creation of the activity.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
        this.registerReceiver(new DetectPowerConnectedStateReceiver(), new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

		mainlayout = (ViewGroup) findViewById(R.id.main_layout);

		mAppWidgetManager = AppWidgetManager.getInstance(this);
		mAppWidgetHost = new AppWidgetHost(this, R.id.APPWIDGET_HOST_ID);
		
		datasource = new WidgIdProvider(this);
		datasource.open();
		mAudioManager = (AudioManager)getSystemService(AUDIO_SERVICE);

		welcomeLayout = (RelativeLayout) findViewById(R.id.welcome_screen);

		findViewById(R.id.button_add).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				selectWidget();
			}
		});
		findViewById(R.id.button_prefs).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent settingsActivity = new Intent(getBaseContext(),
						Preferences.class);
				startActivity(settingsActivity);
			}
		});
		makeWidgets();
	}

	/**
	 * Launches the menu to select the widget. The selected widget will be on
	 * the result of the activity.
	 */
	void selectWidget() {
		int appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
		Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
		pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		addEmptyData(pickIntent);
		startActivityForResult(pickIntent, R.id.REQUEST_PICK_APPWIDGET);
	}

	/**
	 * This avoids a bug in the com.android.settings.AppWidgetPickActivity,
	 * which is used to select widgets. This just adds empty extras to the
	 * intent, avoiding the bug.
	 * 
	 * See more: http://code.google.com/p/android/issues/detail?id=4272
	 */
	void addEmptyData(Intent pickIntent) {
		ArrayList<AppWidgetProviderInfo> customInfo = new ArrayList<AppWidgetProviderInfo>();
		pickIntent.putParcelableArrayListExtra(
				AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo);
		ArrayList<Bundle> customExtras = new ArrayList<Bundle>();
		pickIntent.putParcelableArrayListExtra(
				AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras);
	}

	/**
	 * If the user has selected an widget, the result will be in the 'data' when
	 * this function is called.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			if (requestCode == R.id.REQUEST_PICK_APPWIDGET) {
				configureWidget(data);
			} else if (requestCode == R.id.REQUEST_CREATE_APPWIDGET) {
				createWidget(data);
			}
		} else if (resultCode == RESULT_CANCELED && data != null) {
			int appWidgetId = data.getIntExtra(
					AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
			if (appWidgetId != -1) {
				mAppWidgetHost.deleteAppWidgetId(appWidgetId);
			}
		}
	}

	/**
	 * Checks if the widget needs any configuration. If it needs, launches the
	 * configuration activity.
	 */
	private void configureWidget(Intent data) {
		Bundle extras = data.getExtras();
		int appWidgetId = extras
				.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
		AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager
				.getAppWidgetInfo(appWidgetId);
		if (appWidgetInfo.configure != null) {
			Intent intent = new Intent(
					AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
			intent.setComponent(appWidgetInfo.configure);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			startActivityForResult(intent, R.id.REQUEST_CREATE_APPWIDGET);
		} else {
			createWidget(data);
		}
	}

	/**
	 * Creates the widget and adds to our view layout.
	 */
	public void createWidget(Intent data) {
		Bundle extras = data.getExtras();
		int appWidgetId = extras
				.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
		AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager
				.getAppWidgetInfo(appWidgetId);

		createWidgetView(appWidgetId);
		
		String widgetName = appWidgetInfo.toString().replaceAll("AppWidgetProviderInfo\\S+\\{(\\S+)/\\S+\\)", "$1");
		datasource.addWidget(appWidgetId, widgetName);
	}

	private void makeWidgets() {
		Cursor cursor = datasource.getAllWidgInfo();
		while (!cursor.isAfterLast()) {
			WidgInfo widget = datasource.cursorToWidgInfo(cursor);
			Log.d(TAG,"Adding: "+widget.getWid()+" - " + widget.getName());
			createWidgetView(widget.getWid());
			cursor.moveToNext();
		}
		cursor.close();
	}

	private void createWidgetView(int Id) {
		AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager
				.getAppWidgetInfo(Id);
		widgets.add(mAppWidgetHost.createView(this, Id, appWidgetInfo));
		int index = widgets.size()-1;
		
		if (widgets.size()==1) {
			widgets.get(0).setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
			widgets.get(0).setKeepScreenOn(true);
		} else {
			widgets.get(0).setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			widgets.get(index).setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			
		}
		mainlayout.addView(widgets.get(index));// populate the view itself
		welcomeLayout.setVisibility(View.GONE);
	}

	/**
	 * Registers the AppWidgetHost to listen for updates to any widgets this app
	 * has.
	 */
	@Override
	protected void onStart() {
		super.onStart();
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		
		originalPhoneMode = mAudioManager.getRingerMode();
		if (settings.getBoolean("silentmode",false)) {
			mAudioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
		}
		WindowManager.LayoutParams params = getWindow().getAttributes();
		originalBrightness=params.screenBrightness;
		params.screenBrightness = 0.1f;
		if (settings.getBoolean("dimscreen",false)) {
			getWindow().setAttributes(params);
		}
		mAppWidgetHost.startListening();
	}

	/**
	 * Stop listen for updates for our widgets (saving battery).
	 */
	@Override
	protected void onStop() {
		super.onStop();
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		if (settings.getBoolean("silentmode",false)) {
			mAudioManager.setRingerMode(originalPhoneMode);
		}
		WindowManager.LayoutParams params = getWindow().getAttributes();
		params.screenBrightness = originalBrightness;
		if (settings.getBoolean("dimscreen",false)) {
			getWindow().setAttributes(params);
		}
		mAppWidgetHost.stopListening();
	}

	/**
	 * Removes the widget displayed by this AppWidgetId
	 */
	public void removeWidget(int wid) {
		
		datasource.removeWidget(wid);
		mAppWidgetHost.deleteAppWidgetId(wid);
		for (int i=0; i<widgets.size();i++) {
			if (widgets.get(i).getAppWidgetId()==wid) {
				mainlayout.removeView(widgets.get(i));
				widgets.remove(i);
			}
		}

		if (widgets.size()==1) {
			widgets.get(0).setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		} else if (widgets.size()==0) {
			welcomeLayout.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * Handles the menu.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(TAG,
				"Menu selected: " + item.getTitle() + " / " + item.getItemId()
						+ " / " + R.id.addWidget);
		switch (item.getItemId()) {
		case R.id.addWidget:
			selectWidget();
			return true;
		case R.id.removeWidget:
			removeWidgetMenuSelected();
			return false;
		case R.id.settings_menu:
			Intent settingsActivity = new Intent(getBaseContext(),
					Preferences.class);
			startActivity(settingsActivity);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Handle the 'Remove Widget' menu.
	 */
	public void removeWidgetMenuSelected() {
		final String[] widgetList = new String[widgets.size()];
		final int[] widgetIds = new int[widgets.size()];
		int widgetNum = 0;
		Cursor cursor = datasource.getAllWidgInfo();
		while (!cursor.isAfterLast()) {
			WidgInfo widget = datasource.cursorToWidgInfo(cursor);
			widgetList[widgetNum]=widget.getName();
			widgetIds[widgetNum]=widget.getWid();
			widgetNum++;
			cursor.moveToNext();
		}
		cursor.close();
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Pick a widget to remove");
		builder.setItems(widgetList, new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialogInterface, int which) {
				Toast.makeText(getApplicationContext(), R.string.widget_removed_popup, Toast.LENGTH_SHORT).show();
				Log.d(TAG, widgetList[which]+" of id "+widgetIds[which]+" selected");
				removeWidget(widgetIds[which]);			
				return;				
			}
			
		});
		builder.create().show();
	}

	/**
	 * Creates the menu with options to add and remove widgets.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.widget_menu, menu);
		return true;
	}
	
}
