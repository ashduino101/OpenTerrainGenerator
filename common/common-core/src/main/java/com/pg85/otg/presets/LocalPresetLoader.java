package com.pg85.otg.presets;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import com.pg85.otg.OTG;
import com.pg85.otg.config.biome.BiomeConfig;
import com.pg85.otg.config.biome.BiomeConfigFinder;
import com.pg85.otg.config.io.FileSettingsReader;
import com.pg85.otg.config.io.FileSettingsWriter;
import com.pg85.otg.config.io.IConfigFunctionProvider;
import com.pg85.otg.config.io.SettingsMap;
import com.pg85.otg.config.preset.PresetConfig;
import com.pg85.otg.constants.Constants;
import com.pg85.otg.gen.biome.layers.BiomeLayerData;
import com.pg85.otg.interfaces.IBiome;
import com.pg85.otg.interfaces.ILogger;
import com.pg85.otg.interfaces.IMaterialReader;
import com.pg85.otg.util.OTGLog;
import com.pg85.otg.util.OTGMaterialReader;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;

/**
 * A base class for a platform-specific preset loader, which loads 
 * all presets from disk when the OTG Engine is started at app start, 
 * and registers all biomes with their worldconfig/biomeconfig settings.
 */
public abstract class LocalPresetLoader
{	
	private static final int MAX_INHERITANCE_DEPTH = 15;
	protected final Object materialReaderLock = new Object();
	protected final File presetsDir;
	protected final HashMap<String, Preset> presets = new HashMap<>();
	protected final HashMap<String, String> aliasMap = new HashMap<>();

	public LocalPresetLoader(Path otgRootFolder)
	{
		this.presetsDir = Paths.get(otgRootFolder.toString(), File.separator + Constants.PRESETS_FOLDER).toFile();
	}

	public IMaterialReader getMaterialReader()
	{
		return OTGMaterialReader.get();
	}


	public Preset getPresetByShortNameOrFolderName(String name)
	{
		// Example: preset is stored as "Biome Bundle v7", but also accepts "Biome Bundle"
		if (aliasMap.containsKey(name))
		{
			return this.presets.get(aliasMap.get(name));
		}
		return this.presets.get(name);
	}
	
	public Preset getPresetByFolderName(String name)
	{
		return this.presets.get(name);
	}

	public ArrayList<Preset> getAllPresets()
	{
		return new ArrayList<Preset>(presets.values());
	}

	public Set<String> getAllPresetFolderNames()
	{
		return presets.keySet();
	}
	
	public String getDefaultPresetFolderName()
	{
		return this.presets.keySet().isEmpty() ? Constants.DEFAULT_PRESET_NAME
				: this.presets.containsKey(Constants.DEFAULT_PRESET_NAME)
						? Constants.DEFAULT_PRESET_NAME
						: (String) this.presets.keySet().toArray()[0];
	}
		
	public void loadPresetsFromDisk()
	{
		if(this.presetsDir.exists() && this.presetsDir.isDirectory())
		{
			OTGLog.getLogger().log(
				LogLevel.INFO,
				LogCategory.CONFIGS,
				"Loading presets from " + this.presetsDir
			);
			for(File presetDir : Objects.requireNonNull(this.presetsDir.listFiles()))
			{
				if(presetDir.isDirectory())
				{
					for(File file : Objects.requireNonNull(presetDir.listFiles()))
					{
						if(file.getName().equals(Constants.PRESET_CONFIG_FILE) || file.getName().equals(Constants.LEGACY_WORLD_CONFIG_FILE))
						{
							Preset preset = loadPreset(presetDir.toPath());
							this.presets.put(preset.getFolderName(), preset);
							this.aliasMap.put(preset.getPresetRegistryName(), preset.getFolderName());
							break;
						}
					}
				}
			}
		} else {
			OTGLog.getLogger().log(
				LogLevel.INFO,
				LogCategory.CONFIGS,
				"No presets found in " + this.presetsDir
			);
		}
	}
	
	protected Preset loadPreset(Path presetDir)
	{
		File presetConfigFile = new File(presetDir.toString(), Constants.PRESET_CONFIG_FILE);
		if (!presetConfigFile.exists()) {
			presetConfigFile = new File(presetDir.toString(), Constants.LEGACY_WORLD_CONFIG_FILE);
		}
		File biomesDirectory = new File(presetDir.toString(), Constants.BIOMES_FOLDER);
		if(!biomesDirectory.exists())
		{
			biomesDirectory = new File(presetDir.toString(), Constants.LEGACY_WORLD_BIOMES_FOLDER);
		}
		String presetFolderName = presetDir.toFile().getName();
		
		SettingsMap presetConfigSettings = FileSettingsReader.read(presetFolderName, presetConfigFile);
		PresetConfig presetConfig = new PresetConfig(
				presetDir,
				presetConfigSettings,
				addBiomesFromDirRecursive(biomesDirectory),
				OTGMaterialReader.get()
		);
		FileSettingsWriter.writeToFile(presetConfig.getSettingsAsMap(), presetConfigFile, presetConfig.getPresetInfo().getSettingsMode());

		// use shortPresetName to register the biomes, instead of presetName
		ArrayList<BiomeConfig> biomeConfigs = loadBiomeConfigs(presetDir, biomesDirectory.toPath(), presetConfig, OTG.getEngine().getBiomeResourceManager());
		return new Preset(presetDir, presetConfig.getPresetInfo().getRegistryName(), presetConfig, biomeConfigs);
	}
	
