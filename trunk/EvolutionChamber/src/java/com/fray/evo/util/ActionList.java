package com.fray.evo.util;

import java.util.ArrayList;
import java.util.HashMap;

public class ActionList {
	private static final int numIntegers = 10000;
	private static Integer[] integers = new Integer[numIntegers];

	private int numLeft;
	private HashMap<Integer, ArrayList<Runnable>> map;
	
	public ActionList()
	{
		numLeft = 0;
		map = new HashMap<Integer, ArrayList<Runnable>>();
	}
	
	public void put( int i, Runnable r )
	{
		Integer inte = getInteger( i );
		
		ArrayList<Runnable> list = map.get( i );
		if( list == null )
		{
			list = new ArrayList<Runnable>();
			list.add( r );
			map.put( inte, list );
		}
		else
		{
			list.add( r );
		}
		numLeft++;
	}
	
	public ArrayList<Runnable> get( int i )
	{
		Integer inte = getInteger( i );

		ArrayList<Runnable> result = map.get( inte );
		if( result != null )
			numLeft -= result.size();
		return result;
	}

	public boolean hasFutureActions()
	{
		return numLeft > 0;
	}
	
	private Integer getInteger( int i )
	{
		if( i > numIntegers )
			return new Integer( i );
		
		if( integers[i] == null )
			integers[i] = new Integer( i );
		return integers[i];
	}
}
