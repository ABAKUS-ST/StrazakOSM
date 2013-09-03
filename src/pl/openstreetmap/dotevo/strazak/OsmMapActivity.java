package pl.openstreetmap.dotevo.strazak;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.ResourceProxy.bitmap;
import org.osmdroid.tileprovider.WMSMapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.WMSTileSource;
import org.osmdroid.tileprovider.util.SimpleInvalidationHandler;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MyLocationOverlay;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.TilesOverlay;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;

public class OsmMapActivity extends Activity {

	private Button buttonFollow;
	private MapView mapView;

	private SharedPreferences prefs;
	private MyLocationOverlay locationOverlay;
	private ScaleBarOverlay scaleBarOverlay;
	private TilesOverlay hydrantsOverlay;
	private TilesOverlay forestsOverlay;
	private ResourceProxy resourceProxy;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.osm_map);

		this.buttonFollow = (Button) findViewById(R.id.buttonFollow);

		this.prefs = getSharedPreferences(MainActivity.SETTINGS_FILE,
				Context.MODE_PRIVATE);

		this.setButtonHandlers();

		this.resourceProxy = new DefaultResourceProxyImpl(
				getApplicationContext());

		// Check for internet
		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		if (cm.getActiveNetworkInfo() == null
				|| !cm.getActiveNetworkInfo().isConnectedOrConnecting()) {
			showDialogNoInternet();
		}

		LinearLayout mapLayout = (LinearLayout) findViewById(R.id.map_layout);

		WMSMapTileProviderBasic tileProvider = new WMSMapTileProviderBasic(
				getApplicationContext());
		ITileSource tileSource = new WMSTileSource("WMS_ABAKUS", null, 5, 20,
				256, ".png8", "WMS", "osm:Mapa(8-bit)_active", "EPSG:3857",
				false,
				"http://mapa.abakus.net.pl:8080/geoserver/gwc/service/wms");

		tileProvider.setTileSource(tileSource);

		this.mapView = new MapView(this, 256, this.resourceProxy, tileProvider);
		this.mapView.setUseSafeCanvas(true);
		this.mapView.setLayoutParams(new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		this.setHardwareAccelerationOff();

		mapLayout.addView(this.mapView);

		this.mapView.setBuiltInZoomControls(true);
		this.mapView.setMultiTouchControls(true);

		WMSMapTileProviderBasic hydrantsTileProvider = new WMSMapTileProviderBasic(
				getApplicationContext());
		ITileSource hydrantsTileSource = new WMSTileSource(
				"WMS_ABAKUS_HYDRANTY", null, 5, 20, 256, ".png", "WMS",
				"Hydranty_active", "EPSG:3857", true,
				"http://mapa.abakus.net.pl:8080/geoserver/wms");

		hydrantsTileProvider.setTileSource(hydrantsTileSource);
		hydrantsTileProvider
				.setTileRequestCompleteHandler(new SimpleInvalidationHandler(
						this.mapView));

		this.hydrantsOverlay = new TilesOverlay(hydrantsTileProvider,
				this.resourceProxy);
		this.hydrantsOverlay.setLoadingBackgroundColor(Color.TRANSPARENT);
		this.mapView.getOverlays().add(this.hydrantsOverlay);

		WMSMapTileProviderBasic forestsTileProvider = new WMSMapTileProviderBasic(
				getApplicationContext());
		ITileSource forestsTileSource = new WMSTileSource("WMS_ABAKUS_LASY",
				null, 5, 20, 256, ".png", "WMS", "Lasy_active", "EPSG:3857",
				true, "http://mapa.abakus.net.pl:8080/geoserver/wms");

		forestsTileProvider.setTileSource(forestsTileSource);
		forestsTileProvider
				.setTileRequestCompleteHandler(new SimpleInvalidationHandler(
						this.mapView));

		this.forestsOverlay = new TilesOverlay(forestsTileProvider,
				this.resourceProxy);
		this.forestsOverlay.setLoadingBackgroundColor(Color.TRANSPARENT);
		this.mapView.getOverlays().add(this.forestsOverlay);

		this.scaleBarOverlay = new ScaleBarOverlay(this, this.resourceProxy);
		this.scaleBarOverlay.setCentred(true);
		this.scaleBarOverlay.setScaleBarOffset(getResources()
				.getDisplayMetrics().widthPixels / 2, 10);
		this.mapView.getOverlays().add(this.scaleBarOverlay);

		this.locationOverlay = new MyLocationOverlay(this.getBaseContext(),
				this.mapView, this.resourceProxy);
		this.mapView.getOverlays().add(this.locationOverlay);

		this.mapView.getController().setZoom(16);
		if (MainActivity.getLocation() != null) {
			this.mapView.getController().setCenter(
					new GeoPoint(MainActivity.getLocation()));
		} else {
			this.mapView.getController().setCenter(
					new GeoPoint(52.06916666, 19.48055555));
		}

		this.mapView.invalidate();
		this.mapView.postInvalidate();
	}

	@Override
	protected void onPause() {
		SharedPreferences.Editor edit = this.prefs.edit();
		edit.putBoolean(MainActivity.MAP_SHOW_COMPASS_SETTING,
				this.locationOverlay.isCompassEnabled());
		edit.putBoolean(MainActivity.MAP_SHOW_SCALE_SETTING,
				this.scaleBarOverlay.isEnabled());
		edit.putBoolean(MainActivity.MAP_SHOW_HYDRANTS_SETTING,
				this.hydrantsOverlay.isEnabled());
		edit.putBoolean(MainActivity.MAP_SHOW_FORESTS_SETTING,
				this.forestsOverlay.isEnabled());
		edit.commit();

		this.locationOverlay.disableFollowLocation();
		this.locationOverlay.disableMyLocation();

		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();

		this.locationOverlay.setLocationUpdateMinDistance(50);

		this.locationOverlay.enableFollowLocation();
		this.locationOverlay.enableMyLocation();

		if (this.prefs.getBoolean(MainActivity.MAP_SHOW_COMPASS_SETTING, false)) {
			this.locationOverlay.enableCompass();
		} else {
			this.locationOverlay.disableCompass();
		}

		if (this.prefs.getBoolean(MainActivity.MAP_SHOW_SCALE_SETTING, false)) {
			this.scaleBarOverlay.enableScaleBar();
		} else {
			this.scaleBarOverlay.disableScaleBar();
		}

		if (this.prefs.getBoolean(MainActivity.MAP_SHOW_HYDRANTS_SETTING, true)) {
			this.hydrantsOverlay.setEnabled(true);
		} else {
			this.hydrantsOverlay.setEnabled(false);
		}

		if (this.prefs.getBoolean(MainActivity.MAP_SHOW_FORESTS_SETTING, false)) {
			this.forestsOverlay.setEnabled(true);
		} else {
			this.forestsOverlay.setEnabled(false);
		}
	}

	@Override
	public void onDestroy() {
		this.locationOverlay.disableMyLocation();
		this.locationOverlay.disableFollowLocation();

		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		menu.clear();

		menu.add(Menu.NONE, 1, 1, "Warstwy").setIcon(
				this.resourceProxy.getDrawable(bitmap.ic_menu_mapmode));

		return true;
	}

	@Override
	public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
		switch (item.getItemId()) {
		case 1:
			Intent intentLayers = new Intent(this, OsmMapLayerActivity.class);
			startActivity(intentLayers);
			break;
		}

		return true;
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		return this.mapView.onTrackballEvent(event);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_MOVE) {
			this.locationOverlay.disableFollowLocation();
		}

		return super.onTouchEvent(event);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setHardwareAccelerationOff() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			this.mapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		}
	}

	private void setButtonHandlers() {
		this.buttonFollow.setOnClickListener(new ButtonFollowOnClickListener());
	}

	private void showDialogNoInternet() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.errorNoInternet)
				.setMessage(R.string.queryNoInternet).setCancelable(false)
				.setIcon(R.drawable.transmit_error)
				.setPositiveButton(R.string.next, null);

		builder.show();
	}

	private class ButtonFollowOnClickListener implements OnClickListener {

		@Override
		public void onClick(View view) {
			Location currentLocation = MainActivity.getLocation();
			if (currentLocation != null) {
				mapView.getController()
						.setCenter(new GeoPoint(currentLocation));
			}

			locationOverlay.enableFollowLocation();
		}
	}
}