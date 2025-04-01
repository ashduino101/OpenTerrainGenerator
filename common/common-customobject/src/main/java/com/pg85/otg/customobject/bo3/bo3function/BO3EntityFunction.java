package com.pg85.otg.customobject.bo3.bo3function;

import com.pg85.otg.customobject.bo3.BO3Config;
import com.pg85.otg.customobject.bofunctions.EntityFunction;
import com.pg85.otg.exceptions.InvalidConfigException;
import com.pg85.otg.interfaces.IEntityFunction;
import com.pg85.otg.interfaces.ILogger;
import com.pg85.otg.interfaces.IMaterialReader;
import com.pg85.otg.util.helpers.StreamHelper;
import com.pg85.otg.util.helpers.StringHelper;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Represents an entity in a BO3.
 */
public class BO3EntityFunction extends EntityFunction<BO3Config> implements IEntityFunction
{
	public BO3EntityFunction rotate()
	{
		BO3EntityFunction rotatedBlock = new BO3EntityFunction();
		rotatedBlock.x = z;
		rotatedBlock.y = y;
		rotatedBlock.z = -x;
		rotatedBlock.name = name;
		rotatedBlock.resourceLocation = resourceLocation;
		rotatedBlock.groupSize = groupSize;
		rotatedBlock.originalNameTagOrNBTFileName = originalNameTagOrNBTFileName;
		rotatedBlock.nameTagOrNBTFileName = nameTagOrNBTFileName;
		rotatedBlock.namedBinaryTag = namedBinaryTag;
		rotatedBlock.rotation = (rotation + 1) % 4;

		return rotatedBlock;
	}

	@Override
	public Class<BO3Config> getHolderType()
	{
		return BO3Config.class;
	}

	@Override
	public EntityFunction<BO3Config> createNewInstance()
	{
		return new BO3EntityFunction();
	}

	// TODO: this is only used in BO3Config.readFromStream, try to get rid of it
	public static BO3EntityFunction fromStream(DataInputStream stream, ILogger logger, IMaterialReader materialReader) throws IOException, InvalidConfigException
	{
		BO3EntityFunction entityFunction = new BO3EntityFunction();
		String configFunctionString = StreamHelper.readStringFromStream(stream);
		int bracketIndex = configFunctionString.indexOf('(');
		String parameters = configFunctionString.substring(bracketIndex + 1, configFunctionString.length() - 1);
		List<String> args = Arrays.asList(StringHelper.readCommaSeperatedString(parameters));
		entityFunction.load(args, logger, materialReader);
		return entityFunction;
	}
}
