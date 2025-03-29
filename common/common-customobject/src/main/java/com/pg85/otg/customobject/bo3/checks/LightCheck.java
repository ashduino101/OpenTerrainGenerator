package com.pg85.otg.customobject.bo3.checks;

import com.pg85.otg.customobject.bo3.BO3Config;
import com.pg85.otg.exceptions.InvalidConfigException;
import com.pg85.otg.interfaces.ILogger;
import com.pg85.otg.interfaces.IMaterialReader;
import com.pg85.otg.interfaces.IWorldGenRegion;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

public class LightCheck extends BO3Check
{
	/** The minimum Light level, inclusive */
	private int minLightLevel;
	/** The maximum Light level, inclusive */
	private int maxLightLevel;

	@Override
	public boolean preventsSpawn(IWorldGenRegion worldGenRegion, int x, int y, int z)
	{
		int lightLevel = worldGenRegion.getLightLevel(x, y, z);
		if (lightLevel < minLightLevel || lightLevel > maxLightLevel)
		{
			// Out of bounds
			return true;
		}
		return false;
	}

	@Override
	public void load(List<String> args, ILogger logger, IMaterialReader materialReader) throws InvalidConfigException
	{
		assureSize(5, args);
		x = readInt(args.get(0), -100, 100);
		y = readInt(args.get(1), -100, 100);
		z = readInt(args.get(2), -100, 100);
		minLightLevel = readInt(args.get(3), 0, 16);
		maxLightLevel = readInt(args.get(4), minLightLevel, 16);
	}

	@Override
	public String makeString()
	{
		return "LightCheck(" + x + ',' + y + ',' + z + ',' + minLightLevel + ',' + maxLightLevel + ')';
	}

	/**
	 * Writes the light levels to a stream.
	 */
	public void writeLevelsToStream(DataOutput stream) throws IOException {
		// Technically, the check allows 16, but Minecraft light levels only
		// go up to 15, so higher levels don't matter
		stream.writeByte(Math.min(this.minLightLevel, 15) | (Math.min(this.maxLightLevel, 15) << 4));
	}

	/**
	 * Reads the light levels from a stream.
	 */
	public void readLevelsFromStream(DataInput stream) throws IOException {
		byte b = stream.readByte();
		this.minLightLevel = b & 0b00001111;
		this.maxLightLevel = b & 0b11110000;
	}

	@Override
	public BO3Check rotate()
	{
		LightCheck rotatedCheck = new LightCheck();
		rotatedCheck.x = z;
		rotatedCheck.y = y;
		rotatedCheck.z = -x;
		rotatedCheck.minLightLevel = minLightLevel;
		rotatedCheck.maxLightLevel = maxLightLevel;

		return rotatedCheck;
	}
	
	@Override
	public Class<BO3Config> getHolderType()
	{
		return BO3Config.class;
	}
}
