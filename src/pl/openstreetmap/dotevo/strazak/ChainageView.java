package pl.openstreetmap.dotevo.strazak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TableRow;
import android.widget.TextView.BufferType;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class ChainageView implements OnClickListener, OnFocusChangeListener,
		OnGlobalLayoutListener, TextWatcher {

	private EditText refEdit;
	private EditText distanceEdit;
	private Button addButton;
	private ImageView imageView;
	private MainActivity mainActivity;

	private Button lastRoad1Button;
	private Button lastRoad2Button;
	private Button lastRoad3Button;
	private Button lastRoad4Button;

	private TableRow roadTableRow;
	private Button roadAButton;
	private Button roadSButton;
	private Button roadDKButton;
	private Button roadDWButton;

	private int lastClick = 1;
	private int lastColor = Color.RED;
	private boolean isUnlockPending;
	private boolean isViewChanging;
	private Integer lastButtonId = null;
	private InputMethodManager keyboard;
	private List<String> lastRoadList = new ArrayList<String>();

	public ChainageView(final MainActivity mainActivity) {
		this.mainActivity = mainActivity;

		keyboard = (InputMethodManager) mainActivity
				.getSystemService(Context.INPUT_METHOD_SERVICE);

		lastButtonId = R.id.plus_button;

		initializeControls();

		distanceEdit.setText("0");
	}

	public String getRef() {
		return refEdit.getText().toString();
	}

	public void setRef(String ref) {
		refEdit.setText(ref);
	}

	public String getDistance() {
		return distanceEdit.getText().toString();
	}

	public void setDistance(String distance) {
		distanceEdit.setText(distance);
	}

	public void setViewChanging(boolean viewChanging) {
		isViewChanging = viewChanging;
	}

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.minus_button
				|| view.getId() == R.id.plus_button) {
			mainActivity.changeViewColor(lastButtonId, null);
			lastButtonId = view.getId();
			mainActivity.changeViewColor(lastButtonId, Color.GREEN);
		}

		String road = null;

		switch (view.getId()) {
		case R.id.minus_button:
			lastClick = -1;
			if (distanceEdit.getText().length() == 0) {
				distanceEdit.setText("0");
			} else {
				int distance = Integer.parseInt(distanceEdit.getText()
						.toString());
				distanceEdit.setText(distance > 0 ? String
						.valueOf(distance - 1) : "0");
			}

			hideRoadTableRow();
			break;
		case R.id.plus_button:
			lastClick = 1;
			if (distanceEdit.getText().length() == 0) {
				distanceEdit.setText("1");
			} else {
				int distance = Integer.parseInt(distanceEdit.getText()
						.toString());
				distanceEdit.setText(String.valueOf(distance + 1));
			}

			hideRoadTableRow();
			break;
		case R.id.add_button:
			addChainage();
			break;
		case R.id.last_road_1_button:
		case R.id.last_road_2_button:
		case R.id.last_road_3_button:
		case R.id.last_road_4_button:
			road = ((Button) view).getText().toString();
			lastColor = (Integer) ((Button) view).getTag();
			switch (lastColor) {
			case Color.BLUE:
				refEdit.setBackgroundResource(R.drawable.text_bg_blue);
				break;
			case Color.YELLOW:
				refEdit.setBackgroundResource(R.drawable.text_bg_yellow);
				break;
			case Color.GRAY:
				refEdit.setBackgroundResource(R.drawable.text_bg_gray);
				break;
			default:
				refEdit.setBackgroundResource(R.drawable.text_bg_red);
				break;
			}

			refEdit.setText(road, BufferType.EDITABLE);
			refEdit.setSelection(road.length(), road.length());
			hideRoadTableRow();
			break;
		case R.id.road_a_button:
			road = ((Button) view).getText().toString();
			lastColor = Color.BLUE;
			refEdit.setBackgroundResource(R.drawable.text_bg_blue);
			refEdit.setText(road, BufferType.EDITABLE);
			refEdit.setSelection(road.length(), road.length());
			refEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
			keyboard.showSoftInput(refEdit, InputMethodManager.SHOW_FORCED);
			break;
		case R.id.road_s_button:
			road = ((Button) view).getText().toString();
			lastColor = Color.RED;
			refEdit.setBackgroundResource(R.drawable.text_bg_red);
			refEdit.setText(road, BufferType.EDITABLE);
			refEdit.setSelection(road.length(), road.length());
			refEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
			keyboard.showSoftInput(refEdit, InputMethodManager.SHOW_FORCED);
			break;
		case R.id.road_dk_button:
			lastColor = Color.RED;
			refEdit.setBackgroundResource(R.drawable.text_bg_red);
			refEdit.setText("", BufferType.EDITABLE);
			refEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
			keyboard.showSoftInput(refEdit, InputMethodManager.SHOW_FORCED);
			break;
		case R.id.road_dw_button:
			lastColor = Color.YELLOW;
			refEdit.setBackgroundResource(R.drawable.text_bg_yellow);
			refEdit.setText("", BufferType.EDITABLE);
			refEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
			keyboard.showSoftInput(refEdit, InputMethodManager.SHOW_FORCED);
			break;
		case R.id.road_other_button:
			lastColor = Color.GRAY;
			refEdit.setBackgroundResource(R.drawable.text_bg_gray);
			refEdit.setText("", BufferType.EDITABLE);
			refEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
			keyboard.showSoftInput(refEdit, InputMethodManager.SHOW_FORCED);
			break;
		case R.id.ref_edit:
			if (roadTableRow.getVisibility() == View.GONE) {
				roadTableRow.setVisibility(View.VISIBLE);
				imageView.getViewTreeObserver().addOnGlobalLayoutListener(this);
			}

			break;
		}

		if (view.getId() != R.id.add_button) {
			setEnableButtons();
		}
	}

	@Override
	public void afterTextChanged(Editable s) {
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		setEnableButtons();
	}

	@Override
	public void onFocusChange(View view, boolean hasFocus) {
		if (hasFocus && !isViewChanging) {
			if (view.getId() == R.id.ref_edit) {
				if (roadTableRow.getVisibility() == View.GONE) {
					roadTableRow.setVisibility(View.VISIBLE);
					imageView.getViewTreeObserver().addOnGlobalLayoutListener(
							this);
				}
			} else {
				hideRoadTableRow();
			}
		}
	}

	@Override
	public void onGlobalLayout() {
		imageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
		locateWidgets();
	}

	public void initializeControls() {
		lastRoad1Button = (Button) mainActivity
				.findViewById(R.id.last_road_1_button);
		lastRoad2Button = (Button) mainActivity
				.findViewById(R.id.last_road_2_button);
		lastRoad3Button = (Button) mainActivity
				.findViewById(R.id.last_road_3_button);
		lastRoad4Button = (Button) mainActivity
				.findViewById(R.id.last_road_4_button);

		roadTableRow = (TableRow) mainActivity
				.findViewById(R.id.table_row_road);
		roadAButton = (Button) mainActivity.findViewById(R.id.road_a_button);
		roadSButton = (Button) mainActivity.findViewById(R.id.road_s_button);
		roadDKButton = (Button) mainActivity.findViewById(R.id.road_dk_button);
		roadDWButton = (Button) mainActivity.findViewById(R.id.road_dw_button);

		refEdit = (EditText) mainActivity.findViewById(R.id.ref_edit);
		distanceEdit = (EditText) mainActivity.findViewById(R.id.distance_edit);
		addButton = (Button) mainActivity.findViewById(R.id.add_button);
		imageView = (ImageView) mainActivity
				.findViewById(R.id.image_view_chainage);

		lastRoad1Button.setOnClickListener(this);
		lastRoad2Button.setOnClickListener(this);
		lastRoad3Button.setOnClickListener(this);
		lastRoad4Button.setOnClickListener(this);

		roadAButton.setOnClickListener(this);
		roadSButton.setOnClickListener(this);
		roadDKButton.setOnClickListener(this);
		roadDWButton.setOnClickListener(this);

		roadAButton.setTextColor(Color.WHITE);
		roadSButton.setTextColor(Color.WHITE);

		mainActivity.changeViewColor(roadAButton, Color.BLUE);
		mainActivity.changeViewColor(roadSButton, Color.RED);
		mainActivity.changeViewColor(roadDKButton, Color.RED);
		mainActivity.changeViewColor(roadDWButton, Color.YELLOW);

		refEdit.addTextChangedListener(this);
		refEdit.setOnFocusChangeListener(this);
		refEdit.setOnClickListener(this);
		refEdit.setInputType(InputType.TYPE_NULL);
		refEdit.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				boolean handled = false;
				if (actionId == EditorInfo.IME_ACTION_NEXT) {
					distanceEdit.setSelection(0, distanceEdit.getText()
							.length());
					distanceEdit.requestFocus();
					handled = true;
				}

				return handled;
			}
		});

		distanceEdit.addTextChangedListener(this);
		distanceEdit.setOnFocusChangeListener(this);
		addButton.setOnClickListener(this);

		mainActivity.findViewById(R.id.plus_button).setOnClickListener(this);
		mainActivity.findViewById(R.id.minus_button).setOnClickListener(this);
		mainActivity.findViewById(R.id.road_other_button).setOnClickListener(
				this);

		setEnableButtons();
		loadLastRoads();

		mainActivity.changeViewColor(lastButtonId, Color.GREEN);

		DisplayMetrics metrics = new DisplayMetrics();
		mainActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

		if (isTablet(metrics)) {
			if (getScreenOrientation(metrics) == Configuration.ORIENTATION_PORTRAIT) {
				imageView.setMinimumWidth((int) (100 * metrics.density));

				refEdit.setTextSize(refEdit.getTextSize() * 1.5F);
				distanceEdit.setTextSize(distanceEdit.getTextSize() * 1.5F);
			} else {
				imageView.setImageResource(R.drawable.chainage_bg);
			}
		}

		imageView.getViewTreeObserver().addOnGlobalLayoutListener(this);
	}

	public void setEnableButtons() {
		if (!isUnlockPending) {
			addButton.setEnabled(MainActivity.getLocation() != null
					&& distanceEdit.getText().length() > 0
					&& refEdit.getText().length() > 0);
		}
	}

	public void checkRoadTableRowVisibility() {
		if (!"".equals(refEdit.getText().toString())) {
			switch (lastColor) {
			case Color.BLUE:
				refEdit.setBackgroundResource(R.drawable.text_bg_blue);
				break;
			case Color.YELLOW:
				refEdit.setBackgroundResource(R.drawable.text_bg_yellow);
				break;
			case Color.GRAY:
				refEdit.setBackgroundResource(R.drawable.text_bg_gray);
				break;
			default:
				refEdit.setBackgroundResource(R.drawable.text_bg_red);
				break;
			}

			hideRoadTableRow();
		}
	}

	private void hideRoadTableRow() {
		if (roadTableRow.getVisibility() == View.VISIBLE) {
			roadTableRow.setVisibility(View.GONE);
			imageView.getViewTreeObserver().addOnGlobalLayoutListener(this);
			refEdit.setInputType(InputType.TYPE_NULL);
		}
	}

	private void loadLastRoads() {
		String lastRoadsSplit = mainActivity.getSharedPreferences(
				MainActivity.SETTINGS_FILE, Context.MODE_PRIVATE).getString(
				MainActivity.LAST_ROADS_SETTING, "");

		if (lastRoadsSplit != null && !"".equals(lastRoadsSplit)) {
			lastRoadList = new ArrayList<String>(Arrays.asList(lastRoadsSplit
					.split(";")));
		}

		for (int i = 0; i < 4; i++) {
			Button lastRoadButton = null;

			switch (i) {
			case 0:
				lastRoadButton = lastRoad1Button;
				break;
			case 1:
				lastRoadButton = lastRoad2Button;
				break;
			case 2:
				lastRoadButton = lastRoad3Button;
				break;
			case 3:
				lastRoadButton = lastRoad4Button;
				break;
			}

			if (lastRoadList.size() > i) {
				String[] lastRoad = lastRoadList.get(i).split(":");

				lastRoadButton.setText(lastRoad[0]);
				lastRoadButton.setClickable(true);
				lastRoadButton.setBackgroundDrawable(addButton.getBackground()
						.getConstantState().newDrawable());

				if (lastRoad.length > 1) {
					int color = Integer.parseInt(lastRoad[1]);
					mainActivity.changeViewColor(lastRoadButton, color);
					lastRoadButton.setTag(color);
				} else {
					mainActivity.changeViewColor(lastRoadButton, Color.RED);
					lastRoadButton.setTag(Color.RED);
				}
			} else {
				lastRoadButton.setText("");
				lastRoadButton.setClickable(false);
				lastRoadButton.setBackgroundColor(Color.TRANSPARENT);
			}
		}
	}

	private void saveLastRoads() {
		String lastRoad = refEdit.getText().toString();
		if (lastRoad != null && !"".equals(lastRoad)) {
			lastRoad += ":" + lastColor;
			int lastRoadIndex = lastRoadList.indexOf(lastRoad);
			if (lastRoadIndex >= 0) {
				lastRoadList.remove(lastRoadIndex);
				lastRoadList.add(0, lastRoad);
			} else {
				if (lastRoadList.size() > 0) {
					lastRoadList.add(0, lastRoad);
				} else {
					lastRoadList.add(lastRoad);
				}

				if (lastRoadList.size() > 4) {
					lastRoadList.remove(4);
				}
			}

			Editor settingsEdit = mainActivity.getSharedPreferences(
					MainActivity.SETTINGS_FILE, Context.MODE_PRIVATE).edit();
			settingsEdit.putString(MainActivity.LAST_ROADS_SETTING,
					joinString(lastRoadList, ";"));
			settingsEdit.commit();
		}
	}

	private void locateWidgets() {
		DisplayMetrics metrics = new DisplayMetrics();
		mainActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

		boolean isTablet = isTablet(metrics);
		boolean isPortrait = getScreenOrientation(metrics) == Configuration.ORIENTATION_PORTRAIT;

		LayoutParams refEditLayout = new LayoutParams(refEdit.getLayoutParams());

		if (isPortrait || isTablet) {
			refEditLayout.setMargins(isPortrait && isTablet ? 15 : 2,
					(int) (imageView.getMeasuredHeight() / 18)
							+ (isPortrait && isTablet ? 20 : 0), 0, 0);
		} else {
			refEditLayout.setMargins(2,
					(int) (imageView.getMeasuredHeight() / 17), 0, 0);
		}

		refEdit.setLayoutParams(refEditLayout);

		LayoutParams distanceEditLayout = new LayoutParams(
				distanceEdit.getLayoutParams());
		if (isPortrait || isTablet) {
			distanceEditLayout.setMargins(isPortrait && isTablet ? 15 : 2,
					(int) (imageView.getMeasuredHeight() / 2.15)
							+ (isPortrait && isTablet ? 15 : 0), 0, 0);
		} else {
			distanceEditLayout.setMargins(2,
					(int) (imageView.getMeasuredHeight() / 1.62), 0, 0);
		}

		distanceEdit.setLayoutParams(distanceEditLayout);
	}

	private int getScreenOrientation(DisplayMetrics metrics) {
		if (metrics.widthPixels < metrics.heightPixels) {
			return Configuration.ORIENTATION_PORTRAIT;
		} else {
			return Configuration.ORIENTATION_LANDSCAPE;
		}
	}

	private String joinString(Collection<?> s, String delimiter) {
		StringBuilder builder = new StringBuilder();
		Iterator<?> iter = s.iterator();
		while (iter.hasNext()) {
			builder.append(iter.next());

			if (!iter.hasNext()) {
				break;
			}

			builder.append(delimiter);
		}

		return builder.toString();
	}

	private boolean isTablet(DisplayMetrics metrics) {
		float dpHeight = metrics.heightPixels / metrics.density;
		float dpWidth = metrics.widthPixels / metrics.density;

		return Math.min(dpHeight, dpWidth) >= 550.0F;
	}

	private void addChainage() {
		if (MainActivity.getLocation() == null) {
			Toast.makeText(mainActivity, R.string.waitForFixGPS,
					Toast.LENGTH_LONG).show();
			return;
		}

		HashMap<String, String> tags = new HashMap<String, String>();
		tags.put("highway", "milestone");
		tags.put("distance", distanceEdit.getText().toString());
		tags.put("ref", refEdit.getText().toString());
		tags.put("source", "GPS(APK:StrazakOSM)");

		try {
			if (((StApplication) mainActivity.getApplication()).osmWriter == null) {
				((StApplication) mainActivity.getApplication()).newOSMFile();
			}

			((StApplication) mainActivity.getApplication()).osmWriter.addNode(
					MainActivity.getLocation().getLatitude(), MainActivity
							.getLocation().getLongitude(), tags);
		} catch (Exception e) {
			mainActivity.showDialogFatalError(R.string.errorFileOpen);
		}

		Toast.makeText(mainActivity,
				"Odleg³oœæ = " + distanceEdit.getText().toString(),
				Toast.LENGTH_SHORT).show();

		int distance = Integer.parseInt(distanceEdit.getText().toString());
		distanceEdit.setText(distance + lastClick >= 0 ? String
				.valueOf(distance + lastClick) : "0");

		addButton.setEnabled(false);

		saveLastRoads();
		loadLastRoads();
		hideRoadTableRow();

		isUnlockPending = true;
		mainActivity.getHandler().postDelayed(unlockButton, 3000);
	}

	private Runnable unlockButton = new Runnable() {
		@Override
		public void run() {
			isUnlockPending = false;
			setEnableButtons();
		}
	};
}
