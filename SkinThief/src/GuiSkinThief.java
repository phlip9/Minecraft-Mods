package net.minecraft.src;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import net.minecraft.src.mod_SkinThief.ISkinThief;

public class GuiSkinThief extends GuiScreen {

	private GuiTextField skinTextField;
	private GuiTextField usernameTextField;
	private GuiTextField passwordTextField;
	
	private boolean save = false;
	private boolean permanent = false;
	private ISkinThief skinThief;
	private long startTime;
	
	private Minecraft mc = ModLoader.getMinecraftInstance();
	
	private static final int STEAL = 0;
	private static final int CANCEL = 1;
	private static final int SAVE = 2;
	private static final int PERMANENT = 3;

	public GuiSkinThief(ISkinThief skinThief) {
		startTime = System.currentTimeMillis();
		
		this.skinThief = skinThief;
	}

	public void updateScreen() {
		skinTextField.updateCursorCounter();
		((GuiButton)controlList.get(0)).enabled = skinTextField.getText().trim().length() > 0;
		((GuiButton)controlList.get(2)).displayString = saveSkinToFileYesNo(save);
		((GuiButton)controlList.get(3)).displayString = changeCurrentSkinYesNo(permanent);
		super.updateScreen();
	}

	public void initGui() {
		Keyboard.enableRepeatEvents(true);
		
		controlList.clear();
		controlList.add(new GuiButton(STEAL, width / 2 - 100, 70, "Steal"));
		controlList.add(new GuiButton(CANCEL, width / 2 - 100, 114, "Cancel"));
		controlList.add(new GuiButton(SAVE, width / 2 - 100, 92, 99, 20, saveSkinToFileYesNo(save)));
		controlList.add(new GuiButton(PERMANENT, width / 2 + 2, 92, 99, 20, changeCurrentSkinYesNo(permanent)));
		
		skinTextField = new GuiTextField(this, fontRenderer, width / 2 - 100 - 8, 40, 220, 20, skinThief.getUsername());
		skinTextField.isFocused = true;
		
		usernameTextField = new GuiTextField(this, fontRenderer, width / 2 - 48, 160, 160, 20, skinThief.getUsername());
		usernameTextField.setMaxStringLength(30);
		usernameTextField.isFocused = false;
		
		StringBuilder asterisks = new StringBuilder();
		for(int i = 0; i < skinThief.getPassword().length(); i++) {
			asterisks.append('*');
		}
		
		passwordTextField = new GuiTextField(this, fontRenderer, width / 2 - 48, 185, 160, 20, asterisks.toString());
		passwordTextField.setMaxStringLength(30);
		passwordTextField.isFocused = false;
	}

	public void onGuiClosed() {
		Keyboard.enableRepeatEvents(false);
	}

	private String saveSkinToFileYesNo(boolean yesNo) {
		String tempYesNo;
		if(yesNo){
			tempYesNo = "Yes";
		} else {
			tempYesNo = "No";
		}
		
		String buttonText = (new StringBuilder().append("Save To File: ").append(tempYesNo).toString());
		return buttonText;
	}
	
	private String changeCurrentSkinYesNo(boolean yesNo) {
		String tempYesNo;
		if(yesNo){
			tempYesNo = "Yes";
		} else {
			tempYesNo = "No";
		}
		
		String buttonText = (new StringBuilder().append("Permanent: ").append(tempYesNo).toString());
		return buttonText;
	}

	protected void actionPerformed(GuiButton guibutton) {
		if(!guibutton.enabled){
			return;
		}
		
		if(guibutton.id == STEAL){
			changeSkin(skinTextField.getText(), permanent, save);
		}
		if(guibutton.id == CANCEL){
			mc.displayGuiScreen(null);
			mc.setIngameFocus();
		}
		if(guibutton.id == SAVE){
			save = !save;
		}
		if(guibutton.id == PERMANENT){
			permanent = !permanent;
		}
	}

	private boolean handlePaste(char c, GuiTextField textField) {
		if(c == '\26') {
			textField.setText(GuiScreen.getClipboardString().trim());
			return true;
		} else {
			return false;
		}
	}
	
	protected void keyTyped(char c, int i) {
		if(System.currentTimeMillis() - startTime > 300L){
			
			if (skinTextField.isFocused && skinTextField.isEnabled) {
				if(handlePaste(c, skinTextField)) {
					return;
				}
				
				skinTextField.textboxKeyTyped(c, i);
			}
			
			if(usernameTextField.isFocused && usernameTextField.isFocused) {
				if(handlePaste(c, usernameTextField)) {
					return;
				}
				
				usernameTextField.textboxKeyTyped(c, i);
				
				skinThief.setUsername(usernameTextField.getText().trim());
			}
			
			if(passwordTextField.isFocused && passwordTextField.isFocused) {
				if(handlePaste(c, passwordTextField)) {
					return;
				}
				
				if(i == Keyboard.KEY_BACK) {
					String password = skinThief.getPassword();
					
					int length = password.length();
					if(length > 0) {
						skinThief.setPassword(password.substring(0, password.length() - 1));
					}
					
					c = (char) Keyboard.KEY_BACK;
				} else if(c != '\t' && c != '\026' && (ChatAllowedCharacters.allowedCharacters.indexOf(c) >= 0 || c > ' ')) {
					skinThief.setPassword(skinThief.getPassword().concat(String.valueOf(c)));
					
					c = '*';
					i = 0;
				}
				
				passwordTextField.textboxKeyTyped(c, i);
			}
			
			if(c == '\r' || c == '\n'){
				actionPerformed((GuiButton) controlList.get(0));
			}
		}
	}

