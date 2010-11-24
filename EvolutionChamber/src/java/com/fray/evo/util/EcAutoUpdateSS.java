package com.fray.evo.util;

import static com.fray.evo.ui.swingx.EcSwingXMain.messages;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

/**
 * Auto-updates the application. Gets the latest version by screen-scraping the
 * download list page.
 * 
 * @author mike.angstadt
 * 
 */
public class EcAutoUpdateSS extends EcAutoUpdate {
	/**
	 * The URL of the project's download page.
	 */
	private static final String downloadsPageUrl = "http://code.google.com/p/evolutionchamber/downloads/list";

	/**
	 * A regular expression that matches against URLs to the various versions of
	 * the application.
	 */
	private static final Pattern jarUrlRegex = Pattern.compile("http://evolutionchamber\\.googlecode\\.com/files/evolutionchamber-version-(\\d+)\\.jar");

	/**
	 * Checks to see if there are any updates available. Allows the user to
	 * download and install the latest version if one exists. Gets the latest
	 * version by screen-scraping the download list page.
	 * 
	 * @param ecVersion the version of the currently running application.
	 * Expected to look like "0017".
	 */
	public EcAutoUpdateSS(String ecVersion) {
		super(ecVersion);
	}

	@Override
	protected String findLatestVersion(String ecVersion) {
		String latestVersion = ecVersion;
		try {
			//get the HTML for the page that lists all the downloads
			String html = null;
			{
				URL url = new URL(downloadsPageUrl);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				int read;
				while ((read = in.read()) != -1) {
					out.write(read);
				}
				html = new String(out.toByteArray());
			}

			//loop through all the links that point to various versions of the application
			//find the greatest version number
			int latest = Integer.parseInt(ecVersion);
			Matcher m = jarUrlRegex.matcher(html);
			while (m.find()) {
				int cur = Integer.parseInt(m.group(1));
				if (cur > latest) {
					latest = cur;
					latestVersion = m.group(1);
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// If this happens then our network connection is probably down.
			// We return the current version as there is no way to download any updates.
			JOptionPane.showMessageDialog(null, messages.getString("update.checkFailed.message"), messages.getString("update.checkFailed.title"), JOptionPane.WARNING_MESSAGE);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return latestVersion;
	}
}
