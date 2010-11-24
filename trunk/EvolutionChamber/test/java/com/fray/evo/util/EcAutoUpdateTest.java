package com.fray.evo.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the EcAutoUpdate class.
 * 
 * @author mike.angstadt
 * 
 */
public class EcAutoUpdateTest {
	/**
	 * Tests what happens when there is a newer version available.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdate() throws Exception {
		//version "0013" is an old version so there should be a newer one available
		String oldVersion = "0013";
		EcAutoUpdate auto = new EcAutoUpdate(oldVersion);
		auto.addPropertyChangeListener(new DisplayDownloadProgress());
		String latestVersion = auto.getLatestVersion();
		File file = new File("evolutionchamber-version-" + latestVersion + ".jar");
		file.delete(); //delete the file if it exists from a previous test run

		Assert.assertTrue(auto.isUpdateAvailable());
		Assert.assertTrue(Integer.parseInt(oldVersion) < Integer.parseInt(latestVersion));

		//download the file
		Assert.assertFalse(auto.isUpdating());
		auto.doInBackground();
		Assert.assertTrue(auto.isUpdating());

		//file should have been downloaded to the current working directory
		Assert.assertTrue(file.exists());
		long lastModified = file.lastModified();

		//run the update again
		//since the file was already downloaded, it should not re-download it
		auto = new EcAutoUpdate(oldVersion);
		auto.doInBackground();
		Assert.assertEquals(lastModified, file.lastModified()); //the file should not have changed

		//run the update again
		//if a file exists with the same name of the JAR file, but is *not* the JAR file, then it should notice this and re-download the JAR file
		PrintWriter out = new PrintWriter(new FileOutputStream(file));
		out.print("This is just some text file that happens to have the same name as the Evolution Chamber JAR.");
		out.close();
		lastModified = file.lastModified();
		auto = new EcAutoUpdate(oldVersion);
		auto.addPropertyChangeListener(new DisplayDownloadProgress());
		auto.doInBackground();
		Assert.assertTrue(lastModified < file.lastModified());

		file.delete();
	}

	/**
	 * Test what happens when no update needs to be performed (the user already
	 * has the most up to date version).
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAlreadyLatest() throws Exception {
		//get the latest version
		EcAutoUpdate auto = new EcAutoUpdate("0000");
		String latestVersion = auto.getLatestVersion();

		//try to update from the latest version
		//there should be no update available
		auto = new EcAutoUpdate(latestVersion);
		Assert.assertFalse(auto.isUpdateAvailable());
	}

	/**
	 * Displays the download progress of an update to stdout.
	 * 
	 * @author mike.angstadt
	 * 
	 */
	private class DisplayDownloadProgress implements PropertyChangeListener {
		private boolean first = true;

		public void propertyChange(PropertyChangeEvent evt) {
			if ("progress".equals(evt.getPropertyName())) {
				if (first) {
					first = false;
					System.out.print("Downloading ... ");
				}
				int value = (Integer) evt.getNewValue();
				if (value % 10 == 0) {
					System.out.print(value + "%");
					if (value == 100) {
						System.out.println();
					} else {
						System.out.print(" ... ");
					}
				}
			}
		}
	}
}
