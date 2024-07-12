package com.pg85.otg.customobject.bo3.bo3function;

import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;

import com.pg85.otg.customobject.bo3.BO3Config;
import com.pg85.otg.customobject.bo4.BO4Config;
import com.pg85.otg.customobject.bo4.bo4function.BO4BlockFunction;
import com.pg85.otg.customobject.bofunctions.BlockFunction;
import com.pg85.otg.interfaces.ILogger;
import com.pg85.otg.interfaces.IWorldGenRegion;
import com.pg85.otg.util.biome.ReplaceBlockMatrix;
import com.pg85.otg.util.materials.LocalMaterialData;
import com.pg85.otg.util.nbt.NBTHelper;

/**
 * Represents a block in a BO3.
 */
public class BO3BlockFunction extends BlockFunction<BO3Config>
{
	public BO3BlockFunction() { }
	
	public BO3BlockFunction(BO3Config holder)
	{
		this.holder = holder;
	}
	
	public BO3BlockFunction rotate()
	{
		BO3BlockFunction rotatedBlock = new BO3BlockFunction();
		rotatedBlock.x = z;
		rotatedBlock.y = y;
		rotatedBlock.z = -x;
		rotatedBlock.material = material.rotate();
		rotatedBlock.nbt = nbt;
		rotatedBlock.nbtName = nbtName;

		return rotatedBlock;
	}

	@Override
	public void spawn(IWorldGenRegion worldGenRegion, Random random, int x, int y, int z)
	{
		worldGenRegion.setBlock(x, y, z, this.material, this.nbt);			
	}	
	
	@Override
	public void spawn(IWorldGenRegion worldGenRegion, Random random, int x, int y, int z, ReplaceBlockMatrix replaceBlocks)
	{
		worldGenRegion.setBlock(x, y, z, this.material, this.nbt, replaceBlocks);
	}

	public void writeToStream(List<String> metaDataNames, List<LocalMaterialData> materials, DataOutput stream) throws IOException
	{
		stream.writeShort(this.y);
		boolean bFound = false;
		if(this.material != null)
		{
			for(int i = 0; i < materials.size(); i++)
			{
				if(materials.get(i).equals(this.material))
				{
					stream.writeShort(i);
					bFound = true;
					break;
				}
			}
		}
		if(!bFound)
		{
			stream.writeShort(-1);
		}
		bFound = false;
		if(this.nbtName != null)
		{
			for(int i = 0; i < metaDataNames.size(); i++)
			{
				if(metaDataNames.get(i).equals(this.nbtName))
				{
					stream.writeShort(i);
					bFound = true;
					break;
				}
			}
		}
		if(!bFound)
		{
			stream.writeShort(-1);
		}
	}

	public static BO3BlockFunction fromStream(int x, int z, String[] metaDataNames, LocalMaterialData[] materials, BO3Config holder, ByteBuffer buffer, ILogger logger) throws IOException
	{
		BO3BlockFunction rbf = new BO3BlockFunction(holder);

		File file = holder.getFile();

		rbf.x = x;
		rbf.y = buffer.getShort();
		rbf.z = z;

		short materialId = buffer.getShort();
		if(materialId != -1)
		{
			rbf.material = materials[materialId];
		}
		short metaDataNameId = buffer.getShort();
		if(metaDataNameId != -1)
		{
			rbf.nbtName = metaDataNames[metaDataNameId];
		}

		if(rbf.nbtName != null)
		{
			// Get the file
			rbf.nbt = NBTHelper.loadMetadata(rbf.nbtName, file, logger);
			if(rbf.nbt == null)
			{
				rbf.nbtName = null;
			}
		}

		return rbf;
	}

	@Override
	public Class<BO3Config> getHolderType()
	{
		return BO3Config.class;
	}
}
