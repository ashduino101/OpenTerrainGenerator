package com.pg85.otg.customobject.bo4;

import java.io.*;
import java.nio.file.Path;

import com.pg85.otg.customobject.CustomObjectManager;
import com.pg85.otg.customobject.config.CustomObjectResourcesManager;
import com.pg85.otg.interfaces.ILogger;
import com.pg85.otg.interfaces.IMaterialReader;
import com.pg85.otg.interfaces.IModLoadedChecker;

public class BO4Data
{
	public static boolean bo4DataExists(BO4Config config)
	{
		String filePath = 
			config.getFile().getAbsolutePath().endsWith(".BO4") ? config.getFile().getAbsolutePath().replace(".BO4", ".BO4Data") :
			config.getFile().getAbsolutePath().endsWith(".bo4") ? config.getFile().getAbsolutePath().replace(".bo4", ".BO4Data") :
			config.getFile().getAbsolutePath().endsWith(".BO3") ? config.getFile().getAbsolutePath().replace(".BO3", ".BO4Data") :
			config.getFile().getAbsolutePath().endsWith(".bo3") ? config.getFile().getAbsolutePath().replace(".bo3", ".BO4Data") :
			config.getFile().getAbsolutePath();

		File file = new File(filePath);
		return file.exists();
	}

	public static void generateBO4DataToStream(BO4Config config, DataOutputStream stream, String presetFolderName, Path otgRootFolder, ILogger logger, CustomObjectManager customObjectManager, IMaterialReader materialReader, CustomObjectResourcesManager manager, IModLoadedChecker modLoadedChecker, boolean compress) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(bos);
			config.writeToStream(dos, presetFolderName, otgRootFolder, logger, customObjectManager, materialReader, manager, modLoadedChecker);
			if (compress) {
				byte[] compressedBytes = com.pg85.otg.util.CompressionUtils.compress(bos.toByteArray(), logger);
				stream.write(compressedBytes, 0, compressedBytes.length);
			} else {
				byte[] result = bos.toByteArray();
				stream.write(result, 0, result.length);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void generateBO4Data(BO4Config config, String presetFolderName, Path otgRootFolder, ILogger logger, CustomObjectManager customObjectManager, IMaterialReader materialReader, CustomObjectResourcesManager manager, IModLoadedChecker modLoadedChecker)
	{
		//write to disk
		String filePath = 
			config.getFile().getAbsolutePath().endsWith(".BO4") ? config.getFile().getAbsolutePath().replace(".BO4", ".BO4Data") :
			config.getFile().getAbsolutePath().endsWith(".bo4") ? config.getFile().getAbsolutePath().replace(".bo4", ".BO4Data") :
			config.getFile().getAbsolutePath().endsWith(".BO3") ? config.getFile().getAbsolutePath().replace(".BO3", ".BO4Data") :
			config.getFile().getAbsolutePath().endsWith(".bo3") ? config.getFile().getAbsolutePath().replace(".bo3", ".BO4Data") :
			config.getFile().getAbsolutePath();

		File file = new File(filePath);
		if(!file.exists())
		{
			try {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(bos);
				config.writeToStream(dos, presetFolderName, otgRootFolder, logger, customObjectManager, materialReader, manager, modLoadedChecker);
				byte[] compressedBytes = com.pg85.otg.util.CompressionUtils.compress(bos.toByteArray(), logger);
				dos.close();
				FileOutputStream fos = new FileOutputStream(file);
				DataOutputStream dos2 = new DataOutputStream(fos);
				dos2.write(compressedBytes, 0, compressedBytes.length);
				dos2.close();
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
