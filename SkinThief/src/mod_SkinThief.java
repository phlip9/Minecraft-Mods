package net.minecraft.src;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.Socket;
import java.util.Properties;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;

public class mod_SkinThief extends BaseMod {

	private String playerName;
	private String username;
	private String password;
	
	private long lastKeyPress = 0L;
	private long notificationStart;
	private String notificationText = "";
	private boolean showNotification = false;
	private static final long NOTIFICATION_LENGTH = 2000L;
	private static final int skip = 6;
	
	private boolean shouldReconnect = false;
	
	//private boolean showTest = false;
	
	private Minecraft mc;

	private Properties properties;
	private int guiOpenKey;
	private File configFile;
	
	private static final String defaultKey = "P";

	private void loadConfig() throws FileNotFoundException, IOException{
		properties = new Properties();
		
		if(configFile.exists()){
			properties.load(new FileInputStream(configFile));
			
			guiOpenKey = Keyboard.getKeyIndex(properties.getProperty("guiOpenKey", defaultKey));
			playerName = properties.getProperty("lastPlayerName", mc.session.username);
			
		} else {
			configFile.createNewFile();
			properties.load(new FileInputStream(configFile));
			
			properties.setProperty("guiOpenKey", defaultKey);
			properties.setProperty("lastPlayerName", mc.session.username);

			guiOpenKey = Keyboard.getKeyIndex(defaultKey);
			playerName = mc.session.username;

			properties.store(new FileOutputStream(configFile), "SkinThief Config File");
		}
	}

