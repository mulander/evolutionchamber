package com.fray.evo.action.build;

import java.io.Serializable;

import com.fray.evo.EcBuildOrder;
import com.fray.evo.util.Building;
import com.fray.evo.util.ZergBuildingLibrary;
import com.fray.evo.util.GameLog;

public final class EcActionBuildHive extends EcActionBuildBuilding implements Serializable
{
	public EcActionBuildHive()
	{
		super(ZergBuildingLibrary.Hive);
	}

	@Override
	protected void preExecute(EcBuildOrder s)
	{		
		s.makeBuildingBusy((Building)buildable.getConsumes(),this);
	}

	@Override
	protected void postExecute(EcBuildOrder s, GameLog e)
	{
            s.makeBuildingNotBusy(this);
            s.RemoveBuilding(ZergBuildingLibrary.Lair);
		s.AddBuilding((Building) buildable);
	}
}