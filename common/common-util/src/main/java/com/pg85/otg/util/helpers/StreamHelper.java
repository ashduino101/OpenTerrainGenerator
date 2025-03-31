package com.pg85.otg.util.helpers;

import java.io.*;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class StreamHelper
{

	public static void writeStringToStream(DataOutput stream, String value) throws IOException
	{
		stream.writeBoolean(value == null);
		if(value != null)
		{
			byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
			stream.writeShort(bytes.length);
			stream.write(bytes);
		}
	}

	public static String readStringFromStream(DataInputStream stream) throws IOException
	{
		boolean isNull = stream.readBoolean();
		if(isNull)
		{
			return null;
		}
		
		short length = stream.readShort();
		byte[] chars = new byte[length];
		if(length > 0)
		{
			if (stream.read(chars, 0, chars.length) != chars.length)
			{
				throw new EOFException();
			}
			return new String(chars);
		} else {
			return "";
		}
	}
	
	public static String readStringFromBuffer(ByteBuffer buffer) throws IOException, BufferUnderflowException
	{
		boolean isNull = buffer.get() != 0;
		if(isNull)
		{
			return null;
		}
		
		short length = buffer.getShort();
		byte[] chars = new byte[length];
		if(length > 0)
		{
			buffer.get(chars, 0, chars.length);
			return new String(chars);
		} else {
			return "";
		}
	}

	// Based on https://minecraft.wiki/w/Java_Edition_protocol#VarInt_and_VarLong
	// Implements ZigZag coding as described in https://gist.github.com/mfuerstenau/ba870a29e16536fdbaba
	/// Reads a signed ZigZag-encoded VarInt from the stream.
	public static int readVarIntFromStream(DataInput stream) throws IOException {
		int value = 0;
		int position = 0;
		byte currentByte;

		while (true) {
			currentByte = stream.readByte();
			value |= (currentByte & 0x7f) << position;

			if ((currentByte & 0x80) == 0) break;

			position += 7;

			if (position >= 32) throw new RuntimeException("VarInt is too big");
		}

		return (value >>> 1) ^ -(value & 1);
	}

	/// Writes a signed ZigZag-encoded VarInt to the stream.
	public static void writeVarIntToStream(DataOutput stream, int value) throws IOException {
		value = (value << 1) ^ (value >> 31);
		int continuationBytes = (31 - Integer.numberOfLeadingZeros(value)) / 7;
		for (int i = 0; i < continuationBytes; ++i) {
			stream.writeByte(((byte) ((value & 0x7F) | 0x80)));
			value >>>= 7;
		}
		stream.writeByte((byte) value);
	}
}
