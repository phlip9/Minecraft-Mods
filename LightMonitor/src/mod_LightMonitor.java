package net.minecraft.src;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.lang.reflect.Array;
import java.lang.reflect.Method;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;

public class mod_LightMonitor extends BaseMod {

	private World worldObj;
	private Minecraft mc;
	private long lastKeyPress;
	private FontRenderer fontRenderer;
	private ScaledResolution scaledResolution;
	private int toggleKey;
	private int tick = 0;
	private boolean firstRun = true;
	private Holder holder = new Holder();

	private static File configFile;
	private static Properties props;
	
	private static final int[] BLOCK_BLACKLIST = {6, 30, 31, 37, 38, 39 , 40, 50, 55, 59, 63, 75, 76};
	private static final int[] MOB_BLACKLIST = {8, 9, 10, 11, 20, 27, 28, 44, 53, 66, 70, 72};
	
	private static final int PANEL_BASE_X = 2;
	private static final int PANEL_BASE_Y = 2;
	private static final int TEXT_BASE_Y = PANEL_BASE_Y + 2;
	private static final int TEXT_BASE_X = PANEL_BASE_X + 2;
	
	private static final int RED = 0xff0000;
	private static final int GREEN = 0x00ff00;
	
	private static final int UPDATE_ON_TICK = 7;

	@Override
	public void load() {
		mc = ModLoader.getMinecraftInstance();
		fontRenderer = mc.fontRenderer;
		scaledResolution = new ScaledResolution(mc.gameSettings, mc.displayWidth, mc.displayHeight);
		
		lastKeyPress = System.currentTimeMillis();
		
		configFile = new File(Minecraft.getMinecraftDir(), "mods/LightMonitor.config");
		loadConfig();
		
		ModLoader.setInGameHook(this, true, false);
	}
	
	private void loadConfig(){
		props = new Properties();
		
		if(configFile.exists()){
			try {
				props.load(new FileInputStream(configFile));
			} catch (FileNotFoundException e) {
				System.out.println("[LightMonitor] Config file not found!\n" + e.getMessage());
			} catch (IOException e) {
				System.out.println("[LightMonitor] Could not load config file!\n" + e.getMessage());
			}
			
			holder.viewState = Integer.parseInt(props.getProperty("ViewState", Integer.toString(1)));
			toggleKey = Keyboard.getKeyIndex(props.getProperty("ToggleKey", "L"));
			firstRun = Boolean.parseBoolean(props.getProperty("firstRun", "false"));
			holder.shadow = Boolean.parseBoolean(props.getProperty("DrawTextWithShadow", "true"));
			holder.background = Boolean.parseBoolean(props.getProperty("ShowBackground", "true"));
		} else {
			try {
				configFile.createNewFile();
				props.load(new FileInputStream(configFile));
			} catch (IOException e) {
				System.out.println("[LightMonitor] Could not create config file!\n" + 
						e.getMessage());
			}
			
			props.setProperty("ViewState", Integer.toString(1));
			props.setProperty("ToggleKey", "L");
			props.setProperty("firstRun", "false");
			props.setProperty("DrawTextWithShadow", "true");
			props.setProperty("ShowBackground", "true");

			holder.viewState = 1;
			toggleKey = Keyboard.getKeyIndex("L");
			firstRun = true;
			holder.shadow = true;
			holder.background = true;
			
			try {
				props.store(new FileOutputStream(configFile), "LightMonitor Config File\nDraw Text With Shadow: Toggles whether the display text draws with shadow\nToggle Key: Toggles the LightMonitor display");
			} catch (FileNotFoundException e) {
				System.out.println("[LightMonitor] Config file not found!\n" + e.getMessage());
			} catch (IOException e) {
				System.out.println("[LightMonitor] Could not load config file!\n" + e.getMessage());
			}
		}
	}		

	private int getFirstUncoveredBlock(int x, int y, int z){
		while(mc.theWorld.getBlockId(x, y, z) == 0 && y >= 0){
			y--;
		}
		return y + 1;
	}

	private void drawString(String s, int x, int y, int color){
		if(holder.shadow){
			fontRenderer.drawStringWithShadow(s, x, y, color);
		} else {
			fontRenderer.drawString(s, x, y, color);
		}
	}
	
