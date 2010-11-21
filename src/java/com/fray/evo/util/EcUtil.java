package com.fray.evo.util;

import java.util.ArrayList;

public class EcUtil
{

	public static String toString(ArrayList<String> strings)
	{
		String result = "";
		for (String s : strings)
			result += s + "\n";
		return result;
	}
	
}
