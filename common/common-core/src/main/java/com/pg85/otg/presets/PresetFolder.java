package com.pg85.otg.presets;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

import com.pg85.otg.config.biome.BiomeConfig;
import com.pg85.otg.config.map.FileMapImageProvider;
import com.pg85.otg.config.world.WorldConfig;
import com.pg85.otg.interfaces.*;

/**
 * Represents an OTG preset, with all its world and biome configs, stored in /config/OpenTerrainGenerator/Presets/\<PresetName\>/.
 */
public class PresetFolder implements IPreset {
	private final Path presetFolder;
	private String presetFolderName;
	private String shortPresetName;
	
	// Note: Since we're not using Supplier<>, we need to be careful about any classes fetching 
	// and caching our worldconfig/biomeconfigs etc, or they won't update when reloaded from disk.
	// BiomeGen and ChunkGen cache some settings during a session, so they'll only update on world exit/rejoin.
	private WorldConfig worldConfig;
	private HashMap<String, IBiomeConfig> biomeConfigs = new HashMap<String, IBiomeConfig>();
	private int majorVersion;
	
	public PresetFolder(Path presetFolder, String shortPresetName, WorldConfig worldConfig, ArrayList<BiomeConfig> biomeConfigs)
	{
		this.presetFolder = presetFolder;
		this.presetFolderName = presetFolder.toFile().getName();
		this.shortPresetName = shortPresetName;
		this.worldConfig = worldConfig;
		this.majorVersion = worldConfig.getMajorVersion();

		for(BiomeConfig biomeConfig : biomeConfigs)
		{
			this.biomeConfigs.put(biomeConfig.getName(), biomeConfig);
		}		
	}

	public void update(IPreset preset)
	{
		if (!(preset instanceof PresetFolder)) {
			throw new UnsupportedOperationException("Can only update PresetFolder with another PresetFolder");
		}
		PresetFolder folder = (PresetFolder) preset;
		this.worldConfig = folder.worldConfig;
		this.biomeConfigs = folder.biomeConfigs;
		this.majorVersion = folder.majorVersion;
	}

	public Path getPresetFolder()
	{
		return this.presetFolder;
	}

	@Override
	public String getId()
	{
		return this.presetFolderName;
	}

	@Override
	public String getShortPresetName()
	{
		return this.shortPresetName;
	}

	@Override
	public IWorldConfig getWorldConfig()
	{
		return this.worldConfig;
	}
	
	@Override
	public IBiomeConfig getBiomeConfig(String biomeName)
	{
		return this.biomeConfigs.get(biomeName);
	}

	@Override
	public IMapImageProvider getMapImageSource()
	{
		return new FileMapImageProvider(this.getPresetFolder(), this.getWorldConfig().getImageFile());
	}

	@Override
	public ArrayList<IBiomeConfig> getAllBiomeConfigs()
	{
		return new ArrayList<>(this.biomeConfigs.values());
	}
	
	@Override
	public ArrayList<String> getAllBiomeNames()
	{
		return new ArrayList<>(this.biomeConfigs.keySet());
		
	}

	@Override
	public ICustomObject getCustomObject(String name) {
		throw new UnsupportedOperationException("Getting an object directly from a preset folder is not yet supported!");
	}

	@Override
	public int getMajorVersion()
	{
		return this.majorVersion;
	}

	@Override
	public boolean isPacked() {
		return false;
	}
}
