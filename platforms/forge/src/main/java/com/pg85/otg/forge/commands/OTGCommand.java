package com.pg85.otg.forge.commands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.pg85.otg.forge.commands.arguments.FlagsArgument;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.ArgumentSerializer;
import net.minecraft.command.arguments.ArgumentTypes;

public class OTGCommand
{
	private static final List<BaseCommand> commands = new ArrayList<>();
	
	public static void register(CommandDispatcher<CommandSource> dispatcher)
	{
		HelpCommand helpCommand = new HelpCommand();
		
		commands.clear();
		commands.add(helpCommand);
		commands.add(new MapCommand());
		commands.add(new DataCommand());
		commands.add(new PresetCommand());
		commands.add(new FlushCommand());
		commands.add(new StructureCommand());
		commands.add(new BiomeCommand());
		commands.add(new TpCommand());
		commands.add(new SpawnCommand());
		commands.add(new EditCommand());
		commands.add(new ExportCommand());
		commands.add(new RegionCommand());
		commands.add(new UpdateCommand());
		commands.add(new ExportBO4DataCommand());
		commands.add(new ExportPresetPackCommand());

		commands.sort(Comparator.comparing(BaseCommand::getName));

		LiteralArgumentBuilder<CommandSource> commandBuilder = Commands.literal("otg").requires(
				(context) -> context.hasPermission(2)
			).executes(
					context -> helpCommand.showHelp(context.getSource(), "")
			);
		
		for (BaseCommand command : commands) {
			command.build(commandBuilder);
		}
		
		dispatcher.register(commandBuilder);
	}
	
	public static void registerArguments() {
		ArgumentTypes.register("flags", FlagsArgument.class, new ArgumentSerializer<>(FlagsArgument::create));
	}
	
	public static List<BaseCommand> getCommands() {
		return commands;
	}
}
