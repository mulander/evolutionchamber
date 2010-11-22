package com.fray.evo.util;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the EcAutoUpdate class.
 * @author mike.angstadt
 *
 */
public class EcAutoUpdateTest {
	/**
	 * Tests what happens when there is a newer version available.
	 * @throws Exception
	 */
	@Test
	public void testUpdate() throws Exception {
		//version "0013" is an old version so there should be a newer one available
		String oldVersion = "0013";
		EcAutoUpdate auto = new EcAutoUpdate(oldVersion);
		String latestVersion = auto.getLatestVersion();
		Assert.assertTrue(auto.isUpdateAvailable());
		Assert.assertTrue(Integer.parseInt(oldVersion) < Integer.parseInt(latestVersion));

		//download the file
		Assert.assertFalse(auto.isUpdating());
		auto.doInBackground();
		Assert.assertTrue(auto.isUpdating());

		//file should have been downloaded to the current working directory
		File file = new File("evolutionchamber-version-" + latestVersion + ".jar");
		Assert.assertTrue(file.exists());
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
}