	private void getLastlogin() {
		DataInputStream input = null;
		try {
			File lastLogin = new File(mc.getMinecraftDir(), "lastlogin");

			Random random = new Random(43287234L);
		    byte[] salt = new byte[8];
		    random.nextBytes(salt);
		    PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, 5);

		    SecretKey pbeKey = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(new PBEKeySpec("passwordfile".toCharArray()));
		    Cipher cipher = Cipher.getInstance("PBEWithMD5AndDES");
		    cipher.init(2, pbeKey, pbeParamSpec);
			
			if (cipher != null) {
				input = new DataInputStream(new CipherInputStream(new FileInputStream(lastLogin), cipher));
			}
			
			if (input != null) {
				username = input.readUTF();
				password = input.readUTF();
			}
		} catch (Exception e) {
			username = mc.session.username;
			password = "";
		} finally {
			if(input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void drawCenteredString(String s, int i, int j, int k) {
		mc.fontRenderer.drawStringWithShadow(s, i - mc.fontRenderer.getStringWidth(s) / 2, j, k);
	}

	private void drawRoundedRect(int x, int y, int l, int w, int r, int c) {
		if(r < 0) {
			r = 0;
		}
		
		float a = (float)(c >> 24 & 0xff) / 255F;
		float r1 = (float)(c >> 16 & 0xff) / 255F;
		float g = (float)(c >> 8 & 0xff) / 255F;
		float b = (float)(c & 0xff) / 255F;
		
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		
		GL11.glColor4f(r1, g, b, a);
		
		Tessellator t = Tessellator.instance;
		
		t.startDrawing(GL11.GL_TRIANGLE_FAN);
		
		t.addVertex(x + (l / 2), y + (w / 2), 0.0D); // rectangle center
		
		// lower right quarter circle
		for(int i = 0; i < 90; i += skip) {
			t.addVertex(x + l - r + (Math.sin((i * Math.PI / 180)) * r), y + w - r + (Math.cos((i * Math.PI / 180)) * r), 0.0D);
		}
		
		// upper right quarter circle
		for(int i = 90; i < 180; i += skip) {
			t.addVertex(x + l - r + (Math.sin((i * Math.PI / 180)) * r), y + r + (Math.cos((i * Math.PI / 180)) * r), 0.0D);
		}
		
		// upper left quarter circle
		for(int i = 180; i < 270; i += skip) {
			t.addVertex(x + r + (Math.sin((i * Math.PI / 180)) * r), y + r + (Math.cos((i * Math.PI / 180)) * r), 0.0D);
		}
		
		// lower left quarter circle
		for(int i = 270; i < 360; i += skip) {
			t.addVertex(x + r + (Math.sin((i * Math.PI / 180)) * r), y + w - r + (Math.cos((i * Math.PI / 180)) * r), 0.0D);
		}
		
		t.addVertex(x + l - r, y + w, 0.0D);
		
		t.draw();
		
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
	
	@Override
	public boolean onTickInGame(float f, final Minecraft mc) {
		if(mc.theWorld != null && mc.thePlayer != null){
			if(showNotification) {
				ScaledResolution sr = new ScaledResolution(mc.gameSettings, mc.displayWidth, mc.displayHeight);
				int width = sr.getScaledWidth();
				int height = sr.getScaledHeight();
				
				long deltaTime = System.currentTimeMillis() - notificationStart;
				if (deltaTime < NOTIFICATION_LENGTH) {
					
					int stringWidth = mc.fontRenderer.getStringWidth(notificationText);
					
					int x = width / 2 - ((stringWidth / 2) + 10);
					int y = height / 3 - 12;
					
					drawRoundedRect(x - 2, y - 2, stringWidth + 20 + 4, 30 + 4, 5, 0xff1c1c1c);
					drawRoundedRect(x, y, stringWidth + 20, 30, 5 - 2, 0xff464646);
					
					drawCenteredString(notificationText, width / 2, height / 3, 0xffffff);
					
				} else {
					showNotification = false;
				}
			}
			
			/*if(showTest) {
				ScaledResolution sr = new ScaledResolution(mc.gameSettings, mc.displayWidth, mc.displayHeight);
				int width = sr.getScaledWidth();
				int height = sr.getScaledHeight();
				
				drawRoundedRect(width / 2 - 20 - 2, height / 2 - 20 - 2, 40 + 4, 40 + 4, 5, 0xff1c1e1e);
				drawRoundedRect(width / 2 - 20, height / 2 - 20, 40, 40, 5 - 2, 0xff464646);
			}*/
			
			if(shouldReconnect && mc.currentScreen == null && mc.isMultiplayerWorld()) {
				shouldReconnect = false;
				reconnect();
			}
			
			/*if(Keyboard.isKeyDown(Keyboard.KEY_Y) && System.currentTimeMillis() - lastKeyPress > 300L && mc.currentScreen == null){
				lastKeyPress = System.currentTimeMillis();
				
				showTest = !showTest;
				
				System.out.println("Y pressed!");
			}*/
			
			if(Keyboard.isKeyDown(guiOpenKey) && System.currentTimeMillis() - lastKeyPress > 300L && mc.currentScreen == null){
				lastKeyPress = System.currentTimeMillis();
				
				mc.displayGuiScreen(new GuiSkinThief(new ISkinThief() {
					@Override
					public void setLastName(String _playerName) {
						playerName = _playerName;
					}
					
					@Override
					public String getLastName() {
						return playerName;
					}

					@Override
					public String getUsername() {
						return username;
					}

					@Override
					public String getPassword() {
						return password;
					}

					@Override
					public void setUsername(String _username) {
						username = _username;
					}

					@Override
					public void setPassword(String _password) {
						password = _password;
					}

					@Override
					public void showNotification(String notification) {
						show_notification(notification);
					}

					@Override
					public void markForReconnect() {
						shouldReconnect = true;
					}
				}));
			}
		}
		
		return true;
	}
	
	private void reconnect() {
		String server = null;
		int port = 0;
		
		try {
			Field f1 = WorldClient.class.getDeclaredField("H" /*sendQueue*/);
			f1.setAccessible(true);
			
			NetClientHandler netHandler = (NetClientHandler) f1.get(mc.theWorld);
			
			Field f2 = NetClientHandler.class.getDeclaredField("g" /*netManager*/);
			f2.setAccessible(true);
			
			NetworkManager netManager = (NetworkManager) f2.get(netHandler);
			
			Field f3 = NetworkManager.class.getDeclaredField("h" /*networkSocket*/);
			f3.setAccessible(true);
			
			Socket socket = (Socket) f3.get(netManager);
			
			server = socket.getInetAddress().getHostAddress();
			port = socket.getPort();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		
		if(server != null && port != 0) {
			System.out.println("Server: " + server + ":" + port);
			
			mc.theWorld.sendQuittingDisconnectingPacket();
			
			mc.changeWorld1(null);
			mc.displayGuiScreen(new GuiConnecting(mc, server, port));
		}
	}

	private void show_notification(String notification) {
		notificationText = notification;
		notificationStart = System.currentTimeMillis();
		showNotification = true;
	}
	
	@Override
	public String getVersion() {
		return "1.9";
	}

	@Override
	public void load() {
		mc = ModLoader.getMinecraftInstance();
		configFile = new File(mc.getMinecraftDir(), "mods/SkinThief.config");
		
		try {
			loadConfig();
		} catch (FileNotFoundException e) {
			System.out.println("[SkinThief] Config file not found!\n" + e.getMessage());
		} catch (IOException e) {
			System.out.println("[SkinThief] Failed saving or loading config file, not enough permissions?\n" + e.getMessage());
		}
		
		getLastlogin();
		
		ModLoader.setInGameHook(this, true, false);
	}
	
	
	public interface ISkinThief {
		public String getLastName();
		public void setLastName(String name);
		
		public String getUsername();
		public void setUsername(String username);
		
		public String getPassword();
		public void setPassword(String password);
		
		public void showNotification(String notification);
		
		public void markForReconnect();
	}
}
