package pl.openstreetmap.dotevo.strazak;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import android.app.Application;
import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

public class StApplication extends Application {
	public OsmWriter osmWriter = null;
	public String extStorage;
	private PowerManager.WakeLock wl;

	@Override
	public void onCreate() {
		super.onCreate();
		extStorage = System.getenv().get("EXTERNAL_STORAGE");
		newOSMFile();
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "DoNotDimScreen");
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		wl.acquire();

	}

	public void closeFile() {
		try {
			osmWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		osmWriter = null;
	}

	public int newOSMFile() {
		if (osmWriter != null) {
			closeFile();
		}

		File kpmFolder = new File(extStorage + "/pikietaz");
		if (!kpmFolder.exists()) {
			if (!kpmFolder.mkdir()) {
				return -1;
			}
		}
		File kpmFolder2 = new File(extStorage + "/pikietaz/sended");
		if (!kpmFolder2.exists()) {
			if (!kpmFolder2.mkdir()) {
				return -2;
			}
		}

		Calendar cal = Calendar.getInstance();
		String basename = String.format("%tF_%tH-%tM-%tS", cal, cal, cal, cal);

		try {
			String file = kpmFolder + "/" + basename + ".osm";
			Log.i("New file", file);
			osmWriter = new OsmWriter(file, false);
		} catch (IOException e) {
			return -3;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	public void flushFile() {
		try {
			if (osmWriter != null)
				osmWriter.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
