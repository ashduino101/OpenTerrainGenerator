package com.pg85.otg.config.biome;

import java.nio.file.Path;
import java.util.*;

import com.pg85.otg.OTG;
import com.pg85.otg.config.ConfigFunction;
import com.pg85.otg.config.io.IConfigFunctionProvider;
import com.pg85.otg.config.io.SettingsMap;
import com.pg85.otg.config.settings.biome.*;
import com.pg85.otg.config.preset.PresetConfig;
import com.pg85.otg.constants.Constants;
import com.pg85.otg.customobject.resource.CustomObjectResource;
import com.pg85.otg.customobject.resource.CustomStructureResource;
import com.pg85.otg.customobject.resource.SaplingResource;
import com.pg85.otg.customobject.resource.TreeResource;
import com.pg85.otg.gen.resource.*;
import com.pg85.otg.interfaces.*;
import com.pg85.otg.util.OTGMaterialReader;
import com.pg85.otg.util.biome.OTGBiomeID;
import com.pg85.otg.util.materials.LocalMaterialData;
import com.pg85.otg.util.minecraft.SaplingType;
import lombok.Getter;
import lombok.Setter;

/**
 * BiomeConfig (*.bc) classes
 * 
 * BiomeSettings defines and implements anything needed for BiomeConfig. BiomeConfig
 * contains mainly io/serialisation/instantiation, or details specific to the file of origin.
 * 
 * BiomeConfig should be used only in common-core and platform-specific layers,
 * when reading/writing settings on app start. BiomeSettings should be used
 * wherever settings are used in code.
 */
public class BiomeConfig extends BiomeSettings
{
	public static final HashMap<String, Class<? extends ConfigFunction<?>>> RESOURCE_QUEUE_RESOURCES = new HashMap<>();
	static
	{
		RESOURCE_QUEUE_RESOURCES.put("AboveWaterRes", AboveWaterResource.class);
		RESOURCE_QUEUE_RESOURCES.put("Boulder", BoulderResource.class);
		RESOURCE_QUEUE_RESOURCES.put("Cactus", CactusResource.class);
		RESOURCE_QUEUE_RESOURCES.put("Dungeon", DungeonResource.class);
		RESOURCE_QUEUE_RESOURCES.put("Grass", GrassResource.class);
		RESOURCE_QUEUE_RESOURCES.put("Fossil", FossilResource.class);
		RESOURCE_QUEUE_RESOURCES.put("IceSpike", IceSpikeResource.class);
		RESOURCE_QUEUE_RESOURCES.put("Liquid", LiquidResource.class);
		RESOURCE_QUEUE_RESOURCES.put("Ore", OreResource.class);
		RESOURCE_QUEUE_RESOURCES.put("Plant", PlantResource.class);
		RESOURCE_QUEUE_RESOURCES.put("UnderWaterPlant", UnderWaterPlantResource.class);
		RESOURCE_QUEUE_RESOURCES.put("Reed", ReedResource.class);
		RESOURCE_QUEUE_RESOURCES.put("SmallLake", SmallLakeResource.class);
		RESOURCE_QUEUE_RESOURCES.put("SurfacePatch", SurfacePatchResource.class);
		RESOURCE_QUEUE_RESOURCES.put("UndergroundLake", UndergroundLakeResource.class);
		RESOURCE_QUEUE_RESOURCES.put("UnderWaterOre", UnderWaterOreResource.class);
		RESOURCE_QUEUE_RESOURCES.put("Vein", VeinResource.class);
		RESOURCE_QUEUE_RESOURCES.put("Vines", VinesResource.class);
		RESOURCE_QUEUE_RESOURCES.put("Well", WellResource.class);
		RESOURCE_QUEUE_RESOURCES.put("CustomObject", CustomObjectResource.class);
		RESOURCE_QUEUE_RESOURCES.put("CustomStructure", CustomStructureResource.class);
		RESOURCE_QUEUE_RESOURCES.put("Sapling", SaplingResource.class);
		RESOURCE_QUEUE_RESOURCES.put("Tree", TreeResource.class);
		RESOURCE_QUEUE_RESOURCES.put("Bamboo", BambooResource.class);
		RESOURCE_QUEUE_RESOURCES.put("SeaGrass", SeaGrassResource.class);
		RESOURCE_QUEUE_RESOURCES.put("Kelp", KelpResource.class);
		RESOURCE_QUEUE_RESOURCES.put("SeaPickle", SeaPickleResource.class);
		RESOURCE_QUEUE_RESOURCES.put("Registry", RegistryResource.class);
		RESOURCE_QUEUE_RESOURCES.put("CoralMushroom", CoralMushroomResource.class);
		RESOURCE_QUEUE_RESOURCES.put("CoralTree", CoralTreeResource.class);
		RESOURCE_QUEUE_RESOURCES.put("CoralClaw", CoralClawResource.class);
		RESOURCE_QUEUE_RESOURCES.put("Iceberg", IcebergResource.class);
		RESOURCE_QUEUE_RESOURCES.put("BasaltColumn", BasaltColumnResource.class);
	}

	private final Path path;
	private final PresetConfig parent;
	private OTGBiomeID otgBiomeID;

