package pl.openstreetmap.dotevo.strazak;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.Map.Entry;

import android.util.Log;

/* File from org.osm.kaypadmapper2
 * Thanks to God for OpenSource! :-)
 */

public class OsmWriter {
	private BufferedWriter osmFile;
	public String path;
	private int newNodeId = -1;

	/**
	 * Creates or opens a OSM file. There is no check whether the file exists,
	 * this has to be done by the caller. If append is set to false and the file
	 * exists, it will be overwritten. If append is set to true the new data
	 * will be added to the file. Please note that appending is only possible
	 * for files created by this class, appending to third-party files may break
	 * them.
	 * 
	 * @param path
	 *            the file to create or open
	 * @param append
	 *            set to true to append to existing file
	 * @throws FileNotFoundException
	 *             if the file cannot be opened or created
	 * @throws IOException
	 *             if any other I/O error occurs
	 * @throws FileFormatException
	 *             if the end of the OSM file is not as expected
	 */
	public OsmWriter(String path, boolean append) throws IOException {
		if (!append) {
			// create/overwrite file
			osmFile = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(path), "UTF-8"));
			this.path = path;

			osmFile.write("<?xml version='1.0' encoding='UTF-8'?>\n");
			osmFile.write("<osm version='0.6' generator='Pikietaz'>\n");
		} else {
			// append to existing (and initialised) file
			File oldOsmFile = new File(path);
			File tempOsmFile = new File(path + "~");
			BufferedReader oldOsmReader = new BufferedReader(
					new InputStreamReader(new FileInputStream(oldOsmFile),
							"UTF-8"));
			osmFile = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(tempOsmFile), "UTF-8"));

			this.path = path;

			// replace file, reopening the last track segment (remove everything
			// added by close())
			String line;
			while (true) {
				line = oldOsmReader.readLine();
				if (line == null) {
					// found end of file without </osm> - file is damaged,
					// delete temporary file
					osmFile.close();
					tempOsmFile.delete();
					throw new FileFormatException();
				}
				if (line.trim().equalsIgnoreCase("</osm>")) {
					// replace file
					osmFile.flush();
					oldOsmReader.close();
					oldOsmFile.delete();
					tempOsmFile.renameTo(oldOsmFile);
					break;
				} else {
					osmFile.write(line + "\n");
				}
			}
		}
	}

	/**
	 * Closes this OSM file. The file will also be closed.
	 * 
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public void close() throws IOException {
		osmFile.write("</osm>\n");
		osmFile.close();
		if (newNodeId == -1) {
			File file = new File(path);
			Log.i("Remove", String.valueOf(file.delete()));
		}
	}

	/**
	 * Adds a new node to the OSM file.
	 * 
	 * @param lat
	 *            WGS84 latitude of the new node
	 * @param lon
	 *            WGS84 longitude of the new node
	 * @param tags
	 *            Map containing the tags for the new node.
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public void addNode(double lat, double lon, Map<String, String> tags)
			throws IOException {
		Log.i("Add", "ID:" + String.valueOf(newNodeId));
		osmFile.write("\t<node id=\"" + newNodeId--
				+ "\" visible=\"true\" lat=\"" + lat + "\" lon=\"" + lon
				+ "\">\n");
		for (Entry<String, String> entry : tags.entrySet()) {
			osmFile.write("\t\t<tag k=\"" + entry.getKey() + "\" v=\""
					+ entry.getValue() + "\"/>\n");
		}
		osmFile.write("\t</node>\n");
	}

	/**
	 * Returns the complete path of the GPX file.
	 */
	@Override
	public String toString() {
		return path;
	}

	public void flush() throws IOException {
		osmFile.flush();
	}
}