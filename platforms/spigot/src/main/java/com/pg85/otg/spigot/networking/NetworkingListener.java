package com.pg85.otg.spigot.networking;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import com.pg85.otg.OTG;
import com.pg85.otg.constants.Constants;
import com.pg85.otg.interfaces.IBiomeConfig;
import com.pg85.otg.presets.Preset;
import com.pg85.otg.spigot.OTGPlugin;
import com.pg85.otg.spigot.gen.OTGNoiseChunkGenerator;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;

import io.netty.buffer.Unpooled;
import net.minecraft.server.v1_16_R3.PacketDataSerializer;
import net.minecraft.server.v1_16_R3.WorldServer;

public class NetworkingListener implements Listener
{
	private final OTGPlugin plugin;

	public NetworkingListener(OTGPlugin plugin)
	{
		this.plugin = plugin;
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event)
	{
		sendDataPacket(event.getPlayer());
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent event)
	{
		sendDataPacket(event.getPlayer());
	}

	private void sendDataPacket(Player player)
	{
		WorldServer world = ((CraftWorld) player.getWorld()).getHandle();

		if (!(world.getChunkProvider().getChunkGenerator() instanceof OTGNoiseChunkGenerator))
		{
			return;
		}

		Preset preset = ((OTGNoiseChunkGenerator) world.getChunkProvider().getChunkGenerator()).getPreset();

		PacketDataSerializer buffer = new PacketDataSerializer(Unpooled.buffer(32766));
		String presetName = preset.getFolderName().toLowerCase();
		
		buffer.writeByte((byte) 1);
		buffer.writeInt(preset.getAllBiomeConfigs().size());
		buffer.a(presetName);

		for (IBiomeConfig biome : preset.getAllBiomeConfigs())
		{
			String key = biome.getRegistryKey().toResourceLocationString();
			BiomeSettingSyncWrapper wrapper = OTGClientSyncManager.getSyncedData().get(key);

			if (wrapper != null)
			{
				buffer.a(key.replace(Constants.MOD_ID_SHORT + ":" + presetName + ".", ""));
				wrapper.encode(buffer);
			}
		}

		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			OTG.getEngine().getLogger().log(LogLevel.INFO, LogCategory.MAIN,
					"Sending sync data of size " + buffer.writerIndex() + " bytes to " + player.getName() + ".");
			player.sendPluginMessage(plugin, Constants.MOD_ID_SHORT + ":spigot", buffer.array());
		}, 100); // 100 ticks is arbitrary, but 20 wasn't long enough
		// TODO is there a different event we can use to send this, where we don't need a delay
	}
}