	private boolean inBlacklist(int blockId, int[] blacklist){
		for(int i = 0; i < blacklist.length; i++) {
			if(blacklist[i] == blockId) {
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public boolean onTickInGame(float f, final Minecraft mc) {
		tick++;
		if(tick > UPDATE_ON_TICK){
			tick = 0;
		}
		
		if(mc.theWorld != null && mc.thePlayer != null){
			if(Keyboard.isKeyDown(toggleKey) && System.currentTimeMillis() - lastKeyPress > 200L){
				lastKeyPress = System.currentTimeMillis();
				holder.viewState = (holder.viewState == 0) ? 1 : (holder.viewState == 1) ? 2 : 0;
			}

			if(holder.viewState != 0 && !mc.gameSettings.showDebugInfo && mc.playerController.shouldDrawHUD() && mc.currentScreen == null){
				drawHud();
				
				if(tick == 0){
					updateHolder();
				}
			}
		}
		return true;
	}

	private void drawHud() {
		int height = scaledResolution.getScaledHeight();
		int width = scaledResolution.getScaledWidth();
		
		if (holder.viewState == 1) {
			if (holder.background) {
				drawRect(PANEL_BASE_X - 2, PANEL_BASE_Y - 2, 130 + 2, TEXT_BASE_Y + 34 + 2, 0xff1c1c1c);
				drawRect(PANEL_BASE_X, PANEL_BASE_Y, 130, TEXT_BASE_Y + 34, 0xff464646);
			}
			if (holder.slimes) {
				drawString("Slime Chunk", TEXT_BASE_X + 68, TEXT_BASE_Y + 24, GREEN);
			} else {
				drawString("Slime Chunk", TEXT_BASE_X + 68, TEXT_BASE_Y + 24, RED);
			}
			if (holder.mobs) {
				drawString("Mobs", TEXT_BASE_X, TEXT_BASE_Y + 24, GREEN);
			} else {
				drawString("Mobs", TEXT_BASE_X, TEXT_BASE_Y + 24, RED);
			}
			if (holder.flowers) {
				drawString("Flowers", TEXT_BASE_X + 68, TEXT_BASE_Y + 8, GREEN);
			} else {
				drawString("Flowers", TEXT_BASE_X + 68, TEXT_BASE_Y + 8, RED);
			}
			if (holder.crops) {
				drawString("Crops", TEXT_BASE_X, TEXT_BASE_Y + 8, GREEN);
			} else {
				drawString("Crops", TEXT_BASE_X, TEXT_BASE_Y + 8, RED);
			}
			if (holder.trees) {
				drawString("Trees", TEXT_BASE_X + 32, TEXT_BASE_Y + 8, GREEN);
			} else {
				drawString("Trees", TEXT_BASE_X + 32, TEXT_BASE_Y + 8, RED);
			}
			if (holder.animals) {
				drawString("Animals", TEXT_BASE_X + 32, TEXT_BASE_Y + 16, GREEN);
			} else {
				drawString("Animals", TEXT_BASE_X + 32, TEXT_BASE_Y + 16, RED);
			}
			if (holder.grass) {
				drawString("Grass", TEXT_BASE_X, TEXT_BASE_Y + 16, GREEN);
			} else {
				drawString("Grass", TEXT_BASE_X, TEXT_BASE_Y + 16, RED);
			}
			if (holder.mushrooms) {
				drawString("Mushrooms", TEXT_BASE_X + 68, TEXT_BASE_Y + 16, GREEN);
			} else {
				drawString("Mushrooms", TEXT_BASE_X + 68, TEXT_BASE_Y + 16, RED);
			}
		} else {
			if(holder.background){
				drawRect(PANEL_BASE_X - 2, PANEL_BASE_Y - 2, 48 + 2, TEXT_BASE_Y + 9 + 2, 0xff1c1c1c);
				drawRect(PANEL_BASE_X, PANEL_BASE_Y, 48, TEXT_BASE_Y + 9, 0xff464646);
			}
		}
		
		drawString((new StringBuilder().append("Light: ").append(holder.light).toString()), TEXT_BASE_X, TEXT_BASE_Y, 0xffffff);
	}
	
	private void updateHolder() {
		int playerX = MathHelper.floor_double(mc.thePlayer.posX);
		int playerZ = MathHelper.floor_double(mc.thePlayer.posZ);
		int blockY = getFirstUncoveredBlock(playerX, MathHelper.floor_double(mc.thePlayer.boundingBox.minY), playerZ);

		int blockBelow = mc.theWorld.getBlockId(playerX, blockY - 1, playerZ);
		
		if(inBlacklist(blockBelow, BLOCK_BLACKLIST)) {
			blockBelow = mc.theWorld.getBlockId(playerX, blockY - 2, playerZ);
		}
		
		holder.light = mc.theWorld.getBlockLightValue(playerX, blockY, playerZ);		
		
		scaledResolution = new ScaledResolution(mc.gameSettings, mc.displayWidth, mc.displayHeight);
		
		holder.slimes = (mc.theWorld.getChunkFromBlockCoords(playerX, playerZ).getRandomWithSeed(0x3ad8025fL).nextInt(10) == 0);
		holder.mobs = (holder.light <=7 && !inBlacklist(blockBelow, MOB_BLACKLIST));
		
		holder.flowers = (holder.light >= 8 && (blockBelow == 2 || blockBelow == 3));
		
		if(holder.light >= 9){
			holder.crops = (blockBelow == 2 || blockBelow == 3 || blockBelow == 60);
			holder.trees = (blockBelow == 2 || blockBelow == 3);
			holder.animals = (blockBelow == 2);
		}
		
		holder.grass = (holder.light >= 4 && (blockBelow == 2 || blockBelow == 3));
		holder.mushrooms = (holder.light <= 12 && !(blockBelow == 8 || blockBelow == 9 || blockBelow == 10 || blockBelow == 11));
	}
	
	private void drawRect(int x1, int y1, int x2, int y2, int c) {
		if (x1 < x2) {
			int i = x1;
			x1 = x2;
			x2 = i;
		}
		
		if (y1 < y2) {
			int j = y1;
			y1 = y2;
			y2 = j;
		}
		
		float a = (float)(c >> 24 & 0xff) / 255F;
		float r = (float)(c >> 16 & 0xff) / 255F;
		float g = (float)(c >> 8 & 0xff) / 255F;
		float b = (float)(c & 0xff) / 255F;
		
		Tessellator t = Tessellator.instance;
		
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glColor4f(r, g, b, a);
		
		t.startDrawingQuads();
		t.addVertex(x1, y2, 0.0D);
		t.addVertex(x2, y2, 0.0D);
		t.addVertex(x2, y1, 0.0D);
		t.addVertex(x1, y1, 0.0D);
		t.draw();
		
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	/*private void drawGradientRect(int x1, int y1, int x2, int y2, int color1, int color2) {
		float a1 = (float)(color1 >> 24 & 0xff) / 255F;
		float r1 = (float)(color1 >> 16 & 0xff) / 255F;
		float g1 = (float)(color1 >> 8 & 0xff) / 255F;
		float b1 = (float)(color1 & 0xff) / 255F;
		
		float a2 = (float)(color2 >> 24 & 0xff) / 255F;
		float r2 = (float)(color2 >> 16 & 0xff) / 255F;
		float g2 = (float)(color2 >> 8 & 0xff) / 255F;
		float b2 = (float)(color2 & 0xff) / 255F;
		
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glShadeModel(GL11.GL_SMOOTH);
		
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		tessellator.setColorRGBA_F(r1, g1, b1, a1);
		tessellator.addVertex(x2, y1, 0.0D);
		tessellator.addVertex(x1, y1, 0.0D);
		tessellator.setColorRGBA_F(r2, g2, b2, a2);
		tessellator.addVertex(x1, y2, 0.0D);
		tessellator.addVertex(x2, y2, 0.0D);
		tessellator.draw();
		
		GL11.glShadeModel(GL11.GL_FLAT);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
	}*/
	
	@Override
	public String getVersion() {
		return "1.7";
	}

	
	private class Holder {
		public int viewState = 1;
		public int light = 0;
		
		public boolean background = true;
		public boolean shadow = false;
		
		public boolean slimes = false;
		public boolean mobs = false;
		
		public boolean flowers = false;
		public boolean crops = false;
		public boolean trees = false;
		public boolean animals = false;
		public boolean grass = false;
		public boolean mushrooms = false;
	}
}
