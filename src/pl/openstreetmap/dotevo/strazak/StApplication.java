package pl.openstreetmap.dotevo.strazak;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import android.app.Application;
import android.util.Log;

public class StApplication extends Application {

	public OsmWriter osmWriter;
	public String extStorage;

	@Override
	public void onCreate() {
		super.onCreate();
		extStorage = System.getenv().get("EXTERNAL_STORAGE");
		newOSMFile();
	}

	public void flushFile() {
		try {
			if (osmWriter != null) {
				osmWriter.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void closeFile() {
		flushFile();

		try {
			if (osmWriter != null) {
				osmWriter.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		osmWriter = null;
	}

	public int newOSMFile() {
		closeFile();

		File kpmFolder = new File(extStorage + "/StrazakOSM");
		if (!kpmFolder.exists()) {
			if (!kpmFolder.mkdir()) {
				return -1;
			}
		}

		File kpmFolder2 = new File(extStorage + "/StrazakOSM/sended");
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
}