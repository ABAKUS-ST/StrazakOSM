package org.osmdroid.tileprovider;

import org.osmdroid.tileprovider.modules.INetworkAvailablityCheck;
import org.osmdroid.tileprovider.modules.MapTileFileArchiveProvider;
import org.osmdroid.tileprovider.modules.MapTileFilesystemProvider;
import org.osmdroid.tileprovider.modules.NetworkAvailabliltyCheck;
import org.osmdroid.tileprovider.modules.TileWriter;
import org.osmdroid.tileprovider.modules.WMSMapTileDownloader;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;

import android.content.Context;

public class WMSMapTileProviderBasic extends MapTileProviderArray implements
		IMapTileProviderCallback {

	/**
	 * Creates a {@link MapTileProviderBasic}.
	 */
	public WMSMapTileProviderBasic(final Context pContext) {
		this(pContext, TileSourceFactory.DEFAULT_TILE_SOURCE);
	}

	/**
	 * Creates a {@link MapTileProviderBasic}.
	 */
	public WMSMapTileProviderBasic(final Context pContext,
			final ITileSource pTileSource) {
		this(new SimpleRegisterReceiver(pContext),
				new NetworkAvailabliltyCheck(pContext), pTileSource);
	}

	/**
	 * Creates a {@link MapTileProviderBasic}.
	 */
	public WMSMapTileProviderBasic(final IRegisterReceiver pRegisterReceiver,
			final INetworkAvailablityCheck aNetworkAvailablityCheck,
			final ITileSource pTileSource) {
		super(pTileSource, pRegisterReceiver);

		final TileWriter tileWriter = new TileWriter();

		final MapTileFilesystemProvider fileSystemProvider = new MapTileFilesystemProvider(
				pRegisterReceiver, pTileSource);
		mTileProviderList.add(fileSystemProvider);

		final MapTileFileArchiveProvider archiveProvider = new MapTileFileArchiveProvider(
				pRegisterReceiver, pTileSource);
		mTileProviderList.add(archiveProvider);

		final WMSMapTileDownloader downloaderProvider = new WMSMapTileDownloader(
				pTileSource, tileWriter, aNetworkAvailablityCheck);
		mTileProviderList.add(downloaderProvider);
	}

} // end WMSMapTileProviderBasic