	private ArrayList<String> addBiomesFromDirRecursive(File biomesDirectory)
	{
		ArrayList<String> biomes = new ArrayList<String>();
		if(biomesDirectory.exists())
		{
			for(File biomeConfig : biomesDirectory.listFiles())
			{
				if(biomeConfig.isFile() && biomeConfig.getName().endsWith(Constants.BiomeConfigFileExtension))
				{
					biomes.add(biomeConfig.getName().replace(Constants.BiomeConfigFileExtension, ""));
				}
				else if(biomeConfig.isDirectory())
				{
					biomes.addAll(addBiomesFromDirRecursive(biomeConfig));
				}
			}
		}
		return biomes;
	}

	private ArrayList<BiomeConfig> loadBiomeConfigs(Path presetDir, Path presetBiomesDir, PresetConfig presetConfig, IConfigFunctionProvider biomeResourcesManager)
	{
		// Establish folders
		List<Path> biomeDirs = new ArrayList<Path>(2);
		biomeDirs.add(presetBiomesDir);
		
		// Load all files
		BiomeConfigFinder biomeConfigFinder = new BiomeConfigFinder();
		Map<String, SettingsMap> biomeConfigStubs = biomeConfigFinder.findBiomes(biomeDirs);

		// Read all settings
		ArrayList<BiomeConfig> biomeConfigs = readAndWriteSettings(presetConfig, biomeConfigStubs, biomeResourcesManager);

		// Update settings dynamically, these changes don't get written back to the file
		processSettings(presetConfig, biomeConfigs);

		ILogger logger = OTGLog.getLogger();
		if(logger.getLogCategoryEnabled(LogCategory.CONFIGS) && logger.canLogForPreset(presetDir.getFileName().toString()))
		{
			logger.log(
				LogLevel.INFO,
				LogCategory.CONFIGS,
				MessageFormat.format(
					"{0} biomes Loaded", 
					biomeConfigs.size()
				)
			);
			logger.log(
				LogLevel.INFO, 
				LogCategory.CONFIGS,
				biomeConfigs.stream().map(
					item -> item.getIdentitySettings().getBiomeName()
				).collect(
					Collectors.joining(", ")
				)
			);
		}
		return biomeConfigs;
	}

	private ArrayList<BiomeConfig> readAndWriteSettings(PresetConfig presetConfig, Map<String, SettingsMap> biomeSettingsMaps, IConfigFunctionProvider biomeResourcesManager)
	{
		ArrayList<BiomeConfig> biomeConfigs = new ArrayList<BiomeConfig>();

		for (SettingsMap settingsMap : biomeSettingsMaps.values())
		{
			// Settings reading
			BiomeConfig biomeConfig = new BiomeConfig(settingsMap, presetConfig, biomeResourcesManager);
			//BiomeConfig biomeConfig = new BiomeConfig(settingsMap.getBiomeName(), settingsMap, presetDir, settingsMap.getSettings(), presetConfig, presetShortName, presetMajorVersion, biomeResourcesManager, logger, materialReader);
			biomeConfigs.add(biomeConfig);

			// Settings writing
            Path writeFile = settingsMap.getPath();
            FileSettingsWriter.writeToFile(biomeConfig.getSettingsAsMap(), writeFile.toFile(), presetConfig.getPresetInfo().getSettingsMode());
        }

		return biomeConfigs;
	}

	private void processSettings(PresetConfig presetConfig, ArrayList<BiomeConfig> biomeConfigs)
	{
		for(BiomeConfig biomeConfig : biomeConfigs)
		{
			// Index ReplacedBlocks
			if (!presetConfig.isBiomeConfigsHaveReplacement())
			{
				presetConfig.setBiomeConfigsHaveReplacement(biomeConfig.getSurfaceSettings().getReplacedBlocks().hasReplaceSettings());
			}

			// Index maxSmoothRadius
			if (presetConfig.getMaxSmoothRadius() < biomeConfig.getTerrainSettings().getSmoothRadius())
			{
				presetConfig.setMaxSmoothRadius(biomeConfig.getTerrainSettings().getSmoothRadius());
			}
			if (presetConfig.getMaxSmoothRadius() < biomeConfig.getTerrainSettings().getCHCSmoothRadius())
			{
				presetConfig.setMaxSmoothRadius(biomeConfig.getTerrainSettings().getCHCSmoothRadius());
			}
		}
	}
	
	public abstract IBiome[] getGlobalIdMapping(String presetFolderName);

	public abstract Map<String, BiomeLayerData> getPresetGenerationData();


}
