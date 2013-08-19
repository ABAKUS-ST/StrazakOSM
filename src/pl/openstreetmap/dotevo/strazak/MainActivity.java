package pl.openstreetmap.dotevo.strazak;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
	private static final int OTHER = 100;
	private static final int WPOINT = 100;

	private EditText ref_edit;
	private EditText pk_edit;
	private TextView status_label;
	private Button add_button;
	private LocationManager locationManager = null;
	private static final long locationUpdateMinTimeMs = 0; // minimum time for
															// location updates
															// in ms
	private static final float locationUpdateMinDistance = 0; // minimum
																// distance for
																// location
																// updates in m
	private Location location;

	ViewSwitcher switcher;
	View pikView;
	View hydrantView;

	int hydrant_size = -1;
	int hydrant_type = -1;
	int hydrant_place = -1;
	private Button hydrant_add;
	private Button hydrant_add_wp;
	private EditText hydrant_ref;

	private GestureDetector gesturedetector = null;

	private int lastclick = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
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

		pk_edit = (EditText) findViewById(R.id.pk_edit);
		ref_edit = (EditText) findViewById(R.id.ref_edit);
		status_label = (TextView) findViewById(R.id.status);
		add_button = (Button) findViewById(R.id.add_button);
		hydrant_add = (Button) findViewById(R.id.hydrant_add);
		hydrant_add_wp = (Button) findViewById(R.id.hydrant_add_wp);
		hydrant_ref = (EditText) findViewById(R.id.hydrant_ref);
		add_button.setOnClickListener(this);

		findViewById(R.id.plus_button).setOnClickListener(this);
		findViewById(R.id.minus_button).setOnClickListener(this);

		status_label.setText("Hello");

		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				locationUpdateMinTimeMs, locationUpdateMinDistance, this);
		locationManager.addGpsStatusListener(this);

		findViewById(R.id.hydrant_add).setOnClickListener(this);
		findViewById(R.id.hydrant_add_wp).setOnClickListener(this);
		findViewById(R.id.pillar_100).setOnClickListener(this);
		findViewById(R.id.pillar_50).setOnClickListener(this);
		findViewById(R.id.pillar_x).setOnClickListener(this);
		findViewById(R.id.wall_100).setOnClickListener(this);
		findViewById(R.id.wall_50).setOnClickListener(this);
		findViewById(R.id.wall_x).setOnClickListener(this);
		findViewById(R.id.underground_100).setOnClickListener(this);
		findViewById(R.id.underground_50).setOnClickListener(this);
		findViewById(R.id.underground_x).setOnClickListener(this);
		findViewById(R.id.other).setOnClickListener(this);
		findViewById(R.id.pond).setOnClickListener(this);
		findViewById(R.id.wpoint).setOnClickListener(this);

		switcher = (ViewSwitcher) findViewById(R.id.viewSwitcher1);
		pikView = findViewById(R.id.view1);
		hydrantView = findViewById(R.id.view2);

		gesturedetector = new GestureDetector(this, this);
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
			add_button.setEnabled(true);
		}
	};

	@Override
	public void onClick(View arg0) {
		switch (arg0.getId()) {
		case R.id.minus_button:
			lastclick = -1;
			pk_edit.setText(""
					+ (Integer.parseInt(pk_edit.getText().toString()) - 1));
			break;
		case R.id.add_button:
			Log.i("Pikietaz", "new point");
			if (location == null) {
				Log.e("Pikietaz", "no location!");
				return;
			}
			HashMap<String, String> tags = new HashMap<String, String>();
			tags.put("highway", "milestone");
			tags.put("pk", pk_edit.getText().toString());
			tags.put("source", "GPS");
			tags.put("ref", ref_edit.getText().toString());
			try {
				if (((StApplication) this.getApplication()).osmWriter == null)
					((StApplication) this.getApplication()).newOSMFile();
				((StApplication) this.getApplication()).osmWriter.addNode(
						location.getLatitude(), location.getLongitude(), tags);
			} catch (IOException e) {
				showDialogFatalError(R.string.errorFileOpen);
			}
			Toast.makeText(this, "pk=" + pk_edit.getText().toString(),
					Toast.LENGTH_LONG).show();
			pk_edit.setText(""
					+ (Integer.parseInt(pk_edit.getText().toString()) + lastclick));

			Handler myHandler = new Handler();
			add_button.setEnabled(false);
			myHandler.postDelayed(unlockButton, 1000);
			break;
		case R.id.plus_button:
			lastclick = 1;
			pk_edit.setText(""
					+ (Integer.parseInt(pk_edit.getText().toString()) + 1));
			break;
		// HYDRANT
		case R.id.pillar_50:
			hydrant_size = 75;
			hydrant_type = PILLAR;
			break;
		case R.id.pillar_100:
			hydrant_size = 110;
			hydrant_type = PILLAR;
			break;
		case R.id.pillar_x:
			showDialogNumber();
			hydrant_type = PILLAR;
			break;
		case R.id.underground_50:
			hydrant_size = 75;
			hydrant_type = UNDERGROUND;
			break;
		case R.id.underground_100:
			hydrant_size = 110;
			hydrant_type = UNDERGROUND;
			break;
		case R.id.underground_x:
			showDialogNumber();
			hydrant_type = UNDERGROUND;
			break;
		case R.id.wall_50:
			hydrant_size = 75;
			hydrant_type = WALL;
			break;
		case R.id.wall_100:
			hydrant_size = 110;
			hydrant_type = WALL;
			break;
		case R.id.wall_x:
			showDialogNumber();
			hydrant_type = WALL;
			break;
		case R.id.pond:
			hydrant_type = POND;
			break;
		case R.id.other:
			hydrant_type = OTHER;
			break;
		case R.id.wpoint:
			hydrant_type = WPOINT;
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

	void addHydrant() {
		if (location == null) {
			Toast.makeText(this, "Poczekaj na fix :-)", Toast.LENGTH_LONG)
					.show();
			return;
		}

		String type = "";
		if (hydrant_type == PILLAR)
			type = "pillar";
		else if (hydrant_type == UNDERGROUND)
			type = "underground";
		else if (hydrant_type == WALL)
			type = "wall";
		else if (hydrant_type == POND)
			type = "pond";

		HashMap<String, String> tags = new HashMap<String, String>();
		if (hydrant_type != WPOINT)
			tags.put("emergency", "fire_hydrant");
		if (hydrant_type != OTHER)
			tags.put("fire_hydrant:type", type);
		if (hydrant_size != -1)
			tags.put("fire_hydrant:diameter", String.valueOf(hydrant_size));

		if (hydrant_place == 0) {
			tags.put("fire_hydrant:position", "lane");
		}
		if (hydrant_place == 1) {
			tags.put("fire_hydrant:position", "green");
		}
		if (hydrant_place == 2) {
			tags.put("fire_hydrant:position", "parking lot");
		}
		if (hydrant_place == 3) {
			tags.put("fire_hydrant:position", "sidewalk");
		}

		if (hydrant_ref.getText().length() > 0)
			tags.put("ref", hydrant_ref.getText().toString());

		else {
			tags.put("emergency", "suction_point");
		}

		try {
			if (((StApplication) this.getApplication()).osmWriter == null)
				((StApplication) this.getApplication()).newOSMFile();
			((StApplication) this.getApplication()).osmWriter.addNode(
					location.getLatitude(), location.getLongitude(), tags);
		} catch (IOException e) {
			showDialogFatalError(R.string.errorFileOpen);
			return;
		}
		Toast.makeText(this, "Nowy hydrant typu:" + type, Toast.LENGTH_LONG)
				.show();
		hydrant_type = -1;
		hydrant_size = -1;
		hydrant_place = -1;
		hydrant_ref.setText("");
		setEnableButtons();
	}

	private void setEnableButtons() {
		if (hydrant_type == -1) {
			hydrant_add.setEnabled(false);
			hydrant_add_wp.setEnabled(false);

		} else if (hydrant_type == WPOINT) {
			hydrant_add.setEnabled(true);
			hydrant_add_wp.setEnabled(false);
		} else {
			hydrant_add.setEnabled(true);
			hydrant_add_wp.setEnabled(true);
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		closeApplication();
	}

	String urlServer = "http://mapa.abakus.net.pl/mil_up/up1.php";

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
				new Thread(new HttpFileUpload(urlServer, file, name, this))
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
			status_label.setText(R.string.statusStartedString);
			break;
		case GpsStatus.GPS_EVENT_STOPPED:
			status_label.setText(R.string.statusStoppedString);
			break;
		case GpsStatus.GPS_EVENT_FIRST_FIX:
			status_label.setText(R.string.statusFixString);
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
			status_label.setText("Sat:" + usedSats + "/" + maxSats);
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
		status_label.setText(getString(R.string.statusReadyString,
				location.getAccuracy()));
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
		return gesturedetector.onTouchEvent(event);
	}

	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	int SWIPE_MIN_VELOCITY = 100;
	int SWIPE_MIN_DISTANCE = 100;

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
			} else// Switch Right
			{
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
		final CharSequence[] places = { "Jezdnia", "£¹ka", "Zatoczka",
				"Chodnik" };
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Wybierz miejsce");
		alert.setSingleChoiceItems(places, -1,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						hydrant_place = which;
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
							hydrant_size = Integer.parseInt(userInput.getText()
									.toString());
						else
							hydrant_size = -1;
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

}
