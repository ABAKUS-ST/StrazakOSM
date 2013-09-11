package pl.openstreetmap.dotevo.strazak;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

public class MainActivity extends Activity implements LocationListener,
		Listener, HttpFileUploadResponse, OnGestureListener {

	private static final int REQUEST_GPS_ENABLE = 1;
	private static final int REQUEST_MAP_SHOW = 2;
	private static final int SWIPE_MIN_VELOCITY = 100;
	private static final int SWIPE_MIN_DISTANCE = 100;
	private static final long LOCATION_UPDATE_MIN_TIME_MS = 0;
	private static final float LOCATION_UPDATE_MIN_DISTANCE = 0;
	private static final String URL_SERVER = "http://mapa.abakus.net.pl/mil_up/up1.php";

	public static final String SETTINGS_FILE = "strazak_osm_settings";
	public static final String SELECTED_VIEW_SETTING = "selected_view";
	public static final String LAST_ROADS_SETTING = "last_roads";
	public static final String MAP_SHOW_COMPASS_SETTING = "map_show_compass";
	public static final String MAP_SHOW_SCALE_SETTING = "map_show_scale";
	public static final String MAP_SHOW_HYDRANTS_SETTING = "map_show_hydrants";
	public static final String MAP_SHOW_FORESTS_SETTING = "map_show_forests";

	private ViewSwitcher switcher;
	private GestureDetector gestureDetector;
	private LocationManager locationManager;
	private Handler handler;

	private HydrantsView hydrantsView;
	private ChainageView chainageView;
	private static Location location;
	private boolean isGpsWarningShown;
	private boolean isChildActivityRunning;

	public static Location getLocation() {
		return location;
	}

	public Handler getHandler() {
		return handler;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		handler = new Handler();

		// check for GPS
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			showDialogGpsDisabled();
		}

		// check for external storage
		String extStorageState = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
			// We can read and write the media
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
			// We can only read the media
			showDialogFatalError(R.string.errorStorageRO);
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need to know is we can neither read nor write
			showDialogFatalError(R.string.errorStorageUnavailable);
		}

		initializeControls();

		gestureDetector = new GestureDetector(this, this);

		hydrantsView = new HydrantsView(this);
		chainageView = new ChainageView(this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		locationManager
				.requestLocationUpdates(LocationManager.GPS_PROVIDER,
						LOCATION_UPDATE_MIN_TIME_MS,
						LOCATION_UPDATE_MIN_DISTANCE, this);
		locationManager.addGpsStatusListener(this);

		SharedPreferences settings = getSharedPreferences(SETTINGS_FILE,
				Context.MODE_PRIVATE);

		if (settings.contains(SELECTED_VIEW_SETTING)) {
			int selectedView = switcher.getDisplayedChild();
			int lastSelectedView = settings.getInt(SELECTED_VIEW_SETTING, 0);
			if (selectedView != lastSelectedView) {
				switcher.setDisplayedChild(lastSelectedView);
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		((StApplication) getApplication()).flushFile();

		locationManager.removeUpdates(this);
		locationManager.removeGpsStatusListener(this);

		Editor settingsEdit = getSharedPreferences(SETTINGS_FILE,
				Context.MODE_PRIVATE).edit();
		settingsEdit
				.putInt(SELECTED_VIEW_SETTING, switcher.getDisplayedChild());
		settingsEdit.commit();
	}

	@Override
	protected void onStop() {
		if (!isChildActivityRunning) {
			((StApplication) getApplication()).closeFile();
		}

		super.onStop();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		int selectedView = switcher.getDisplayedChild();
		final String hydrantRef = hydrantsView.getHydrantRef();
		final String chainageRef = chainageView.getRef();
		final String chainageDistance = chainageView.getDistance();

		setContentView(R.layout.activity_main);

		initializeControls();

		if (selectedView != switcher.getDisplayedChild()) {
			switcher.setDisplayedChild(selectedView);
		}

		handler.post(new Runnable() {

			@Override
			public void run() {
				hydrantsView.initializeControls();
				hydrantsView.setHydrantRef(hydrantRef);

				chainageView.initializeControls();
				chainageView.setRef(chainageRef);
				chainageView.setDistance(chainageDistance);
				chainageView.checkRoadTableRowVisibility();
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_GPS_ENABLE:
			isChildActivityRunning = false;
			isGpsWarningShown = false;

			LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
			if (!locationManager
					.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				showDialogGpsDisabled();
			}

			break;
		case REQUEST_MAP_SHOW:
			isChildActivityRunning = false;
			break;
		default:
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.quit:
			finish();
			return true;
		case R.id.upload:
			showDialogUploadWarning();
			return true;
		case R.id.changeview:
			chainageView.setViewChanging(true);
			nextView();
			chainageView.setViewChanging(false);
			return true;
		case R.id.info:
			showDialogInfo();
			return true;
		case R.id.showmap:
			isChildActivityRunning = true;

			Intent intent = new Intent(MainActivity.this, OsmMapActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivityForResult(intent, REQUEST_MAP_SHOW);

			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onGpsStatusChanged(int event) {
		switch (event) {
		case GpsStatus.GPS_EVENT_STARTED:
			setTitle(R.string.statusStartedGPS);
			break;
		case GpsStatus.GPS_EVENT_STOPPED:
			setTitle(R.string.statusStoppedGPS);
			break;
		case GpsStatus.GPS_EVENT_FIRST_FIX:
			setTitle(R.string.statusFixGPS);
			break;
		case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
			GpsStatus gpsStatus = locationManager.getGpsStatus(null);

			int usedSats = 0;
			for (GpsSatellite sat : gpsStatus.getSatellites()) {
				if (sat.usedInFix()) {
					usedSats++;
				}
			}

			String gpsText = "";
			if (usedSats == 0) {
				gpsText = "brak (0)";
			} else if (usedSats >= 1 && usedSats <= 3) {
				gpsText = "s³aby (1-3)";
			} else if (usedSats >= 4 && usedSats <= 6) {
				gpsText = "dobry (4-6)";
			} else if (usedSats >= 7) {
				gpsText = "znakomity (7+)";
			}

			setTitle(getString(R.string.app_name) + ". GPS: " + gpsText);
			break;
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// useless, doesn't get called when a GPS fix is available
	}

	@Override
	public void onProviderDisabled(String provider) {
		if (provider.equals(LocationManager.GPS_PROVIDER)) {
			showDialogGpsDisabled();
		}
	}

	@Override
	public void onProviderEnabled(String provider) {
		// ignored. GPS availability is checked on startup
	}

	@Override
	public void onLocationChanged(Location location) {
		MainActivity.location = location;

		hydrantsView.setEnableButtons();
		chainageView.setEnableButtons();
	}

	@Override
	public void uploadResponse(final int code, final File file) {
		if (code == 200) {
			file.renameTo(new File(file.getParent() + "/sended/"
					+ file.getName()));

			handler.post(new Runnable() {

				@Override
				public void run() {
					Toast.makeText(
							MainActivity.this,
							"Plik " + file.getName()
									+ " zosta³ przes³any poprawnie",
							Toast.LENGTH_SHORT).show();
				}
			});
		} else {
			handler.post(new Runnable() {

				@Override
				public void run() {
					Toast.makeText(
							MainActivity.this,
							"UWAGA! Przesy³anie pliku " + file.getName()
									+ " nie powiod³o siê (b³¹d: " + code
									+ ") SPRÓBUJ PONOWNIE", Toast.LENGTH_SHORT)
							.show();
				}
			});
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return gestureDetector.onTouchEvent(event);
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		// Get Position
		float ev1X = e1.getX();
		float ev2X = e2.getX();

		// Get distance of X (e1) to X (e2)
		final float xdistance = Math.abs(ev1X - ev2X);
		// Get veclocity of cusor
		final float xvelocity = Math.abs(velocityX);

		if ((xvelocity > SWIPE_MIN_VELOCITY)
				&& (xdistance > SWIPE_MIN_DISTANCE)) {
			if (ev1X > ev2X)// Switch Left
			{
				chainageView.setViewChanging(true);
				previousView();
				chainageView.setViewChanging(false);
			} else {
				// Switch Right
				chainageView.setViewChanging(true);
				nextView();
				chainageView.setViewChanging(false);
			}
		}

		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	public void changeViewColor(Integer viewResId, Integer color) {
		if (viewResId != null) {
			changeViewColor(findViewById(viewResId), color);
		}
	}

	public void changeViewColor(View view, Integer color) {
		if (view != null) {
			Drawable d = view.getBackground();
			view.invalidateDrawable(d);

			if (color != null) {
				PorterDuffColorFilter filter = new PorterDuffColorFilter(color,
						PorterDuff.Mode.SRC_ATOP);
				d.setColorFilter(filter);
			} else {
				d.clearColorFilter();
			}
		}
	}

	private void initializeControls() {
		switcher = (ViewSwitcher) findViewById(R.id.viewSwitcher1);
	}

	private void sendData(String name) {
		((StApplication) getApplication()).newOSMFile();
		File kpmFolder = new File(((StApplication) getApplication()).extStorage
				+ "/StrazakOSM");
		File[] files = kpmFolder.listFiles();
		for (File file : files) {
			if (file.isFile()
					&& file.length() > 0
					&& file.getPath().equals(
							((StApplication) getApplication()).osmWriter.path) == false) {
				new Thread(new HttpFileUpload(URL_SERVER, file, name, this))
						.start();
			}
		}
	}

	private void previousView() {
		switcher.setInAnimation(this, R.anim.in_animation1);
		switcher.setOutAnimation(this, R.anim.out_animation1);
		switcher.showPrevious();
	}

	private void nextView() {
		switcher.setInAnimation(this, R.anim.in_animation);
		switcher.setOutAnimation(this, R.anim.out_animation);
		switcher.showNext();
	}

	// =============================Alerts========================================

	public void showDialogFatalError(int messageId) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.error)
				.setMessage(messageId)
				.setCancelable(false)
				.setNegativeButton(R.string.quit,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								finish();
							}
						});

		builder.show();
	}

	private void showDialogNoInternet() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.errorNoInternet)
				.setMessage(R.string.queryNoUpload).setCancelable(false)
				.setIcon(R.drawable.transmit_error)
				.setNegativeButton(R.string.cancel, null);

		builder.show();
	}

	private void showDialogGpsDisabled() {
		if (!isGpsWarningShown) {
			isGpsWarningShown = true;

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.errorGpsDisabled)
					.setMessage(R.string.queryGpsDisabled)
					.setCancelable(false)
					.setNegativeButton(R.string.quit,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									finish();
								}
							})
					.setPositiveButton(R.string.systemSettings,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									isChildActivityRunning = true;

									startActivityForResult(
											new Intent(
													android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS),
											REQUEST_GPS_ENABLE);
								}
							});

			builder.show();
		}
	}

	private void showDialogUploadWarning() {
		// Check for internet
		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		if (cm.getActiveNetworkInfo() == null
				|| !cm.getActiveNetworkInfo().isConnectedOrConnecting()) {
			showDialogNoInternet();
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.warning)
					.setMessage(R.string.send_warning)
					.setPositiveButton(R.string.next,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									showDialogUpload();
								}
							});

			builder.show();
		}
	}

	private void showDialogUpload() {
		LayoutInflater li = LayoutInflater.from(this);
		View promptsView = li.inflate(R.layout.name_dialog, null);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(promptsView);

		final EditText userInput = (EditText) promptsView
				.findViewById(R.id.editTextDialogUserInput);

		handler.postDelayed(new Runnable() {

			public void run() {
				MotionEvent downEvent = MotionEvent.obtain(
						SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
						MotionEvent.ACTION_DOWN, 0, 0, 0);
				MotionEvent upEvent = MotionEvent.obtain(
						SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
						MotionEvent.ACTION_UP, 0, 0, 0);

				userInput.dispatchTouchEvent(downEvent);
				userInput.dispatchTouchEvent(upEvent);

				downEvent.recycle();
				upEvent.recycle();
			}
		}, 200);

		builder.setCancelable(false)
				.setTitle(R.string.input_phone)
				.setPositiveButton(R.string.upload,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								sendData(userInput.getText().toString());
							}
						}).setNegativeButton(R.string.cancel, null);

		AlertDialog dialog = builder.create();

		dialog.show();

		final Button positiveButton = dialog
				.getButton(DialogInterface.BUTTON_POSITIVE);
		positiveButton.setEnabled(false);

		userInput.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				String text = userInput.getText().toString();
				if ("".equals(text)) {
					positiveButton.setEnabled(false);
				} else {
					boolean hasDigitsOnly = true;
					boolean hasLetter = false;
					boolean hasDot = false;
					boolean hasAt = false;

					for (int i = 0; i < text.length(); i++) {
						hasDigitsOnly &= Character.isDigit(text.charAt(i));
						hasLetter |= Character.isLetter(text.charAt(i));
						hasDot |= Character.valueOf('.').equals(text.charAt(i))
								&& i > 0 && i < text.length() - 1;
						hasAt |= Character.valueOf('@').equals(text.charAt(i))
								&& i > 0 && i < text.length() - 1;
					}

					positiveButton.setEnabled((hasDigitsOnly && text.length() >= 9)
							|| (hasLetter && hasDot && hasAt));
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});
	}

	private void showDialogInfo() {
		LayoutInflater li = LayoutInflater.from(this);
		View promptsView = li.inflate(R.layout.info, null);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(promptsView);

		TextView emailText = (TextView) promptsView
				.findViewById(R.id.textViewMail);
		emailText.setMovementMethod(LinkMovementMethod.getInstance());

		TextView authorText = (TextView) promptsView
				.findViewById(R.id.textViewAuthor);
		authorText.setMovementMethod(LinkMovementMethod.getInstance());

		TextView versionText = (TextView) promptsView
				.findViewById(R.id.textViewVersion);
		try {
			versionText
					.setText("Wersja "
							+ getPackageManager().getPackageInfo(
									getPackageName(), 0).versionName);
		} catch (NameNotFoundException e) {
			versionText.setVisibility(View.GONE);
		}

		builder.setCancelable(false).setPositiveButton(R.string.ok, null);

		builder.show();
	}
}