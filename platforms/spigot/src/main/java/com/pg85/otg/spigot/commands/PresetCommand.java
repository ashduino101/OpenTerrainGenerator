package com.pg85.otg.spigot.commands;

import java.util.Collections;
import java.util.List;

import com.pg85.otg.interfaces.IPreset;
import com.pg85.otg.presets.PackedPreset;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.entity.Player;

import com.pg85.otg.presets.PresetFolder;
import com.pg85.otg.spigot.gen.OTGNoiseChunkGenerator;

import net.minecraft.server.v1_16_R3.WorldServer;

public class PresetCommand extends BaseCommand
{
	public PresetCommand()
	{
		super("preset");
		this.helpMessage = "Displays information about the current world's preset.";
		this.usage = "/otg preset";
	}

	public boolean execute(CommandSender sender, String[] args)
	{
		if (!(sender instanceof Player))
		{
			sender.sendMessage("Only players can execute this command");
			return true;
		}
		Player player = (Player) sender;
		WorldServer world = ((CraftWorld) player.getWorld()).getHandle();

		if (!(world.getChunkProvider().getChunkGenerator() instanceof OTGNoiseChunkGenerator))
		{
			sender.sendMessage("OTG is not enabled in this world");
			return true;
		}

		IPreset preset = ((OTGNoiseChunkGenerator) world.getChunkProvider().getChunkGenerator()).getPreset();
		sender.sendMessage("Preset: " + preset.getId() +
				"\nDescription: " + preset.getWorldConfig().getDescription() +
				"\nMajor version: " + preset.getWorldConfig().getMajorVersion()
				+ "\nPacked: " + ((preset instanceof PackedPreset) ? "yes" : "no"));
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String[] args)
	{
		return Collections.emptyList();
	}
}
