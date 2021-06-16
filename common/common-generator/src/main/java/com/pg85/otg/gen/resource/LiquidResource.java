package com.pg85.otg.gen.resource;

import com.pg85.otg.constants.Constants;
import com.pg85.otg.exception.InvalidConfigException;
import com.pg85.otg.logging.ILogger;
import com.pg85.otg.util.ChunkCoordinate;
import com.pg85.otg.util.helpers.RandomHelper;
import com.pg85.otg.util.interfaces.IBiomeConfig;
import com.pg85.otg.util.interfaces.IMaterialReader;
import com.pg85.otg.util.interfaces.IWorldGenRegion;
import com.pg85.otg.util.materials.LocalMaterialData;
import com.pg85.otg.util.materials.MaterialSet;

import java.util.List;
import java.util.Random;

/**
 * Generates a waterfall feature, called a Spring in minecraft code.
 */
public class LiquidResource extends FrequencyResourceBase
{
	private final LocalMaterialData material;
	private final int maxAltitude;
	private final int minAltitude;
	private final MaterialSet sourceBlocks;

	public LiquidResource(IBiomeConfig biomeConfig, List<String> args, ILogger logger, IMaterialReader materialReader) throws InvalidConfigException
	{
		super(biomeConfig, args, logger, materialReader);
		assureSize(6, args);

		this.material = materialReader.readMaterial(args.get(0));
		this.frequency = readInt(args.get(1), 1, 5000);
		this.rarity = readRarity(args.get(2));
		this.minAltitude = readInt(args.get(3), Constants.WORLD_DEPTH, Constants.WORLD_HEIGHT - 1);
		this.maxAltitude = readInt(args.get(4), this.minAltitude, Constants.WORLD_HEIGHT - 1);
		this.sourceBlocks = readMaterials(args, 5, materialReader);
	}

	@Override
	public void spawn(IWorldGenRegion worldGenregion, Random rand, boolean villageInChunk, int x, int z, ChunkCoordinate chunkBeingDecorated)
	{
		int y = RandomHelper.numberInRange(rand, this.minAltitude, this.maxAltitude);

		LocalMaterialData worldMaterial = worldGenregion.getMaterial(x, y + 1, z, chunkBeingDecorated);
		if (worldMaterial == null || !this.sourceBlocks.contains(worldMaterial))
		{
			return;
		}
		
		worldMaterial = worldGenregion.getMaterial(x, y - 1, z, chunkBeingDecorated);
		if (worldMaterial == null || !this.sourceBlocks.contains(worldMaterial))
		{
			return;
		}

		worldMaterial = worldGenregion.getMaterial(x, y, z, chunkBeingDecorated);
		if (worldMaterial == null || !worldMaterial.isAir() || !this.sourceBlocks.contains(worldMaterial))
		{
			return;
		}

		int sourceCount = 0;
		int airCount = 0;

		worldMaterial = worldGenregion.getMaterial(x - 1, y, z, chunkBeingDecorated);
		sourceCount = (worldMaterial != null && this.sourceBlocks.contains(worldMaterial)) ? sourceCount + 1 : sourceCount;
		airCount = (worldMaterial != null && worldMaterial.isAir()) ? airCount + 1 : airCount;

		worldMaterial = worldGenregion.getMaterial(x + 1, y, z, chunkBeingDecorated);
		sourceCount = (worldMaterial != null && this.sourceBlocks.contains(worldMaterial)) ? sourceCount + 1 : sourceCount;
		airCount = (worldMaterial != null && worldMaterial.isAir()) ? airCount + 1 : airCount;

		worldMaterial = worldGenregion.getMaterial(x, y, z - 1, chunkBeingDecorated);
		sourceCount = (worldMaterial != null && this.sourceBlocks.contains(worldMaterial)) ? sourceCount + 1 : sourceCount;
		airCount = (worldMaterial != null && worldMaterial.isAir()) ? airCount + 1 : airCount;

		worldMaterial = worldGenregion.getMaterial(x, y, z + 1, chunkBeingDecorated);
		sourceCount = (worldMaterial != null && this.sourceBlocks.contains(worldMaterial)) ? sourceCount + 1 : sourceCount;
		airCount = (worldMaterial != null && worldMaterial.isAir()) ? airCount + 1 : airCount;

		if ((sourceCount == 3) && (airCount == 1))
		{
			worldGenregion.setBlock(x, y, z, this.material, null, chunkBeingDecorated, false);
		}
	}
	
	@Override
	public String toString()
	{
		return "Liquid(" + this.material + "," + this.frequency + "," + this.rarity + "," + this.minAltitude + "," + this.maxAltitude + makeMaterials(this.sourceBlocks) + ")";
	}	
}
