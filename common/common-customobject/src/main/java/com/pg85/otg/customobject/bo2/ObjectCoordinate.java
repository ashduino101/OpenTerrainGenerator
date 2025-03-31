package com.pg85.otg.customobject.bo2;

import com.pg85.otg.customobject.bofunctions.BlockFunction;
import com.pg85.otg.customobject.config.CustomObjectConfigFile;
import com.pg85.otg.exceptions.InvalidConfigException;
import com.pg85.otg.interfaces.IMaterialReader;
import com.pg85.otg.interfaces.IWorldGenRegion;
import com.pg85.otg.util.biome.ReplaceBlockMatrix;

import java.util.Random;

class ObjectCoordinate extends BlockFunction<BO2>
{
	private int branchDirection;
	private int branchOdds;

	protected ObjectCoordinate() {
		this.branchDirection = -1;
		this.branchOdds = -1;

		this.nbt = null;
		this.nbtName = "";
	}

	private ObjectCoordinate(int _x, short _y, int _z)
	{
		this.x = _x;
		this.y = _y;
		this.z = _z;
		this.branchDirection = -1;
		this.branchOdds = -1;

		this.nbt = null;
		this.nbtName = "";
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof ObjectCoordinate)
		{
			ObjectCoordinate object = (ObjectCoordinate) obj;
			return object.x == this.x && object.y == this.y && object.z == this.z;
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return this.x + this.z << 8 + this.y << 16;
	}

	ObjectCoordinate rotate()
	{
		ObjectCoordinate newCoordinate = new ObjectCoordinate(this.z, this.y, (this.x * -1));
		newCoordinate.material = this.material.rotate();
		newCoordinate.holder = this.holder;
		newCoordinate.branchOdds = this.branchOdds;

		if (this.branchDirection != -1)
		{
			newCoordinate.branchDirection = this.branchDirection + 1;
			if (newCoordinate.branchDirection > 3)
			{
				newCoordinate.branchDirection = 0;
			}
		}

		return newCoordinate;
	}

	static ObjectCoordinate getCoordinateFromString(String key, String value, IMaterialReader materialReader, BO2 holder)
	{
		String[] coordinates = key.split(",(?![^\\(\\[]*[\\]\\)])", 3); // Splits on any comma not inside brackets
		if (coordinates.length != 3)
		{
			return null;
		}

		try
		{

			int x = Integer.parseInt(coordinates[0]);
			int z = Integer.parseInt(coordinates[1]);
			int y = Integer.parseInt(coordinates[2]);

			ObjectCoordinate newCoordinate = new ObjectCoordinate(x, (short) y, z);

			// TODO: What is this for, where do we ever use # or @?
			String workingDataString = value;
			if (workingDataString.contains("#"))
			{
				String[] stringSet = workingDataString.split("#");
				workingDataString = stringSet[0];
				String[] branchData = stringSet[1].split("@");
				newCoordinate.branchDirection = Integer.parseInt(branchData[0]);
				newCoordinate.branchOdds = Integer.parseInt(branchData[1]);
			}
			newCoordinate.material = materialReader.readMaterial(workingDataString);
			newCoordinate.holder = holder;

			return newCoordinate;
		}
		catch (NumberFormatException | InvalidConfigException e)
		{
			return null;
		}
    }

	// These functions are only implemented so that we can extend BlockFunction
	@Override
	public void spawn(IWorldGenRegion worldGenRegion, Random random, int x, int y, int z) {
		worldGenRegion.setBlock(x, y, z, this.material, this.nbt);
	}

	@Override
	public void spawn(IWorldGenRegion worldGenRegion, Random random, int x, int y, int z, ReplaceBlockMatrix replaceBlocks) {
		worldGenRegion.setBlock(x, y, z, this.material, this.nbt, replaceBlocks);
	}

	@Override
	public Class<BO2> getHolderType() {
		return BO2.class;
	}
}
