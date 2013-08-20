package pl.openstreetmap.dotevo.strazak;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

public class MainActivity extends Activity implements OnClickListener,
		LocationListener, Listener, HttpFileUploadResponse, OnGestureListener {

	private static final int REQUEST_GPS_ENABLE = 1;
	private static final int PILLAR = 1;
	private static final int UNDERGROUND = 2;
	private static final int WALL = 3;
	private static final int POND = 4;
	private static final int SUCTION_POINT = 5;
	private static final int OTHER = 100;
	private static final int SWIPE_MIN_VELOCITY = 100;
	private static final int SWIPE_MIN_DISTANCE = 100;
	private static final long LOCATION_UPDATE_MIN_TIME_MS = 0;
	private static final float LOCATION_UPDATE_MIN_DISTANCE = 0;
	private static final String URL_SERVER = "http://mapa.abakus.net.pl/mil_up/up1.php";

	private EditText refEdit;
	private EditText distanceEdit;
	private TextView statusLabel;
	private Button addButton;
	private ViewSwitcher switcher;
	private Button hydrantAdd;
	private Button hydrantAddWp;
	private EditText hydrantRef;
	private GestureDetector gestureDetector = null;

	private LocationManager locationManager = null;
	private Location location;

	private int hydrantSize = -1;
	private int hydrantType = -1;
	private int hydrantPlace = -1;
	private int lastClick = 0;
	private Integer lastButtonId = null;

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

		distanceEdit = (EditText) findViewById(R.id.distance_edit);
		refEdit = (EditText) findViewById(R.id.ref_edit);
		statusLabel = (TextView) findViewById(R.id.status);
		addButton = (Button) findViewById(R.id.add_button);
		hydrantAdd = (Button) findViewById(R.id.hydrant_add);
		hydrantAddWp = (Button) findViewById(R.id.hydrant_add_wp);
		hydrantRef = (EditText) findViewById(R.id.hydrant_ref);

		addButton.setOnClickListener(this);

		findViewById(R.id.plus_button).setOnClickListener(this);
		findViewById(R.id.minus_button).setOnClickListener(this);

		statusLabel.setText("Hello");

		locationManager
				.requestLocationUpdates(LocationManager.GPS_PROVIDER,
						LOCATION_UPDATE_MIN_TIME_MS,
						LOCATION_UPDATE_MIN_DISTANCE, this);
		locationManager.addGpsStatusListener(this);

		findViewById(R.id.hydrant_add).setOnClickListener(this);
		findViewById(R.id.hydrant_add_wp).setOnClickListener(this);
		findViewById(R.id.pillar_100).setOnClickListener(this);
		findViewById(R.id.pillar_80).setOnClickListener(this);
		findViewById(R.id.pillar_x).setOnClickListener(this);
		findViewById(R.id.wall_100).setOnClickListener(this);
		findViewById(R.id.wall_80).setOnClickListener(this);
		findViewById(R.id.wall_x).setOnClickListener(this);
		findViewById(R.id.underground_100).setOnClickListener(this);
		findViewById(R.id.underground_80).setOnClickListener(this);
		findViewById(R.id.underground_x).setOnClickListener(this);
		findViewById(R.id.other).setOnClickListener(this);
		findViewById(R.id.pond).setOnClickListener(this);
		findViewById(R.id.wpoint).setOnClickListener(this);

		switcher = (ViewSwitcher) findViewById(R.id.viewSwitcher1);

		gestureDetector = new GestureDetector(this, this);

		setEnableButtons();
	}

	@Override
	protected void onPause() {
		super.onPause();
		((StApplication) this.getApplication()).flushFile();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		locationManager.removeUpdates(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private Runnable unlockButton = new Runnable() {
		@Override
		public void run() {
			addButton.setEnabled(true);
		}
	};

	@Override
	public void onClick(View arg0) {
		changeViewColor(lastButtonId, null);
		lastButtonId = arg0.getId();

		switch (arg0.getId()) {
		case R.id.minus_button:
			lastClick = -1;
			distanceEdit
					.setText(""
							+ (Integer.parseInt(distanceEdit.getText()
									.toString()) - 1));
			break;
		case R.id.add_button:
			Log.i("Pikietaz", "new point");
			if (location == null) {
				Log.e("Pikietaz", "no location!");
				return;
			}

			HashMap<String, String> tags = new HashMap<String, String>();
			tags.put("highway", "milestone");
			tags.put("distance", distanceEdit.getText().toString());
			tags.put("source", "GPS");
			tags.put("ref", refEdit.getText().toString());
			try {
				if (((StApplication) this.getApplication()).osmWriter == null)
					((StApplication) this.getApplication()).newOSMFile();
				((StApplication) this.getApplication()).osmWriter.addNode(
						location.getLatitude(), location.getLongitude(), tags);
			} catch (IOException e) {
				showDialogFatalError(R.string.errorFileOpen);
			}

			Toast.makeText(this,
					"distance=" + distanceEdit.getText().toString(),
					Toast.LENGTH_LONG).show();
			distanceEdit
					.setText(""
							+ (Integer.parseInt(distanceEdit.getText()
									.toString()) + lastClick));

			Handler myHandler = new Handler();
			addButton.setEnabled(false);
			myHandler.postDelayed(unlockButton, 1000);
			break;
		case R.id.plus_button:
			lastClick = 1;
			distanceEdit
					.setText(""
							+ (Integer.parseInt(distanceEdit.getText()
									.toString()) + 1));
			break;
		// HYDRANT
		case R.id.pillar_80:
			hydrantSize = 80;
			hydrantType = PILLAR;
			changeViewColor(lastButtonId, Color.GREEN);
			break;
		case R.id.pillar_100:
			hydrantSize = 100;
			hydrantType = PILLAR;
			changeViewColor(lastButtonId, Color.GREEN);
			break;
		case R.id.pillar_x:
			showDialogNumber();
			hydrantType = PILLAR;
			changeViewColor(lastButtonId, Color.GREEN);
			break;
		case R.id.underground_80:
			hydrantSize = 80;
			hydrantType = UNDERGROUND;
			changeViewColor(lastButtonId, Color.GREEN);
			break;
		case R.id.underground_100:
			hydrantSize = 100;
			hydrantType = UNDERGROUND;
			changeViewColor(lastButtonId, Color.GREEN);
			break;
		case R.id.underground_x:
			showDialogNumber();
			hydrantType = UNDERGROUND;
			changeViewColor(lastButtonId, Color.GREEN);
			break;
		case R.id.wall_80:
			hydrantSize = 80;
			hydrantType = WALL;
			changeViewColor(lastButtonId, Color.GREEN);
			break;
		case R.id.wall_100:
			hydrantSize = 100;
			hydrantType = WALL;
			changeViewColor(lastButtonId, Color.GREEN);
			break;
		case R.id.wall_x:
			showDialogNumber();
			hydrantType = WALL;
			changeViewColor(lastButtonId, Color.GREEN);
			break;
		case R.id.pond:
			hydrantType = POND;
			changeViewColor(lastButtonId, Color.GREEN);
			break;
		case R.id.other:
			hydrantType = OTHER;
			changeViewColor(lastButtonId, Color.GREEN);
			break;
		case R.id.wpoint:
			hydrantType = SUCTION_POINT;
			changeViewColor(lastButtonId, Color.GREEN);
			break;
		case R.id.hydrant_add:
			addHydrant();
			break;
		case R.id.hydrant_add_wp:
			showDialogAddPlace();
			break;
		}

		setEnableButtons();
	}

	private void addHydrant() {
		if (location == null) {
			Toast.makeText(this, "Poczekaj na fix :-)", Toast.LENGTH_LONG)
					.show();
			return;
		}

		String type = "";
		if (hydrantType == PILLAR)
			type = "pillar";
		else if (hydrantType == UNDERGROUND)
			type = "underground";
		else if (hydrantType == WALL)
			type = "wall";
		else if (hydrantType == POND)
			type = "pond";
		else if (hydrantType == SUCTION_POINT)
			type = "suction_point";
		else if (hydrantType == OTHER)
			type = "other";

		HashMap<String, String> tags = new HashMap<String, String>();

		tags.put("emergency", "fire_hydrant");

		if (hydrantType != OTHER)
			tags.put("fire_hydrant:type", type);
		if (hydrantSize != -1)
			tags.put("fire_hydrant:diameter", String.valueOf(hydrantSize));

		if (hydrantPlace == 0)
			tags.put("fire_hydrant:position", "lane");
		if (hydrantPlace == 1)
			tags.put("fire_hydrant:position", "green");
		if (hydrantPlace == 2)
			tags.put("fire_hydrant:position", "parking_lot");
		if (hydrantPlace == 3)
			tags.put("fire_hydrant:position", "sidewalk");

		tags.put("source", "GSM (APK:StrazakOSM)");

		if (hydrantRef.getText().length() > 0) {
			tags.put("ref", hydrantRef.getText().toString());
		}

		try {
			if (((StApplication) this.getApplication()).osmWriter == null) {
				((StApplication) this.getApplication()).newOSMFile();
			}

			((StApplication) this.getApplication()).osmWriter.addNode(
					location.getLatitude(), location.getLongitude(), tags);
		} catch (IOException e) {
			showDialogFatalError(R.string.errorFileOpen);
			return;
		}

		Toast.makeText(this, "Nowy hydrant typu: " + type, Toast.LENGTH_LONG)
				.show();

		hydrantType = -1;
		hydrantSize = -1;
		hydrantPlace = -1;
		hydrantRef.setText("");

		setEnableButtons();
	}

	private void setEnableButtons() {
		if (hydrantType == -1 || location == null) {
			hydrantAdd.setEnabled(false);
			hydrantAddWp.setEnabled(false);
		} else if (hydrantType == PILLAR || hydrantType == UNDERGROUND) {
			hydrantAdd.setEnabled(true);
			hydrantAddWp.setEnabled(true);
		} else {
			hydrantAdd.setEnabled(true);
			hydrantAddWp.setEnabled(false);
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		closeApplication();
	}

	public void sendData(String name) {
		// New File
		((StApplication) this.getApplication()).newOSMFile();
		File kpmFolder = new File(
				((StApplication) this.getApplication()).extStorage
						+ "/pikietaz");
		File[] files = kpmFolder.listFiles();
		for (File file : files) {
			if (file.isFile()
					&& file.length() > 0
					&& file.getPath()
							.equals(((StApplication) this.getApplication()).osmWriter.path) == false) {
				new Thread(new HttpFileUpload(URL_SERVER, file, name, this))
						.start();
			}
		}
	}

	public void closeApplication() {
		((StApplication) this.getApplication()).closeFile();
		finish();
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.quit:
			closeApplication();
			return true;
		case R.id.upload:
			showDialogUpload();
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
			statusLabel.setText(R.string.statusStartedString);
			break;
		case GpsStatus.GPS_EVENT_STOPPED:
			statusLabel.setText(R.string.statusStoppedString);
			break;
		case GpsStatus.GPS_EVENT_FIRST_FIX:
			statusLabel.setText(R.string.statusFixString);
			break;
		case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
			GpsStatus gpsStatus = locationManager.getGpsStatus(null);
			int maxSats = 0;
			int usedSats = 0;
			Iterable<GpsSatellite> gpsSatellites = gpsStatus.getSatellites();
			for (GpsSatellite sat : gpsSatellites) {
				maxSats++;
				if (sat.usedInFix()) {
					usedSats++;
				}
			}

			statusLabel.setText("Sat:" + usedSats + "/" + maxSats);
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
		statusLabel.setText(getString(R.string.statusReadyString,
				location.getAccuracy()));
		setEnableButtons();
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

	// Next, Previous Views
	private void previousView() {
		// Previous View
		switcher.setInAnimation(this, R.anim.in_animation1);
		switcher.setOutAnimation(this, R.anim.out_animation1);
		switcher.showPrevious();
	}

	private void nextView() {
		// Next View
		switcher.setInAnimation(this, R.anim.in_animation);
		switcher.setOutAnimation(this, R.anim.out_animation);
		switcher.showNext();
	}

	// =============================Alerts========================================
	private void showDialogFatalError(int messageId) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(messageId)
				.setCancelable(false)
				.setNegativeButton(R.string.quit,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								closeApplication();
							}
						});
		builder.create().show();
	}

	private void showDialogGpsDisabled() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.errorGpsDisabled)
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
		builder.create().show();
	}

	private void showDialogUpload() {
		LayoutInflater li = LayoutInflater.from(this);
		View promptsView = li.inflate(R.layout.namedialog, null);
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setView(promptsView);
		final EditText userInput = (EditText) promptsView
				.findViewById(R.id.editTextDialogUserInput);

		alertDialogBuilder
				.setCancelable(false)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						sendData(userInput.getText().toString());
					}
				})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});

		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}

	private void showDialogInfo() {
		LayoutInflater li = LayoutInflater.from(this);
		View promptsView = li.inflate(R.layout.info, null);
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setView(promptsView);

		alertDialogBuilder.setCancelable(false).setPositiveButton("OK",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
					}
				});

		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}

	private void showDialogAddPlace() {
		final CharSequence[] places = { "Jezdnia", "£¹ka", "Zatoka", "Chodnik" };
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Wybierz miejsce");
		alert.setSingleChoiceItems(places, -1,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						hydrantPlace = which;
						addHydrant();
						dialog.cancel();
					}
				}).setNegativeButton("Anuluj",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		alert.show();
	}

	private void showDialogNumber() {
		LayoutInflater li = LayoutInflater.from(this);
		View promptsView = li.inflate(R.layout.number, null);
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setView(promptsView);
		final EditText userInput = (EditText) promptsView
				.findViewById(R.id.editTextDialogUserInput);

		alertDialogBuilder
				.setCancelable(false)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						if (userInput.getText().length() > 0)
							hydrantSize = Integer.parseInt(userInput.getText()
									.toString());
						else
							hydrantSize = -1;
					}
				})
				.setNegativeButton("Anuluj",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
								changeViewColor(lastButtonId, null);
							}
						});

		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}

	private void changeViewColor(Integer buttonResId, Integer color) {
		if (buttonResId != null) {
			View view = findViewById(buttonResId);
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
}