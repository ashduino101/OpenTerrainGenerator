package com.pg85.otg.spigot.commands;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import com.pg85.otg.OTG;
import com.pg85.otg.interfaces.IPreset;
import com.pg85.otg.presets.PresetFolder;
import com.pg85.otg.presets.PresetPacker;
import com.pg85.otg.spigot.gen.OTGNoiseChunkGenerator;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;

import net.minecraft.server.v1_16_R3.WorldServer;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.entity.Player;

public class ExportPresetPackCommand extends BaseCommand
{
    private static boolean isRunning = false;


    public ExportPresetPackCommand() {
        super("pack");
        this.helpMessage = "Exports entire preset as a package ready for distribution.";
        this.usage = "/otg pack";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args)
    {
        return new ArrayList<>();
    }

    @Override
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
        if (!(preset instanceof PresetFolder))
        {
            sender.sendMessage("Only unpacked presets can be packed.");
            return true;
        }

        if (!isRunning)
        {
            isRunning = true;
            sender.sendMessage("Packing preset for distribution, this might take a while...");
            sender.sendMessage("Run this command again to see progress.");
            new Thread(() -> {
                String outputPath = OTG.getEngine().getPresetsDirectory() + "/" + preset.getId() + ".preset";
                try (FileOutputStream file = new FileOutputStream(outputPath))
                {
                    OTG.getEngine().getLogger().log(LogLevel.INFO, LogCategory.MAIN, String.format("Packing preset to %s", outputPath));

                    PresetPacker.packToFile((PresetFolder) preset, file, OTG.getEngine().getLogger());

                    OTG.getEngine().getLogger().log(LogLevel.INFO, LogCategory.MAIN, "Preset export complete.");
                    sender.sendMessage("OTG preset export is done.");
                    isRunning = false;
                }
                catch (IOException e)
                {
                    sender.sendMessage("Failed to pack preset! Check logs for details.");
                    OTG.getEngine().getLogger().log(LogLevel.WARN, LogCategory.MAIN, e.toString());
                }
            }).start();
        } else {
            sender.sendMessage("OTG preset export is running.");
        }
        return true;
    }
}
