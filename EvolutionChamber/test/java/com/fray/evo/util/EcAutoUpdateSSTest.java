package com.fray.evo.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the EcAutoUpdateSS class.
 * @author mike.angstadt
 *
 */
public class EcAutoUpdateSSTest {
	/**
	 * Tests what happens when there is a newer version available.
	 * @throws Exception
	 */
	@Test
	public void testUpdate() throws Exception {
		//version "0013" is an old version so there should be a newer one available
		String oldVersion = "0013";
		EcAutoUpdateSS auto = new EcAutoUpdateSS(oldVersion, new CallbackImpl());
		String latestVersion = auto.getLatestVersion();
		Assert.assertTrue(auto.isUpdateAvailable());
		Assert.assertTrue(Integer.parseInt(oldVersion) < Integer.parseInt(latestVersion));
		
		//no need to test the download code because this is tested in the parent class's test class
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
		EcAutoUpdateSS auto = new EcAutoUpdateSS("0000", new CallbackImpl());
		String latestVersion = auto.getLatestVersion();

		//try to update from the latest version
		//there should be no update available
		auto = new EcAutoUpdateSS(latestVersion, new CallbackImpl());
		Assert.assertFalse(auto.isUpdateAvailable());
	}
	
	/**
	 * Callback implementation for the auto-update class.
	 * @author mike.angstadt
	 *
	 */
	private class CallbackImpl implements EcAutoUpdate.Callback {
		@Override
		public void checksumFailed() {
			Assert.fail("Checksum of the downloaded file did not match the expected checksum.");
		}

		@Override
		public void updateCheckFailed() {
			Assert.fail("Could not find the latest version.  Check your internet connection.");
		}
	}
}