	public void handleKeyboardInput() {
		if(Keyboard.getEventKeyState()){
			
			int key = Keyboard.getEventKey();
			char c = Keyboard.getEventCharacter();
			
			if(key == 1){
				mc.displayGuiScreen(null);
				mc.setIngameFocus();
			}
			
			if(c == '\t') {
				if(skinTextField.isFocused) {
					skinTextField.isFocused = false;
					usernameTextField.isFocused = true;
				} else if(usernameTextField.isFocused) {
					usernameTextField.isFocused = false;
					passwordTextField.isFocused = true;
				} else {
					passwordTextField.isFocused = false;
					skinTextField.isFocused = true;
				}
				
				return;
			}
			
			keyTyped(Keyboard.getEventCharacter(), key);
		}
	}

	protected void mouseClicked(int x, int y, int button) {
		skinTextField.mouseClicked(x, y, button);
		
		usernameTextField.mouseClicked(x, y, button);
		passwordTextField.mouseClicked(x, y, button);
		
		super.mouseClicked(x, y, button);
	}

	public void drawScreen(int x, int y, float f) {
		drawDefaultBackground();
		drawCenteredString(fontRenderer, "Change Skin", width / 2, 12, 0xffffff);
		drawCenteredString(fontRenderer, "Enter a player name or image URL.", width / 2, 27, 0xa0a0a0);
		
		skinTextField.drawTextBox();
		
		drawCenteredString(fontRenderer, "Permanent?", width / 2, 148, 0xffffff);
		
		drawString(fontRenderer, "Username: ", width / 2 - 101, 166, 0xa0a0a0);
		usernameTextField.drawTextBox();
		
		drawString(fontRenderer, "Password: ", width / 2 - 101, 191, 0xa0a0a0);
		passwordTextField.drawTextBox();
		super.drawScreen(x, y, f);
	}

	/*private int alphaFade(long deltaTime, int solidColor) {
		double falpha = -1 * (Math.pow(((double) deltaTime / (double) fadeTime), 2.0D)) + 1;
		
		int alpha = (int) (falpha * 0xFF);
		
		alpha = (alpha < 10 && alpha >= 0) ? 10 : alpha;
		
		return (((alpha & 0xFF) << 24) + solidColor);
	}*/
	
	private void changeSkin(final String skinName, final boolean permanent, final boolean save) { 
		new Thread() {
			@Override
			public void run() {
				int state = ConnectionUtils.UrlorSkin(skinName, skinThief);
				
				if(state == ConnectionUtils.SKIN) {
					skinThief.setLastName(skinName);
				} else if(state == ConnectionUtils.NONE) {
					return;
				}
				
				String skinUrl = (state == ConnectionUtils.URL) ? skinName : new StringBuilder().append("http://s3.amazonaws.com/MinecraftSkins/").append(skinName).append(".png").toString();
				
				if(permanent) {
					if(skinThief.getUsername().isEmpty()) {
						skinThief.showNotification("Username field is empty!");
						return;
					}
					
					if(skinThief.getPassword().isEmpty()) {
						skinThief.showNotification("Password field is empty!");
						return;
					}
				
					try {
						ConnectionUtils.doSkinChange(skinUrl, skinThief);
					} catch (Exception e) {
						e.printStackTrace();
						skinThief.showNotification("Error changing skin!");
					}
				}
				
				try {
					purgeSkinCache();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				if(permanent && mc.isMultiplayerWorld()) {
					skinThief.markForReconnect();
				} else {
					mc.thePlayer.skinUrl = skinUrl;
					mc.renderGlobal.obtainEntitySkin(mc.thePlayer);
				}
				
				mc.displayGuiScreen(null);
				mc.setIngameFocus();
				
				if(save) {
					if(!ConnectionUtils.saveSkinToFile(skinName, skinUrl, state, skinThief)) {
						skinThief.showNotification("Error saving skin!");
					}
				}
			}
		}.start();
	}
	
	private void purgeSkinCache() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		if(mc.thePlayer.skinUrl != null) {
			Field f = RenderEngine.class.getDeclaredField("j" /*urlToImageDataMap*/);
			f.setAccessible(true);
			
			Map urlImageMap = (Map) f.get(mc.renderEngine);
			ThreadDownloadImageData imageData = (ThreadDownloadImageData) urlImageMap.get(mc.thePlayer.skinUrl);
			
			if(imageData != null) {
				imageData.referenceCount--;
				
				if(imageData.referenceCount == 0) {
					if(imageData.textureName >= 0) {
						Field f1 = RenderEngine.class.getDeclaredField("f" /*textureNameToImageMap*/);
						f1.setAccessible(true);
						
						IntHashMap textureImageMap = (IntHashMap) f1.get(mc.renderEngine);
						textureImageMap.removeObject(imageData.textureName);
					}
					
					urlImageMap.remove(mc.thePlayer.skinUrl);
				}
			}
		}
	}
}