	public BiomeConfig(SettingsMap settingsMap, PresetConfig presetSettings, IConfigFunctionProvider biomeResourcesManager)
	{
		super(settingsMap.getName());
		this.path = settingsMap.getPath();
		IMaterialReader materialReader = OTGMaterialReader.get();
		parent = presetSettings;
		renameOldSettings(settingsMap, OTG.getEngine().getLogger(), materialReader);
		identitySettings = IdentitySettings.buildIdentitySettings(settingsMap);
		mobSettings = MobSettings.getMobSettings(settingsMap);
		generationSettings = BiomeGenerationSettings.getPlacementSettings(settingsMap, presetSettings.getGenerationSettings());
		structureSettings = BiomeStructureSettings.getBiomeStructureSettings(settingsMap, presetSettings.getStructureSettings());
		terrainSettings = BiomeTerrainSettings.getBiomeTerrainSettings(settingsMap, presetSettings.getTerrainSettings());
		visualSettings = BiomeVisualSettings.getBiomeVisualSettings(settingsMap, presetSettings.getVisualSettings());

		surfaceSettings = SurfaceSettings.getSurfaceSettings(
				settingsMap,
				materialReader,
				presetSettings.getBlockSettings());

		resourceSettings = BiomeResourceSettings.getResourceSettings(
				settingsMap,
				presetSettings.getResourceSettings(),
				new ArrayList<>(
						settingsMap.getConfigFunctions(
								this,
								biomeResourcesManager,
								presetSettings.getConfigName(),
								OTG.getEngine().getPluginConfig())));
	}


    @Override
	public void setOTGBiomeId(int id) {
		this.oldOTGBiomeID = id;
		this.otgBiomeID = new OTGBiomeID(id, this.getRegistryKey(), this.getConfigName());
	}

	@Override
	public int getOldOTGBiomeID() {
		return this.oldOTGBiomeID;
	}
	@Setter
    @Getter
    private IBiomeResourceLocation registryKey;
	private int oldOTGBiomeID;

	@Override
	public Path getConfigPath() {
		return path;
	}

	public void writeConfigSettings(SettingsMap writer) {
		writer.putSetting(Constants.ConfigVersionSetting, Constants.ConfigVersion);
		BiomeConfigWriter.writeConfigSettings(this, writer);
	}

	@Override
	public void renameOldSettings(SettingsMap settings, ILogger logger, IMaterialReader materialReader)
	{
		settings.renameOldSetting("DisableNotchHeightControl", BiomeTerrainSettings.DISABLE_BIOME_HEIGHT);
		settings.renameOldSetting("BiomeDictId", IdentitySettings.BIOME_DICT_TAGS);
		settings.renameOldSetting("IsleInBiome", BiomeGenerationSettings.ISLE_IN_BIOMES);
		settings.renameOldSetting("BiomeIsBorder", BiomeGenerationSettings.BORDER_IN_BIOMES);
		settings.renameOldSetting("BiomeColor", BiomeGenerationSettings.BIOME_MAP_COLOR);
	}

	@Override
	public List<ConfigFunction<BiomeSettings>> getResourceQueue() {
		return this.getResourceSettings().getResourceQueue();
	}

	@Override
	public List<List<String>> getCustomStructureNames() {
		List<List<String>> customStructureNamesByGen = new ArrayList<>();
		for (ICustomStructureGen structureGens : this.getResourceSettings().getCustomStructures()) {
			List<String> customStructureNames = Arrays.asList(structureGens.getObjectNames());
			customStructureNamesByGen.add(customStructureNames);
		}
		return customStructureNamesByGen;
	}

	@Override
	public void setStructureGen(ICustomStructureGen customStructureGen) {
		this.structureGen = customStructureGen;
	}

	@Override
	public boolean getIsTemplateForBiome() {
		return this.getIdentitySettings().isTemplateForBiome();
	}

	@Override
	public List<String> getBiomeDictTags() {
		return this.getIdentitySettings().getBiomeDictTags();
	}

	@Override
	public double getCHCData(int controlLayer) {
		return this.getTerrainSettings().getCustomHeightControl()[controlLayer];
	}


	@Override
	public boolean biomeConfigsHaveReplacement() {
		return this.parent.isBiomeConfigsHaveReplacement();
	}

	@Override
	public ISaplingSpawner getSaplingGen(SaplingType type) {
		ISaplingSpawner gen = this.resourceSettings.getSaplingGrowers().get(type);
		if (gen == null && type.growsTree()) {
			gen = this.resourceSettings.getSaplingGrowers().get(SaplingType.All);
		}
		return gen;
	}

	public ISaplingSpawner getCustomSaplingGen(LocalMaterialData materialData, boolean wideTrunk) {
		if (wideTrunk) {
			ISaplingSpawner spawner = this.resourceSettings.getCustomBigSaplingGrowers().get(materialData);
			if (spawner != null) {
				return spawner;
			}
		}
		return this.resourceSettings.getCustomSaplingGrowers().get(materialData);
	}

	public OTGBiomeID getOTGBiomeID() {
		return this.otgBiomeID;
	}
}
