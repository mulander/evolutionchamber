package com.fray.evo.util;

import static com.fray.evo.ui.swingx.EcSwingXMain.messages;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

/**
 * Automatically downloads the newest version of the application and runs it.
 */
public class EcAutoUpdate extends SwingWorker<Void, Void> {
	
	/**
	 * The format of the URL that points to the JAR file.
	 */
	private static final String downloadUrlFormat = "http://evolutionchamber.googlecode.com/files/evolutionchamber-version-%s.jar";
	
	/**
	 * The format of the URL that contains the JAR file's checksum.
	 */
	private static final String	checksumUrlFormat = "http://code.google.com/p/evolutionchamber/downloads/detail?name=evolutionchamber-version-%s.jar";
	
	/**
	 * The regular expression used to scrape the checksum from the webpage of the JAR file.
	 */
	private static final Pattern checksumPattern = Pattern.compile("SHA1 Checksum: ([0-9a-f]+)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	
	/**
	 * Whether or not there is a newer version available.
	 */
	private boolean 			updateAvailable= false;
	
	/**
	 * Whether or not it is performing an update.
	 */
	private boolean				updating       = false;
	
	/**
	 * The version number of the latest version. For example, "0017".
	 */
	private String				latestVersion  = "";
	
	/**
	 * The name of the downloaded JAR file.
	 */
	private String          	jarFile        = "";
	
	/**
	 * Whether or not the checksum of the downloaded file matches the expected checksum.
	 */
	private boolean				checksumMatches = true;

	/**
	 * Constructor.
	 * @param ecVersion the version of the currently running application. For example, "0017".
	 */
	public EcAutoUpdate(String ecVersion) {
		this.latestVersion	 	= findLatestVersion(ecVersion);
		
		if( !this.latestVersion.equals( ecVersion ) )
			this.updateAvailable = true;
	}
	
	/**
	 * Whether or not there is a newer version available.
	 * @return true if a newer version is available, false if not
	 */
	public boolean isUpdateAvailable() {
		return updateAvailable;
	}
	
	/**
	 * Whether or not it is performing an update.
	 * @return true if an update is being performed, false if not.
	 */
	public boolean isUpdating() {
		return updating;
	}
	
	/**
	 * Sets whether or not it is performing an update.
	 * @param updating true if an update is being performed, false if not.
	 */
	public void setUpdating(boolean updating){
		this.updating = updating;
	}
	
	/**
	 * Gets the version number of the latest version. For example, "0017".
	 * @return the latest version number
	 */
	public String getLatestVersion() {
		return latestVersion;
	}
	
	/**
	 * Determines what the latest version of the application is.
	 * @param ecVersion the version of the currently running application. For example, "0017".
	 * @return the latest version number
	 */
	protected String findLatestVersion(String ecVersion) {
		String latestVersion = ecVersion;
		// This implementation assumes that each version posted to the page is incremental
		// if we miss a number (like in release 0011) the downloaded version will be 0010
		// even if newer releases are present.
		try {
			int responseCode = 200;
			while(responseCode == 200) {
				latestVersion = String.format("%04d",Integer.parseInt(latestVersion) + 1);
				URL u = new URL(String.format(downloadUrlFormat, latestVersion));
				responseCode = ( (HttpURLConnection) u.openConnection() ).getResponseCode();
				if (responseCode != 200)
					latestVersion = String.format("%04d",Integer.parseInt(latestVersion) - 1);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// If this happens then our network connection is probably down.
			// We return the current version as there is no way to download any updates.
			JOptionPane.showMessageDialog(null, messages.getString("update.checkFailed.message"), messages.getString("update.checkFailed.title"), JOptionPane.WARNING_MESSAGE);
			latestVersion = ecVersion;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return latestVersion;
	}
	
    @Override
    public Void doInBackground() {
        int progress = 0;
        setProgress(0);
        this.updating = true;
		try {
			//get the file's checksum from the website
			String pageChecksum = null;
			{
				//download the page that contains the checksum
				URL checksumUrl = new URL(String.format(checksumUrlFormat, this.latestVersion));
				HttpURLConnection conn = (HttpURLConnection) checksumUrl.openConnection();
				BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				int read;
				while ((read = in.read()) != -1) {
					out.write(read);
				}
				String html = new String(out.toByteArray());
				in.close();
				out.close();
				
				//scrape the checksum from the page
				Matcher m = checksumPattern.matcher(html);
				if (m.find()){
					pageChecksum = m.group(1);
				}
			}

			//determine whether or not the JAR needs to be downloaded
			boolean download = true;
			URL downloadUrl = new URL(String.format(downloadUrlFormat, this.latestVersion));
			File file = new File(downloadUrl.getFile().substring( downloadUrl.getFile().lastIndexOf('/') + 1));
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			if (file.exists()){
				//the user might have already downloaded the update, but is still running the old version
				//in this case, allow the user to use the JAR that was already downloaded
				
				//calculate the checksum of the file
				FileInputStream in = new FileInputStream(file);
				byte[] buf = new byte[1024];
				int read;
				while ((read = in.read(buf)) != -1){
					md.update(buf, 0, read);
				}
				String fileChecksum = convertToHex(md.digest());
				
				//compare checksums
				if (pageChecksum != null && pageChecksum.equalsIgnoreCase(fileChecksum)){
					//if the checksum matches, then there's no need to download the file again
					download = false;
					checksumMatches = true;
				}
			}
			
			if (download){
				//check to make sure the URL points to the JAR file (binary data) and not something else
			    URLConnection uc 	= downloadUrl.openConnection();
			    String contentType 	= uc.getContentType();
			    int contentLength 	= uc.getContentLength();
			    if (contentType.startsWith("text/") || contentLength == -1) {
			      throw new IOException("This is not a binary file.");
			    }
			    
			    //download the file and also compute its checksum
			    String fileChecksum;
			    {
					FileOutputStream out = new FileOutputStream(file);
				    InputStream raw = uc.getInputStream();
				    InputStream in 	= new BufferedInputStream(raw);
				    byte[] buf = new byte[1024];
				    int bytesRead = 0;
				    int offset = 0;
				    while ((bytesRead = in.read(buf)) != -1){
				    	md.update(buf, 0, bytesRead);
				    	out.write(buf, 0, bytesRead);
				    	offset += bytesRead;
				    	progress = (int)( (offset / (float)contentLength) * 100 );
					    setProgress(progress);
				    }
				    fileChecksum = convertToHex(md.digest());
				    in.close();
				    out.close();
				    
				    if (offset != contentLength) {
				        throw new IOException("Only read " + offset + " bytes; Expected " + contentLength + " bytes");
				    }
			    }
			    
			    //compare checksums
			    //skip the checksum comparison if, for whatever reason, the checksum couldn't be scraped from the website
			    checksumMatches = pageChecksum == null || pageChecksum.equalsIgnoreCase(fileChecksum);
			}

			this.jarFile = file.getName();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			//thrown if the JVM doesn't recognize the "SHA-1" hash algorithm, which should never happen
			e.printStackTrace();
		}
        return null;
    }

    /*
     * Executed in event dispatching thread.
     */
    @Override
    public void done() {
    	setProgress(100);
    	
    	if (checksumMatches){
	    	//run the newly downloaded JAR file (the new version)
	        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
	        String restartCmd[] = new String[] { javaBin, "-jar", this.jarFile};
	        try {
				Runtime.getRuntime().exec( restartCmd );
			} catch (IOException e) {
				e.printStackTrace();
			}
    	} else {
    		JOptionPane.showMessageDialog(null, messages.getString("update.failed.message"), messages.getString("update.failed.title"), JOptionPane.ERROR_MESSAGE);
    	}
		
		//quit the current program (the old version)
        System.exit(0);
    }
    
    /**
     * Converts an array of bytes to a hex string.
     * @see http://www.anyexample.com/programming/java/java_simple_class_to_compute_sha_1_hash.xml
     * 
     * @param data the data to convert
     * @return the hex string
     */
    private static String convertToHex(byte[] data) { 
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < data.length; i++) { 
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do { 
                if ((0 <= halfbyte) && (halfbyte <= 9)) 
                    buf.append((char) ('0' + halfbyte));
                else 
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while(two_halfs++ < 1);
        } 
        return buf.toString();
    }
}
