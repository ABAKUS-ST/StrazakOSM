package org.osmdroid.tileprovider.tilesource;

import java.util.Locale;

import org.osmdroid.ResourceProxy.string;
import org.osmdroid.tileprovider.MapTile;

import android.util.Log;

public class WMSTileSource extends OnlineTileSourceBase {

	private String serviceName;
	private String layerName;
	private String projectionKind;
	private Boolean transparent;

	public WMSTileSource(final String aName, final string aResourceId,
			final int aZoomMinLevel, final int aZoomMaxLevel,
			final int aTileSizePixels, final String aImageFilenameEnding,
			final String aServiceName, final String aLayerName,
			final String aProjectionKind, Boolean aTransparent,
			final String... aBaseUrl) {
		super(aName, aResourceId, aZoomMinLevel, aZoomMaxLevel,
				aTileSizePixels, aImageFilenameEnding, aBaseUrl);

		this.serviceName = aServiceName;
		this.layerName = aLayerName;
		this.projectionKind = aProjectionKind;
		this.transparent = aTransparent;
	}

	@Override
	public String getTileURLString(MapTile aTile) {

		StringBuffer tileURLString = new StringBuffer();
		tileURLString.append(getBaseUrl());
		tileURLString.append(wmsTileCoordinates(aTile));

		return tileURLString.toString();
	}

	private final static double ORIGIN_SHIFT = Math.PI * 6378137;

	/**
	 * WMS requires the bounding box to be defined as the point (west, south) to
	 * the point (east, north).
	 * 
	 * @return The WMS string defining the bounding box values.
	 */
	public String wmsTileCoordinates(MapTile value) {

		BoundingBox newTile = tile2boundingBox(value.getX(), value.getY(),
				value.getZoomLevel());

		StringBuffer stringBuffer = new StringBuffer();

		stringBuffer.append("?service=");
		stringBuffer.append(this.serviceName);
		stringBuffer.append("&request=GetMap&version=1.1.1&layers=");
		stringBuffer.append(this.layerName);
		stringBuffer.append("&styles=&format=image/");
		stringBuffer.append(this.imageFilenameEnding().replace(".", ""));
		stringBuffer.append("&transparent=");
		stringBuffer.append(this.transparent ? "true" : "false");
		stringBuffer.append("&height=");
		stringBuffer.append(this.getTileSizePixels());
		stringBuffer.append("&width=");
		stringBuffer.append(this.getTileSizePixels());
		stringBuffer.append("&srs=");
		stringBuffer.append(this.projectionKind);
		stringBuffer.append("&crs=");
		stringBuffer.append(this.projectionKind);
		stringBuffer.append("&tiled=true&bbox=");
		stringBuffer.append(String.format(Locale.US, "%f", newTile.west));
		stringBuffer.append(",");
		stringBuffer.append(String.format(Locale.US, "%f", newTile.south));
		stringBuffer.append(",");
		stringBuffer.append(String.format(Locale.US, "%f", newTile.east));
		stringBuffer.append(",");
		stringBuffer.append(String.format(Locale.US, "%f", newTile.north));

		return stringBuffer.toString();
	}

	/**
	 * A simple class for holding the NSEW lat and lon values.
	 */
	private class BoundingBox {
		double north;
		double south;
		double east;
		double west;
	}

	/**
	 * This method converts tile xyz values to a WMS bounding box.
	 * 
	 * @param x
	 *            The x tile coordinate.
	 * @param y
	 *            The y tile coordinate.
	 * @param zoom
	 *            The zoom level.
	 * 
	 * @return The completed bounding box.
	 */
	BoundingBox tile2boundingBox(final int x, final int y, final int zoom) {

		Log.d("MapTile", "--------------- x = " + x);
		Log.d("MapTile", "--------------- y = " + y);
		Log.d("MapTile", "--------------- zoom = " + zoom);

		BoundingBox bb = new BoundingBox();

		bb.north = yToWgs84toEPSGLat(y, zoom);
		bb.south = yToWgs84toEPSGLat(y + 1, zoom);
		bb.west = xToWgs84toEPSGLon(x, zoom);
		bb.east = xToWgs84toEPSGLon(x + 1, zoom);

		return bb;
	}

	/**
	 * Converts X tile number to EPSG value.
	 * 
	 * @param tileX
	 *            the x tile being requested.
	 * @param zoom
	 *            The current zoom level.
	 * 
	 * @return EPSG longitude value.
	 */
	static double xToWgs84toEPSGLon(int tileX, int zoom) {

		// convert x tile position and zoom to wgs84 longitude
		double value = tileX / Math.pow(2.0, zoom) * 360.0 - 180;

		// apply the shift to get the EPSG longitude
		return value * ORIGIN_SHIFT / 180.0;

	}

	/**
	 * Converts Y tile number to EPSG value.
	 * 
	 * @param tileY
	 *            the y tile being requested.
	 * @param zoom
	 *            The current zoom level.
	 * 
	 * @return EPSG latitude value.
	 */
	static double yToWgs84toEPSGLat(int tileY, int zoom) {

		// convert x tile position and zoom to wgs84 latitude
		double value = Math.PI - (2.0 * Math.PI * tileY) / Math.pow(2.0, zoom);
		value = Math.toDegrees(Math.atan(Math.sinh(value)));

		value = Math.log(Math.tan((90 + value) * Math.PI / 360.0))
				/ (Math.PI / 180.0);

		// apply the shift to get the EPSG latitude
		return value * ORIGIN_SHIFT / 180.0;

	}

}
