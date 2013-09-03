package org.osmdroid.tileprovider.modules;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.MapTileRequestState;
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase.LowMemoryException;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.util.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;

public class WMSMapTileDownloader extends MapTileModuleProviderBase {

	// ===========================================================
	// Constants
	// ===========================================================

	private static final Logger logger = LoggerFactory
			.getLogger(MapTileDownloader.class);

	// ===========================================================
	// Fields
	// ===========================================================

	private final IFilesystemCache mFilesystemCache;

	private OnlineTileSourceBase mTileSource;

	private final INetworkAvailablityCheck mNetworkAvailablityCheck;

	// ===========================================================
	// Constructors
	// ===========================================================

	public WMSMapTileDownloader(final ITileSource pTileSource) {
		this(pTileSource, null, null);
	}

	public WMSMapTileDownloader(final ITileSource pTileSource,
			final IFilesystemCache pFilesystemCache) {
		this(pTileSource, pFilesystemCache, null);
	}

	public WMSMapTileDownloader(final ITileSource pTileSource,
			final IFilesystemCache pFilesystemCache,
			final INetworkAvailablityCheck pNetworkAvailablityCheck) {
		super(NUMBER_OF_TILE_DOWNLOAD_THREADS, TILE_DOWNLOAD_MAXIMUM_QUEUE_SIZE);

		mFilesystemCache = pFilesystemCache;
		mNetworkAvailablityCheck = pNetworkAvailablityCheck;
		setTileSource(pTileSource);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public ITileSource getTileSource() {
		return mTileSource;
	}

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected String getName() {
		return "WMS Online Tile Download Provider";
	}

	@Override
	protected String getThreadGroupName() {
		return "downloader";
	}

	@Override
	protected Runnable getTileLoader() {
		return new WMSTileLoader();
	}

	@Override
	public boolean getUsesDataConnection() {
		return true;
	}

	@Override
	public int getMinimumZoomLevel() {
		return (mTileSource != null ? mTileSource.getMinimumZoomLevel()
				: MINIMUM_ZOOMLEVEL);
	}

	@Override
	public int getMaximumZoomLevel() {
		return (mTileSource != null ? mTileSource.getMaximumZoomLevel()
				: MAXIMUM_ZOOMLEVEL);
	}

	@Override
	public void setTileSource(ITileSource tileSource) {
		// We are only interested in OnlineTileSourceBase tile sources
		if (tileSource instanceof OnlineTileSourceBase) {
			mTileSource = (OnlineTileSourceBase) tileSource;
		} else {
			// Otherwise shut down the tile downloader
			mTileSource = null;
		}
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	private class WMSTileLoader extends MapTileModuleProviderBase.TileLoader {

		@Override
		public Drawable loadTile(final MapTileRequestState aState)
				throws CantContinueException {

			if (mTileSource == null) {
				return null;
			}

			InputStream in = null;
			OutputStream out = null;
			final MapTile tile = aState.getMapTile();

			try {

				if (mNetworkAvailablityCheck != null
						&& !mNetworkAvailablityCheck.getNetworkAvailable()) {
					if (DEBUGMODE) {
						logger.debug("WMSMapTileDownloader -- Skipping "
								+ getName()
								+ " due to NetworkAvailabliltyCheck.");
					}
					return null;
				}

				final String tileURLString = mTileSource.getTileURLString(tile);

				if (DEBUGMODE) {
					logger.debug("WMSMapTileDownloader -- Downloading Maptile from url: "
							+ tileURLString);
				}

				if (TextUtils.isEmpty(tileURLString)) {
					return null;
				}

				final HttpClient httpClient = new DefaultHttpClient();
				final HttpUriRequest head = new HttpGet(tileURLString);
				final HttpResponse response = httpClient.execute(head);

				// Check to see if we got success
				final org.apache.http.StatusLine line = response
						.getStatusLine();
				if (line.getStatusCode() != 200) {
					logger.warn("WMSMapTileDownloader -- Problem downloading MapTile: "
							+ tile + " HTTP response: " + line);
					return null;
				}

				final HttpEntity entity = response.getEntity();
				if (entity == null) {
					logger.warn("WMSMapTileDownloader -- No content downloading MapTile: "
							+ tile);
					return null;
				}
				in = entity.getContent();

				final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
				out = new BufferedOutputStream(dataStream,
						StreamUtils.IO_BUFFER_SIZE);
				StreamUtils.copy(in, out);
				out.flush();
				final byte[] data = dataStream.toByteArray();
				final ByteArrayInputStream byteStream = new ByteArrayInputStream(
						data);

				// Save the data to the filesystem cache
				if (mFilesystemCache != null) {
					mFilesystemCache.saveFile(mTileSource, tile, byteStream);
					byteStream.reset();
				}
				final Drawable result = mTileSource.getDrawable(byteStream);

				return result;
			} catch (final UnknownHostException e) {
				// no network connection so empty the queue
				logger.warn("WMSMapTileDownloader -- UnknownHostException downloading MapTile: "
						+ tile + " : " + e);
				throw new CantContinueException(e);
			} catch (final LowMemoryException e) {
				// low memory so empty the queue
				logger.warn("WMSMapTileDownloader -- LowMemoryException downloading MapTile: "
						+ tile + " : " + e);
				throw new CantContinueException(e);
			} catch (final FileNotFoundException e) {
				logger.warn("WMSMapTileDownloader -- Tile not found: " + tile
						+ " : " + e);
			} catch (final IOException e) {
				logger.warn("WMSMapTileDownloader -- IOException downloading MapTile: "
						+ tile + " : " + e);
			} catch (final Throwable e) {
				logger.error(
						"WMSMapTileDownloader -- Error downloading MapTile: "
								+ tile, e);
			} finally {
				StreamUtils.closeStream(in);
				StreamUtils.closeStream(out);
			}

			return null;
		}

		@Override
		protected void tileLoaded(final MapTileRequestState pState,
				final Drawable pDrawable) {
			removeTileFromQueues(pState.getMapTile());
			// don't return the tile because we'll wait for the fs provider to
			// ask for it
			// this prevent flickering when a load of delayed downloads complete
			// for tiles
			// that we might not even be interested in any more
			pState.getCallback().mapTileRequestCompleted(pState, null);
		}

	} // end WMSTileLoader
} // end WMSMapTileDownloader
