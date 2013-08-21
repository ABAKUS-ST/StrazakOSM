package pl.openstreetmap.dotevo.strazak;

import java.util.HashMap;

import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ChainageView implements OnClickListener {

	private EditText refEdit;
	private EditText distanceEdit;
	private Button addButton;
	private MainActivity mainActivity;

	private int lastClick = 0;

	public ChainageView(MainActivity mainActivity) {
		this.mainActivity = mainActivity;

		distanceEdit = (EditText) mainActivity.findViewById(R.id.distance_edit);
		refEdit = (EditText) mainActivity.findViewById(R.id.ref_edit);
		addButton = (Button) mainActivity.findViewById(R.id.add_button);

		addButton.setOnClickListener(this);

		mainActivity.findViewById(R.id.plus_button).setOnClickListener(this);
		mainActivity.findViewById(R.id.minus_button).setOnClickListener(this);

		setEnableButtons();
	}

	@Override
	public void onClick(View arg0) {
		switch (arg0.getId()) {
		case R.id.minus_button:
			lastClick = -1;
			distanceEdit
					.setText(""
							+ (Integer.parseInt(distanceEdit.getText()
									.toString()) - 1));
			break;
		case R.id.plus_button:
			lastClick = 1;
			distanceEdit
					.setText(""
							+ (Integer.parseInt(distanceEdit.getText()
									.toString()) + 1));
			break;
		case R.id.add_button:
			addChainage();
			break;
		}

		setEnableButtons();
	}

	public void setEnableButtons() {
		addButton.setEnabled(mainActivity.getLocation() != null);
	}

	private void addChainage() {
		if (mainActivity.getLocation() == null) {
			Toast.makeText(mainActivity, "Poczekaj na ustalenie pozycji GPS",
					Toast.LENGTH_LONG).show();
			return;
		}

		HashMap<String, String> tags = new HashMap<String, String>();
		tags.put("highway", "milestone");
		tags.put("distance", distanceEdit.getText().toString());
		tags.put("ref", refEdit.getText().toString());
		tags.put("source", "GSM (APK:StrazakOSM)");

		try {
			if (((StApplication) mainActivity.getApplication()).osmWriter == null) {
				((StApplication) mainActivity.getApplication()).newOSMFile();
			}

			((StApplication) mainActivity.getApplication()).osmWriter.addNode(
					mainActivity.getLocation().getLatitude(), mainActivity
							.getLocation().getLongitude(), tags);
		} catch (Exception e) {
			mainActivity.showDialogFatalError(R.string.errorFileOpen);
		}

		Toast.makeText(mainActivity,
				"Odleg³oœæ = " + distanceEdit.getText().toString(),
				Toast.LENGTH_LONG).show();

		distanceEdit
				.setText(""
						+ (Integer.parseInt(distanceEdit.getText().toString()) + lastClick));

		addButton.setEnabled(false);

		new Handler().postDelayed(unlockButton, 1000);
	}

	private Runnable unlockButton = new Runnable() {
		@Override
		public void run() {
			setEnableButtons();
		}
	};
}
