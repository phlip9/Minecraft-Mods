package net.minecraft.src;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;

public class mod_SpeedMiner extends BaseMod {
	
	private int mToggleKey;
	private long mLastKeyPress;
	private ScaledResolution mScaledResolution;
	private Minecraft mc;
	private Properties mProperties;
	private boolean mFading = false;
	private long mFadeStart;
	private int mPrevAlpha;
	
	private static boolean speedMining = false;
	private static File configFile;
	
	private static final int TEXT_Y_OFFSET = 1;
	private static final long FADE_TIME = 2000L;
	
	@Override
	public String getVersion() {
		return "1.3";
	}

	@Override
	public void load() {
		mc = ModLoader.getMinecraftInstance();
		mLastKeyPress = System.currentTimeMillis();
		mScaledResolution = new ScaledResolution(mc.gameSettings, mc.displayWidth, mc.displayHeight);
		
		configFile = new File(Minecraft.getMinecraftDir(), "mods/SpeedMiner.config");
		
		try {
			loadConfig();
		} catch (FileNotFoundException e) {
			System.out.println("[SpeedMiner] Config file not found!\n" + e.getMessage());
		} catch (IOException e) {
			System.out.println("[SpeedMiner] Could not save config file, not enough permissions?\n" + e.getMessage());
		}
		
		ModLoader.setInGameHook(this, true, false);
		
		System.out.println();
	}
	
	private void loadConfig() throws FileNotFoundException, IOException {
		
		mProperties = new Properties();
		
		if(configFile.exists()){
			mProperties.load(new FileInputStream(configFile));
			
			mToggleKey = Keyboard.getKeyIndex(mProperties.getProperty("ToggleKey", "L"));
		} else {
			configFile.createNewFile();
			mProperties.load(new FileInputStream(configFile));
			
			mProperties.setProperty("ToggleKey", "O");
			mToggleKey = Keyboard.getKeyIndex("O");
			
			mProperties.store(new FileOutputStream(configFile), "SpeedMiner Config File - by phlip9");
		}
	}
	
	@Override
	public boolean onTickInGame(float f, final Minecraft mc) {
		if(mc.theWorld != null && mc.thePlayer != null && !mc.gameSettings.showDebugInfo && mc.playerController.shouldDrawHUD() && mc.currentScreen == null){
			if(Keyboard.isKeyDown(mToggleKey) && System.currentTimeMillis() - mLastKeyPress > 200L){
				mLastKeyPress = System.currentTimeMillis();
				speedMining = !speedMining;
				
				mScaledResolution = new ScaledResolution(mc.gameSettings, mc.displayWidth, mc.displayHeight);
				
				mFadeStart = System.currentTimeMillis();
				mFading = true;
				mPrevAlpha = 0xFF;
				
				System.out.println("SpeedMiner " + ((speedMining) ? "Enabled" : "Disabled"));
			}

			if(mFading){
				long deltaTime = System.currentTimeMillis() - mFadeStart;
				
				if(deltaTime < FADE_TIME){
					String toDisplay = new StringBuilder().append("Speed Mining ").append((speedMining) ? "Enabled" : "Disabled").toString();
					
					int alpha = getDeltaAlpha(deltaTime);
					
					if(alpha < 10 && alpha >= 0) {
						alpha = 10;
					}
					
					int greyscale = ((alpha & 0xFF) << 24) + (0xFF << 16) + (0xFF << 8) + 0xFF;
					
					mc.fontRenderer.drawString(
							toDisplay, 
							((mScaledResolution.getScaledWidth() / 2) - mc.fontRenderer.getStringWidth(toDisplay) / 2),
							TEXT_Y_OFFSET, 
							greyscale);

				} else {
					mFading = false;
				}
			}
		}
		
		return true;
	}
	
	public static boolean isSpeedMining() {
		return speedMining;
	}
	
	private int getDeltaAlpha(long curDeltaTime) {
		return (int)((FADE_TIME - curDeltaTime) * 0xFF / FADE_TIME);
		
		/*if(alpha < 0) {
			alpha = ~alpha + 1;
		}
		
		if(alpha >= prevAlpha) {
			alpha = prevAlpha;
		}
		
		return (((alpha & 0xFF) << 24) + 0xFFFFFF);*/
	}
}
