package net.minecraft.src;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.src.mod_SkinThief.ISkinThief;

public class ConnectionUtils {
	
	private static final Pattern atPattern = Pattern.compile("name=\"authenticityToken\".value=\"[a-z0-9]{40}");
	
	public static final int NONE = 0;
	public static final int URL = 1;
	public static final int SKIN = 2;
	
	public static boolean saveSkinToFile(String skinName, String skinUrl, int skinState, ISkinThief skinThief) {
		File skinDir = new File(ModLoader.getMinecraftInstance().getMinecraftDir(), "skins");
		
		if(!skinDir.exists()) {
			skinDir.mkdir();
		}
		
		File skinFile;
		if(skinState == URL) {
			// Find an open file name.
			for(int i = 0; (skinFile = new File(skinDir, "skin" + ((i != 0) ? i : "") + ".png")).exists(); i++) { }
		} else {
			skinFile = new File(skinDir, skinName + ".png");
			
			if(skinFile.exists()) {
				skinFile.delete();
			}
		}
		
		BufferedInputStream input = null;
		BufferedOutputStream output = null;
		
		try {
			input = new BufferedInputStream(new URL(skinUrl).openStream());
			output = new BufferedOutputStream(new FileOutputStream(skinFile));
			
			byte[] buffer = new byte[8129];
			int length;
			while((length = input.read(buffer)) > 0) {
				output.write(buffer, 0, length);
			}
		} catch (IOException e) {
			skinThief.showNotification("Could not save skin to file!");
			e.printStackTrace();
			return false;
		} finally {
			if(input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			if(output != null) {
				try {
					output.flush();
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return true;
	}
	
	public static void doSkinChange(String skinUrl, ISkinThief skinThief) throws IOException {
		HttpURLConnection.setFollowRedirects(false);
		
		// Get login page
		HttpURLConnection getLogin = (HttpURLConnection) new URL("http://www.minecraft.net/login").openConnection();
		
		getLogin.setRequestMethod("GET");
		getLogin.setDoInput(true);
		getLogin.setDoOutput(false);
		
		String content = getContent(getLogin);
		String authToken = getAuthToken(content);
		
		Map<String, String> cookies = getCookies(getLogin);

		printHeader(getLogin);
		printCookies(cookies);
		printBorder();
		
		getLogin.disconnect();
		
		
		// Do login
		HttpURLConnection doLogin = (HttpURLConnection) new URL("http://www.minecraft.net/login").openConnection();
		
		doLogin.setRequestMethod("POST");
		doLogin.setDoInput(true);
		doLogin.setDoOutput(true);
		
		doLogin.setRequestProperty("Origin", "www.minecraft.net");
		doLogin.setRequestProperty("Referer", "www.minecraft.net/login");
		doLogin.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		setCookies(doLogin, cookies);
		
		OutputStreamWriter writer = new OutputStreamWriter(doLogin.getOutputStream());
		writer.write(buildLoginRequest(authToken, skinThief.getUsername(), skinThief.getPassword()));
		writer.flush();
		
		cookies = getCookies(doLogin);
		
		printHeader(doLogin);
		printCookies(cookies);
		printBorder();
		
		doLogin.disconnect();
		writer.close();
		
		
		// Follow re-direct
		HttpURLConnection redirect = (HttpURLConnection) new URL("http://www.minecraft.net").openConnection();
		
		redirect.setRequestMethod("GET");
		redirect.setDoInput(true);
		redirect.setDoOutput(false);
		
		redirect.setRequestProperty("Referer", "http://www.minecraft.net/login");
		
		setCookies(redirect, cookies);
		
		content = getContent(redirect);
		if(!content.contains("Logged in")) {
			skinThief.showNotification("Username or password incorrect!");
			return;
		}
		
		cookies = getCookies(redirect);
		
		printHeader(redirect);
		printCookies(cookies);
		printBorder();
		
		redirect.disconnect();
		
		
		// Get profile page
		HttpURLConnection getProfile = (HttpURLConnection) new URL("http://www.minecraft.net/profile").openConnection();
		
		getProfile.setRequestMethod("GET");
		getProfile.setDoInput(true);
		getProfile.setDoOutput(false);
		
		getProfile.setRequestProperty("Referer", "http://www.minecraft.net/profile");
		
		setCookies(getProfile, cookies);
		
		content = getContent(getProfile);
		authToken = getAuthToken(content);
		cookies = getCookies(getProfile);
		
		printHeader(getProfile);
		printCookies(cookies);
		printBorder();
		
		getProfile.disconnect();
		
		
		// Do skin change
		HttpURLConnection doSkinChange = (HttpURLConnection) new URL("http://www.minecraft.net/profile/skin").openConnection();
		doSkinChange.setRequestMethod("POST");
		doSkinChange.setDoInput(true);
		doSkinChange.setDoOutput(true);
		
		doSkinChange.setRequestProperty("Referer", "http://www.minecraft.net/profile");
		
		String boundary = Long.toHexString(System.currentTimeMillis());
		doSkinChange.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
		
		setCookies(doSkinChange, cookies);
		
		DataOutputStream output = null;
		try {
			// -Test-URLs-
			// DJ Creeper: http://www.minecraftskins.com/uploaded_skins/skin_12022609023140541.png
			// Skin: http://dl.dropbox.com/u/22652332/skin.png
			
			URL url = new URL(skinUrl);
			
			String[] split = url.getFile().split("/");
			String filename = split[split.length - 1];
			
			output = new DataOutputStream(doSkinChange.getOutputStream());
			output.writeBytes(	"--" + boundary + "\r\n" +
								"Content-Disposition: form-data; name=\"authenticityToken\"\r\n" +
								"\r\n" +
								authToken + "\r\n" +
								"--" + boundary + "\r\n" +
								"Content-Disposition: form-data; name=\"skin\"; filename=\"" + filename + "\"\r\n" + 
								"Content-Type: " + getImageMIME(filename) + "\r\n" + 
								"\r\n");
			
			BufferedInputStream input = null;
			try { 
				input = new BufferedInputStream(url.openStream());
				byte[] buffer = new byte[8129];
				for(int length = 0; (length = input.read(buffer)) > 0;) {
					output.write(buffer, 0, length);
				}
			} finally {
				if(input != null) {
					input.close();
				}
			}
			
			output.writeBytes("\r\n" + "--" + boundary + "--\r\n");
			output.flush();
		} finally {
			if(output != null) {
				output.flush();
				output.close();
			}
		}
		
		cookies = getCookies(getProfile);
		
		printHeader(doSkinChange);
		printCookies(cookies);
		printBorder();
		
		doSkinChange.disconnect();
	}
	
	public static String getContent(URLConnection connection) {
		BufferedInputStream input = null;
		StringBuffer content = null;
		
		try {
			input = new BufferedInputStream(connection.getInputStream());
			content = new StringBuffer();
			
			byte[] buffer = new byte[8129];
			int length;
			while((length = input.read(buffer)) > 0) {
				content.append(new String(buffer, 0, length));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return (content != null) ? content.toString() : "";
	}
	
	private static void printBorder() {
		System.out.println("-------------------------------------");
	}
	
	private static void printCookies(Map<String, String> cookies) {
		for(String cookieName : cookies.keySet()) {
			System.out.println(cookieName + "=" + cookies.get(cookieName));
		}
	}
	
	private static void printHeader(URLConnection connection) {
		String headerName = null;
		for(int i = 1; (headerName = connection.getHeaderFieldKey(i)) != null; i++) {
			System.out.println(headerName + ": " + connection.getHeaderField(i));
		}
	}
	
	private static Map<String, String> getCookies(URLConnection connection) {
		Map<String, String> cookies = new HashMap<String, String>();
		
		String headerName = null;
		for(int i = 1; (headerName = connection.getHeaderFieldKey(i)) != null; i++) {
			if(headerName.equals("Set-Cookie")) {
				String cookieContents = connection.getHeaderField(i);
				
				String cookie = cookieContents.substring(0, cookieContents.indexOf(";"));
				String cookieName = cookie.substring(0, cookie.indexOf("="));
				String cookieValue = cookie.substring(cookie.indexOf("=") + 1);
				
				cookies.put(cookieName, cookieValue);
			}
		}
		
		return cookies;
	}
	
	private static void setCookies(URLConnection connection, Map<String, String> cookies) {
		StringBuffer requestCookie = new StringBuffer();
		
		Set<String> set = cookies.keySet();
		Iterator<String> it = set.iterator();
		
		while(it.hasNext()) {
			String cookieName = it.next();
			requestCookie.append(cookieName + "=" + cookies.get(cookieName) + (it.hasNext() ? "; " : ""));
		}
		
		connection.setRequestProperty("Cookie", requestCookie.toString());
	}
	
	private static String buildLoginRequest(String authToken, String username, String password) {
		return new StringBuilder()
			.append("authenticityToken=").append(encode(authToken))
			.append("&username=").append(encode(username))
			.append("&password=").append(encode(password))
			.append("&remember=").append(encode("true"))
			.toString();
	}
	
	private static String encode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
			return s;
		}
	}
	
	private static String getAuthToken(String content) {
		Matcher matcher = atPattern.matcher(content);
		
		if(matcher.find()) {
			String unparsedToken = matcher.toMatchResult().group();
			return unparsedToken.substring(unparsedToken.indexOf("value=\"") + 7);
		} else {
			throw new RuntimeException("Could not parse authenticityToken!");
		}
	}
	
	private static String getImageMIME(String filename) {
		if(filename.contains(".png")) {
			return "image/png";
		} else if(filename.contains(".jpg") || filename.contains(".jpeg")) {
			return "image/jpeg";
		} else if(filename.contains(".gif")) {
			return "image/gif";
		} else if(filename.contains(".bmp")) {
			return "image/x-bmp";
		} else {
			return null;
		}
	}
	
	public static int UrlorSkin(String skin, ISkinThief skinThief) {
		if(skin.contains("http://") || skin.contains("www.") || skin.contains(".com") || skin.contains(".net")) {
			if(isValidUrl(skin, skinThief)) {
				return URL;
			} else {
				return NONE;
			}
		} else {
			if(isPlayer(skin, skinThief)) {
				return SKIN;
			} else {
				return NONE;
			}
		}
	}
	
	private static boolean isValidUrl(String url, ISkinThief skinThief) {
		if(!(url.contains(".png") || url.contains(".jpg") || url.contains(".gif") || url.contains(".bmp") || url.contains(".jpeg"))) {
			skinThief.showNotification("URL is not a picture!");
			return false;
		} else {
			try {
				URL checkUrl = new URL(url);
				try {
					URLConnection connection = checkUrl.openConnection();
					connection.connect();
					return true;
				} catch(IOException e){
					skinThief.showNotification("Can't connect to URL!");
					return false;
				}
			} catch (MalformedURLException e) {
				skinThief.showNotification("URL is not valid!");
				return false;
			}
		}
	}
	
	private static boolean isValidImage(String url) {
		return (url.contains(".png") || url.contains(".jpg") || url.contains(".gif") || url.contains(".bmp") || url.contains(".jpeg"));
	}
	
	private static boolean isPlayer(String playerName, ISkinThief skinThief) {
		HttpURLConnection checkNameConn;
		try {
			checkNameConn = (HttpURLConnection) new URL("http://www.minecraft.net/haspaid.jsp?user=" + playerName).openConnection();
			
			String content = ConnectionUtils.getContent(checkNameConn);
			
			if(content.trim().contains("true")){
				return true;
			} else {
				skinThief.showNotification("Player name does not exist!");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}
