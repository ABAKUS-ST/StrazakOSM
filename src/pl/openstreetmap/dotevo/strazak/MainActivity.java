package pl.openstreetmap.dotevo.strazak;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.text.method.LinkMovementMethod;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ViewSwitcher;

public class MainActivity extends Activity implements LocationListener,
		Listener, HttpFileUploadResponse, OnGestureListener {

	private static final int REQUEST_GPS_ENABLE = 1;
	private static final int SWIPE_MIN_VELOCITY = 100;
	private static final int SWIPE_MIN_DISTANCE = 100;
	private static final long LOCATION_UPDATE_MIN_TIME_MS = 0;
	private static final float LOCATION_UPDATE_MIN_DISTANCE = 0;
	private static final String URL_SERVER = "http://mapa.abakus.net.pl/mil_up/up1.php";

	private ViewSwitcher switcher;
	private GestureDetector gestureDetector;
	private LocationManager locationManager;

	private HydrantsView hydrantsView;
	private ChainageView chainageView;
	private Location location;
	private boolean isGpsWarningShown;

	public Location getLocation() {
		return location;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// check for GPS
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			showDialogGpsDisabled();
		}

		location = locationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);

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

		switcher = (ViewSwitcher) findViewById(R.id.viewSwitcher1);

		locationManager
				.requestLocationUpdates(LocationManager.GPS_PROVIDER,
						LOCATION_UPDATE_MIN_TIME_MS,
						LOCATION_UPDATE_MIN_DISTANCE, this);
		locationManager.addGpsStatusListener(this);

		gestureDetector = new GestureDetector(this, this);

		hydrantsView = new HydrantsView(this);
		chainageView = new ChainageView(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		((StApplication) getApplication()).flushFile();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		locationManager.removeUpdates(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		closeApplication();
	}

	public void sendData(String name) {
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

	public void closeApplication() {
		((StApplication) getApplication()).closeFile();
		finish();
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.quit:
			closeApplication();
			return true;
		case R.id.upload:
			showDialogUploadWarning();
			return true;
		case R.id.changeview:
			nextView();
			return true;
		case R.id.info:
			showDialogInfo();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_GPS_ENABLE:
			isGpsWarningShown = false;
			LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
			if (!locationManager
					.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				showDialogGpsDisabled();
			}

			break;
		default:
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public void onGpsStatusChanged(int event) {
		switch (event) {
		case GpsStatus.GPS_EVENT_STARTED:
			this.setTitle(R.string.statusStartedGPS);
			break;
		case GpsStatus.GPS_EVENT_STOPPED:
			this.setTitle(R.string.statusStoppedGPS);
			break;
		case GpsStatus.GPS_EVENT_FIRST_FIX:
			this.setTitle(R.string.statusFixGPS);
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

			this.setTitle("Stra¿akOSM. GPS: " + gpsText);
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
		this.location = location;

		hydrantsView.setEnableButtons();
		chainageView.setEnableButtons();
	}

	@Override
	public void uploadResponse(int code, File file) {
		if (code == 200) {
			file.renameTo(new File(file.getParent() + "/sended/"
					+ file.getName()));
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
				previousView();
			} else {
				// Switch Right
				nextView();
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

	public void previousView() {
		switcher.setInAnimation(this, R.anim.in_animation1);
		switcher.setOutAnimation(this, R.anim.out_animation1);
		switcher.showPrevious();
	}

	public void nextView() {
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
								closeApplication();
							}
						});

		builder.show();
	}

	private void showDialogGpsDisabled() {
		if (!isGpsWarningShown) {
			isGpsWarningShown = true;

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.errorGpsDisabled)
					.setCancelable(false)
					.setNegativeButton(R.string.quit,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									closeApplication();
								}
							})
					.setPositiveButton(R.string.systemSettings,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
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
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.warning)
				.setMessage(R.string.send_warning)
				.setPositiveButton("Dalej",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								showDialogUpload();
							}
						});

		builder.show();
	}

	private void showDialogUpload() {
		LayoutInflater li = LayoutInflater.from(this);
		View promptsView = li.inflate(R.layout.name_dialog, null);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(promptsView);

		final EditText userInput = (EditText) promptsView
				.findViewById(R.id.editTextDialogUserInput);

		new Handler().postDelayed(new Runnable() {

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
				.setPositiveButton("Wyœlij",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								sendData(userInput.getText().toString());
							}
						}).setNegativeButton("Anuluj", null);

		builder.show();
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

		builder.setCancelable(false).setPositiveButton("OK", null);

		builder.show();
	}
}