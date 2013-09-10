package pl.openstreetmap.dotevo.strazak;

import java.util.HashMap;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class HydrantsView implements OnClickListener {

	private static final int PILLAR = 1;
	private static final int UNDERGROUND = 2;
	private static final int WALL = 3;
	private static final int POND = 4;
	private static final int SUCTION_POINT = 5;
	private static final int OTHER = 100;

	private Button hydrantAdd;
	private Button hydrantAddWp;
	private EditText hydrantRef;
	private MainActivity mainActivity;

	private int hydrantSize = -1;
	private int hydrantType = -1;
	private int hydrantPlace = -1;
	private Integer lastButtonId = null;

	public HydrantsView(MainActivity mainActivity) {
		this.mainActivity = mainActivity;

		initializeControls();
	}

	public String getHydrantRef() {
		return this.hydrantRef.getText().toString();
	}

	public void setHydrantRef(String hydrantRef) {
		this.hydrantRef.setText(hydrantRef);
	}

	@Override
	public void onClick(View view) {
		if (view.getId() != R.id.hydrant_add
				&& view.getId() != R.id.hydrant_add_wp) {
			mainActivity.changeViewColor(lastButtonId, null);
			lastButtonId = view.getId();
			mainActivity.changeViewColor(lastButtonId, Color.GREEN);

			hydrantType = -1;
			hydrantSize = -1;
			hydrantPlace = -1;
		}

		switch (view.getId()) {
		case R.id.pillar_80:
			hydrantSize = 80;
			hydrantType = PILLAR;
			break;
		case R.id.pillar_100:
			hydrantSize = 100;
			hydrantType = PILLAR;
			break;
		case R.id.pillar_x:
			showDialogNumber();
			hydrantType = PILLAR;
			break;
		case R.id.underground_80:
			hydrantSize = 80;
			hydrantType = UNDERGROUND;
			break;
		case R.id.underground_100:
			hydrantSize = 100;
			hydrantType = UNDERGROUND;
			break;
		case R.id.underground_x:
			showDialogNumber();
			hydrantType = UNDERGROUND;
			break;
		case R.id.wall_25:
			hydrantSize = 25;
			hydrantType = WALL;
			break;
		case R.id.wall_52:
			hydrantSize = 52;
			hydrantType = WALL;
			break;
		case R.id.wall_x:
			showDialogNumber();
			hydrantType = WALL;
			break;
		case R.id.pond:
			hydrantType = POND;
			break;
		case R.id.other:
			hydrantType = OTHER;
			break;
		case R.id.wpoint:
			hydrantType = SUCTION_POINT;
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

	public void initializeControls() {
		hydrantAdd = (Button) mainActivity.findViewById(R.id.hydrant_add);
		hydrantAddWp = (Button) mainActivity.findViewById(R.id.hydrant_add_wp);
		hydrantRef = (EditText) mainActivity.findViewById(R.id.hydrant_ref);

		hydrantAdd.setOnClickListener(this);
		hydrantAddWp.setOnClickListener(this);

		mainActivity.findViewById(R.id.pillar_100).setOnClickListener(this);
		mainActivity.findViewById(R.id.pillar_80).setOnClickListener(this);
		mainActivity.findViewById(R.id.pillar_x).setOnClickListener(this);
		mainActivity.findViewById(R.id.wall_52).setOnClickListener(this);
		mainActivity.findViewById(R.id.wall_25).setOnClickListener(this);
		mainActivity.findViewById(R.id.wall_x).setOnClickListener(this);
		mainActivity.findViewById(R.id.underground_100)
				.setOnClickListener(this);
		mainActivity.findViewById(R.id.underground_80).setOnClickListener(this);
		mainActivity.findViewById(R.id.underground_x).setOnClickListener(this);
		mainActivity.findViewById(R.id.other).setOnClickListener(this);
		mainActivity.findViewById(R.id.pond).setOnClickListener(this);
		mainActivity.findViewById(R.id.wpoint).setOnClickListener(this);

		setEnableButtons();

		if (lastButtonId != null) {
			mainActivity.changeViewColor(lastButtonId, Color.GREEN);
		}
	}

	public void setEnableButtons() {
		if (hydrantType == -1 || MainActivity.getLocation() == null) {
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

	private void addHydrant() {
		mainActivity.changeViewColor(lastButtonId, null);
		lastButtonId = null;

		if (MainActivity.getLocation() == null) {
			Toast.makeText(mainActivity, R.string.waitForFixGPS,
					Toast.LENGTH_LONG).show();
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

		tags.put("source", "GPS(APK:StrazakOSM)");

		if (hydrantRef.getText().length() > 0) {
			tags.put("ref", hydrantRef.getText().toString());
		}

		try {
			if (((StApplication) mainActivity.getApplication()).osmWriter == null) {
				((StApplication) mainActivity.getApplication()).newOSMFile();
			}

			((StApplication) mainActivity.getApplication()).osmWriter.addNode(
					MainActivity.getLocation().getLatitude(), MainActivity
							.getLocation().getLongitude(), tags);
		} catch (Exception e) {
			mainActivity.showDialogFatalError(R.string.errorFileOpen);
			return;
		}

		Toast.makeText(mainActivity, "Nowy hydrant typu: " + type,
				Toast.LENGTH_SHORT).show();

		hydrantType = -1;
		hydrantSize = -1;
		hydrantPlace = -1;
		hydrantRef.setText("");

		setEnableButtons();
	}

	private void showDialogAddPlace() {
		final CharSequence[] places = { "Droga", "Trawa", "Zatoka", "Chodnik" };
		AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
		builder.setTitle(R.string.pick_place)
				.setSingleChoiceItems(places, -1,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								hydrantPlace = which;
								addHydrant();
								dialog.cancel();
							}
						})
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								hydrantPlace = -1;
							}
						});

		builder.show();
	}

	private void showDialogNumber() {
		LayoutInflater li = LayoutInflater.from(mainActivity);
		View promptsView = li.inflate(R.layout.number_dialog, null);
		AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
		builder.setView(promptsView);

		final EditText userInput = (EditText) promptsView
				.findViewById(R.id.editTextDialogUserInput);

		mainActivity.getHandler().postDelayed(new Runnable() {

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

		builder.setTitle(R.string.input_size)
				.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								if (userInput.getText().length() > 0) {
									hydrantSize = Integer.parseInt(userInput
											.getText().toString());
								} else {
									hydrantSize = -1;
								}
							}
						})
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								hydrantSize = -1;
							}
						});

		builder.show();
	}
}
