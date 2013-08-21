package pl.openstreetmap.dotevo.strazak;

import java.util.HashMap;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
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

		hydrantAdd = (Button) mainActivity.findViewById(R.id.hydrant_add);
		hydrantAddWp = (Button) mainActivity.findViewById(R.id.hydrant_add_wp);
		hydrantRef = (EditText) mainActivity.findViewById(R.id.hydrant_ref);

		hydrantAdd.setOnClickListener(this);
		hydrantAddWp.setOnClickListener(this);

		mainActivity.findViewById(R.id.pillar_100).setOnClickListener(this);
		mainActivity.findViewById(R.id.pillar_80).setOnClickListener(this);
		mainActivity.findViewById(R.id.pillar_x).setOnClickListener(this);
		mainActivity.findViewById(R.id.wall_100).setOnClickListener(this);
		mainActivity.findViewById(R.id.wall_80).setOnClickListener(this);
		mainActivity.findViewById(R.id.wall_x).setOnClickListener(this);
		mainActivity.findViewById(R.id.underground_100)
				.setOnClickListener(this);
		mainActivity.findViewById(R.id.underground_80).setOnClickListener(this);
		mainActivity.findViewById(R.id.underground_x).setOnClickListener(this);
		mainActivity.findViewById(R.id.other).setOnClickListener(this);
		mainActivity.findViewById(R.id.pond).setOnClickListener(this);
		mainActivity.findViewById(R.id.wpoint).setOnClickListener(this);

		setEnableButtons();
	}

	@Override
	public void onClick(View arg0) {
		changeViewColor(lastButtonId, null);

		lastButtonId = arg0.getId();

		if (lastButtonId != R.id.hydrant_add
				&& lastButtonId != R.id.hydrant_add_wp) {
			changeViewColor(lastButtonId, Color.GREEN);
		}

		switch (arg0.getId()) {
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
		case R.id.wall_80:
			hydrantSize = 80;
			hydrantType = WALL;
			break;
		case R.id.wall_100:
			hydrantSize = 100;
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

	public void setEnableButtons() {
		if (hydrantType == -1 || mainActivity.getLocation() == null) {
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
		if (mainActivity.getLocation() == null) {
			Toast.makeText(mainActivity, "Poczekaj na ustalenie pozycji GPS",
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

		tags.put("source", "GSM (APK:StrazakOSM)");

		if (hydrantRef.getText().length() > 0) {
			tags.put("ref", hydrantRef.getText().toString());
		}

		try {
			if (((StApplication) mainActivity.getApplication()).osmWriter == null) {
				((StApplication) mainActivity.getApplication()).newOSMFile();
			}

			((StApplication) mainActivity.getApplication()).osmWriter.addNode(
					mainActivity.getLocation().getLatitude(), mainActivity
							.getLocation().getLongitude(), tags);
		} catch (Exception e) {
			mainActivity.showDialogFatalError(R.string.errorFileOpen);
			return;
		}

		Toast.makeText(mainActivity, "Nowy hydrant typu: " + type,
				Toast.LENGTH_LONG).show();

		hydrantType = -1;
		hydrantSize = -1;
		hydrantPlace = -1;
		hydrantRef.setText("");

		setEnableButtons();
	}

	private void changeViewColor(Integer buttonResId, Integer color) {
		if (buttonResId != null) {
			View view = mainActivity.findViewById(buttonResId);
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

	private void showDialogAddPlace() {
		final CharSequence[] places = { "Droga", "Trawa", "Zatoka", "Chodnik" };
		AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
		builder.setTitle("Wybierz miejsce");
		builder.setSingleChoiceItems(places, -1,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						hydrantPlace = which;
						addHydrant();
					}
				}).setNegativeButton("Anuluj",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						hydrantPlace = -1;
					}
				});

		builder.show();
	}

	private void showDialogNumber() {
		LayoutInflater li = LayoutInflater.from(mainActivity);
		View promptsView = li.inflate(R.layout.number, null);
		AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
		builder.setView(promptsView);

		final EditText userInput = (EditText) promptsView
				.findViewById(R.id.editTextDialogUserInput);

		builder.setCancelable(false)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						if (userInput.getText().length() > 0) {
							hydrantSize = Integer.parseInt(userInput.getText()
									.toString());
						} else {
							hydrantSize = -1;
						}
					}
				})
				.setNegativeButton("Anuluj",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								hydrantSize = -1;
							}
						});

		builder.show();
	}
}
