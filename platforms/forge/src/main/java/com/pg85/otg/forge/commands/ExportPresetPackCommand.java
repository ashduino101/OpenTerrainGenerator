package com.pg85.otg.forge.commands;

import java.io.*;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.pg85.otg.OTG;
import com.pg85.otg.config.biome.BiomeConfig;
import com.pg85.otg.config.io.FileSettingsPacker;
import com.pg85.otg.config.io.NameTable;
import com.pg85.otg.config.io.SettingsMap;
import com.pg85.otg.forge.gen.OTGNoiseChunkGenerator;
import com.pg85.otg.interfaces.IBiomeConfig;
import com.pg85.otg.presets.Preset;
import com.pg85.otg.presets.PresetPacker;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;

public class ExportPresetPackCommand extends BaseCommand
{
    private static boolean isRunning = false;

    public ExportPresetPackCommand()
    {
        super("pack");
        this.helpMessage = "Exports entire preset as a package ready for distribution.";
        this.usage = "/otg pack";
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder)
    {
        builder.then(Commands.literal("exportpreset")
                .executes(context -> exportPreset(context.getSource()))
        );
    }

    private int exportPreset(CommandSource source)
    {
        if (!(source.getLevel().getChunkSource().generator instanceof OTGNoiseChunkGenerator))
        {
            source.sendSuccess(new StringTextComponent("OTG is not enabled in this world"), false);
            return 0;
        }

        Preset preset = ((OTGNoiseChunkGenerator)source.getLevel().getChunkSource().generator).getPreset();
        if (!isRunning)
        {
            isRunning = true;
            source.sendSuccess(new StringTextComponent("Packing preset for distribution, this might take a while..."), false);
            source.sendSuccess(new StringTextComponent("Run this command again to see progress."), false);
            new Thread(() -> {
                String outputPath = OTG.getEngine().getPresetsDirectory() + "/" + preset.getFolderName() + ".preset";
                try (FileOutputStream file = new FileOutputStream(outputPath))
                {
                    OTG.getEngine().getLogger().log(LogLevel.INFO, LogCategory.MAIN, String.format("Packing preset to %s", outputPath));

                    PresetPacker.packToFile(preset, file, OTG.getEngine().getLogger());

                    OTG.getEngine().getLogger().log(LogLevel.INFO, LogCategory.MAIN, "Preset export complete.");
                    source.sendSuccess(new StringTextComponent("OTG preset export is done."), false);
                    isRunning = false;
                }
                catch (IOException e)
                {
                    source.sendFailure(new StringTextComponent("Failed to pack preset! Check logs for details."));
                    OTG.getEngine().getLogger().log(LogLevel.WARN, LogCategory.MAIN, e.toString());
                }
            }).start();
        } else {
            source.sendSuccess(new StringTextComponent("OTG preset export is running."), false);
        }
        return 0;
    }
}
