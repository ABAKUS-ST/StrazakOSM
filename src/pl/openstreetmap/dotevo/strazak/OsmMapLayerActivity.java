package pl.openstreetmap.dotevo.strazak;

import java.util.ArrayList;
import java.util.List;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.ResourceProxy.bitmap;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListView;

public class OsmMapLayerActivity extends Activity {

	private SharedPreferences prefs;
	private ResourceProxy resourceProxy;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layer);

		this.resourceProxy = new DefaultResourceProxyImpl(
				getApplicationContext());
		this.prefs = getSharedPreferences(MainActivity.SETTINGS_FILE,
				Context.MODE_PRIVATE);

		final LayersAdapter adapter = new LayersAdapter(this,
				this.createLayerList());

		ListView list = (ListView) findViewById(R.id.items);
		list.setAdapter(adapter);
		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View view,
					int position, long index) {
				LayerEncapsulator entry = (LayerEncapsulator) adapter
						.getItem(position);
				SharedPreferences.Editor edit = prefs.edit();
				edit.putBoolean(entry.getPreference(),
						!prefs.getBoolean(entry.getPreference(), false));
				edit.commit();

				finish();
			}
		});
	}

	private List<LayerEncapsulator> createLayerList() {
		List<LayerEncapsulator> list = new ArrayList<LayerEncapsulator>();

		list.add(new LayerEncapsulator(getResources().getDrawable(
				R.drawable.ic_menu_hydrant), "Pokazuj hydranty",
				MainActivity.MAP_SHOW_HYDRANTS_SETTING));
		list.add(new LayerEncapsulator(getResources().getDrawable(
				R.drawable.ic_menu_tree), "Pokazuj oddzia³y leœne",
				MainActivity.MAP_SHOW_FORESTS_SETTING));
		list.add(new LayerEncapsulator(this.resourceProxy
				.getDrawable(bitmap.ic_menu_compass), "Pokazuj kompas",
				MainActivity.MAP_SHOW_COMPASS_SETTING));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			list.add(new LayerEncapsulator(this.resourceProxy
					.getDrawable(bitmap.ic_menu_mapmode), "Pokazuj skalê",
					MainActivity.MAP_SHOW_SCALE_SETTING));
		}

		return list;
	}

	private class LayerEncapsulator {

		private Drawable icon;
		private String name;
		private String preference;

		public LayerEncapsulator(Drawable icon, String name, String preference) {
			this.icon = icon;
			this.name = name;
			this.preference = preference;
		}

		public Drawable getIcon() {
			return icon;
		}

		public String getName() {
			return name;
		}

		public String getPreference() {
			return preference;
		}
	}

	private class LayersAdapter extends BaseAdapter implements OnClickListener {

		private Context mContext;
		private List<LayerEncapsulator> mLayerList;

		public LayersAdapter(Context context, List<LayerEncapsulator> layerList) {
			mContext = context;
			mLayerList = layerList;
		}

		@Override
		public int getCount() {
			if (mLayerList == null) {
				return 0;
			} else {
				return mLayerList.size();
			}
		}

		@Override
		public Object getItem(int position) {
			if (mLayerList == null) {
				return null;
			} else {
				return mLayerList.get(position);
			}
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup viewGroup) {
			LayerEncapsulator entry = (LayerEncapsulator) getItem(position);

			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) mContext
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.layer_item, null);
			}

			CheckedTextView cbLayer = (CheckedTextView) convertView
					.findViewById(R.id.layer_check);
			cbLayer.setText(entry.getName());
			cbLayer.setChecked(prefs.getBoolean(entry.getPreference(), false));

			ImageView ivManeuver = (ImageView) convertView
					.findViewById(R.id.layer_icon);
			ivManeuver.setImageDrawable(entry.getIcon());
			return convertView;
		}

		@Override
		public void onClick(View arg0) {
		}
	}
}
