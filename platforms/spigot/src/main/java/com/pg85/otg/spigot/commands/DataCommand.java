package com.pg85.otg.spigot.commands;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.util.StringUtil;

import com.pg85.otg.OTG;

import net.minecraft.server.v1_16_R3.IRegistry;
import net.minecraft.server.v1_16_R3.MinecraftKey;
import net.minecraft.server.v1_16_R3.RegistryGeneration;

public class DataCommand extends BaseCommand
{
	private static final List<String> DATA_TYPES = new ArrayList<>(Arrays.asList(
			"biome",
			"block",
			"entity",
			"sound",
			"particle",
			"configured_feature"
	));
	
	public DataCommand() {
		super("data");
		this.helpMessage = "Dumps various types of game data to files for preset development.";
		this.usage = "/otg data <type>";
		this.detailedHelp = new String[] { 
				"<type>: The type of data to dump.",
				" - biome: All registered biomes.",
				" - block: All registered blocks.",
				" - entity: All registered entities.",
				" - sound: All registered sounds.",
				" - particle: All registered particles.",
				" - dimension: All registered dimensions.",
				" - configured_feature: All registered configured features (Used to decorate biomes during worldgen)."
			};
	}

	public boolean execute(CommandSender sender, String[] args)
	{
		// /otg data music
		// /otg data sound
		if (args.length != 1)
		{
			sender.sendMessage(getUsage());
			sender.sendMessage("Data types: "+String.join(", ", DATA_TYPES));
			return true;
		}

		IRegistry<?> registry;

		switch (args[0].toLowerCase())
		{
			case "biome":
				registry = ((CraftServer) Bukkit.getServer()).getServer().customRegistry.b(IRegistry.ay);
				break;
			case "block":
				registry = IRegistry.BLOCK;
				break;
			case "entity":
				registry = IRegistry.ENTITY_TYPE;
				break;
			case "sound":
				registry = IRegistry.SOUND_EVENT;
				break;
			case "particle":
				registry = IRegistry.PARTICLE_TYPE;
				break;
			case "configured_feature":
				registry = RegistryGeneration.e;
				break;
			default:
				sender.sendMessage("Data types: "+String.join(", ", DATA_TYPES));
				return true;
		}
		Set<MinecraftKey> set = registry.keySet();
		new Thread(() -> {
			try
			{
				Path root = OTG.getEngine().getOTGRootFolder();
				File folder = new File(root.toString() + File.separator + "output");
				if (!folder.exists())
				{
					folder.mkdirs();
				}

				String fileName = "data-output-" + args[0] + ".txt".toLowerCase();
				File output = new File(folder, fileName);
				FileWriter writer = new FileWriter(output);
				for (MinecraftKey key : set)
				{
					writer.write(key.toString() + "\n");
				}
				writer.close();
				sender.sendMessage("File exported as " + output.getPath());
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}).start();
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String[] args)
	{
		return StringUtil.copyPartialMatches(args[1], DATA_TYPES, new ArrayList<>());
	}
}
