Minecraft Mods
==============

Author:  phlip9
Repository:  https://github.com/phlip9/Minecraft-Mods


SkinThief
---------
- Steal another player's skin in-game.
- Temporarily change your skin to see how it looks.
- Permanently change your skin from a player name or image url.

Minecraft Forum post: http://www.minecraftforum.net/topic/681859-123-skinthief-steal-someones-skin-in-game/

TODO:
- Change skin from file
- UI redesign
- Notification Popup could use some anti-aliasing on the rounded rectangle curves
- Documentation


LightMonitor
------------
- In-game HUD that displays the amount of light the current block is receiving.

Minecraft Forum post: http://www.minecraftforum.net/topic/496037-123-light-monitor-in-game-light-level-display-no-item-in-game-overlay/

TODO:
- Overlay redesign
- Documentation


SpeedMiner
----------
- Break blocks 2x faster
- Reach further than normal
- Multiplayer only

Minecraft Forum post: http://www.minecraftforum.net/topic/482792-123-speedminer-break-blocks-2x-faster-multiplayer/

TODO:
- Auto tool switcher: switch from pick to shovel if the block changes from something like stone to sand.

Note:  There is a lite version that does not require ModLoader and always enables speed mining.


NoVoidFog
---------
- Disables "void fog" at the bottom of the world
- Disables the particles there too

No forum post.  Care to make one?

Note:  There is only a lite version that doesn't need any other plugins and always disables the void fog and particles.


Build Instructions:
-------------------

1. Have Java, MCP, and Eclipse properly set up. Instructions can be found here: http://mcp.ocean-labs.de/
2. Clean the MCP build environment (hint: use cleanup.bat for Windows and cleanup.sh for Mac/Linux.)
3. Decompile minecraft with decompile.(bat/sh) *Note* Make sure you have Modloader installed in the minecraft.jar you are decompiling!
4. Copy the files from the source directory of the mod you wish to build.
5. Paste these files into the src/minecraft/net/minecraft/src folder.
6. Now, open up eclipse. Set your working directory to your_mcp_directory/eclipse.
7. Edit to your hearts content! When you want to run the client, simply hit the run button in eclipse.

Cheers,
phlip9
