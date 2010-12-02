package com.fray.evo;
import static com.fray.evo.ui.swingx.EcSwingXMain.messages;
import com.fray.evo.util.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;


public class EcState implements Serializable
{
	public EcSettings	settings	= new EcSettings();
        private HashSet<Upgrade> upgrades;
        private HashMap<Building, Integer> buildings;
        private HashMap<Unit, Integer> units;

	public EcState()
	{
		hatcheryTimes.add(new Integer(0));
                units = new HashMap<Unit, Integer>();
                for(Unit unit: UnitLibrary.getAllZergUnits()){
                    units.put(unit, 0);
                }
                buildings = new HashMap<Building, Integer>();
                for(Building building: BuildingLibrary.getAllZergBuildings()){
                    buildings.put(building, 0);
                }
                upgrades = new HashSet<Upgrade>();

                units.put(UnitLibrary.Drone, 6);
                units.put(UnitLibrary.Overlord, 1);
                buildings.put(BuildingLibrary.Hatchery, 1);
	}

	public double					preTimeScore		= 0.0;
	public double					timeBonus			= 0.0;

	public double					minerals			= 50;
	public double					gas					= 0;
	public double					supplyUsed			= 6;
	public int						evolvingHatcheries	= 0;
	public int						evolvingLairs		= 0;
	public int						evolvingHives		= 0;
	public int						requiredBases		= 1;

	private int						larva			= 3;

	public int						scoutDrone			= 0;

	public int						seconds				= 0;
	public int						targetSeconds		= 0;
	public int						invalidActions		= 0;
	public double					actionLength		= 0;
	public int						waits;

	public int						maxOverDrones		= 50;
	public int						overDroneEfficiency	= 80;

	public List<Integer>			hatcheryTimes		= new ArrayList<Integer>();

	public ArrayList<EcState>		waypoints			= new ArrayList<EcState>();
	public EcState					mergedWaypoints		= null;

	@Override
	public Object clone() throws CloneNotSupportedException
	{
		EcState s = new EcState();
		assign(s);
		return s;
	}

    protected void assign(EcState s) {
        for (EcState st : waypoints) {
            try {
                s.waypoints.add((EcState) st.clone());
            } catch (CloneNotSupportedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        try {
            if (mergedWaypoints == null) {
                s.mergedWaypoints = null;
            } else {
                s.mergedWaypoints = (EcState) mergedWaypoints.clone();
            }
        } catch (CloneNotSupportedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        s.settings = settings;
        s.minerals = minerals;
        s.gas = gas;
        s.supplyUsed = supplyUsed;

        s.requiredBases = requiredBases;
        s.scoutDrone = scoutDrone;

        s.buildings = (HashMap<Building, Integer>) buildings.clone();
        s.upgrades = (HashSet<Upgrade>) upgrades.clone();
        s.units = (HashMap<Unit, Integer>) units.clone();

        s.seconds = seconds;
        s.targetSeconds = targetSeconds;
        s.invalidActions = invalidActions;
        s.actionLength = actionLength;
    }

	public int supply()
	{
		return Math.min((getOverlords() + getOverseers()) * 8 + 2 * bases(), 200);
	}

        public int supplyClean()
	{
		return Math.min((units.get(UnitLibrary.Overlord) + units.get(UnitLibrary.Overseer)) * 8 + 2 * bases(), 200);
	}

	public static EcState defaultDestination()
	{
		EcState d = new EcState();

		d.SetUnits(UnitLibrary.Drone,0);
		d.SetUnits(UnitLibrary.Overlord, 0);
		d.SetBuilding(BuildingLibrary.Hatchery, 0);
		d.targetSeconds = 60 * 120;

		return d;
	}

	public double score(EcState candidate)
	{
		return settings.getFitnessFunction().score(candidate, this);
	}

	public void union(EcState s)
	{
		if (s.requiredBases > requiredBases)
			requiredBases = s.requiredBases;

		for(Unit unit: units.keySet()){
                    units.put(unit, Math.max(units.get(unit), s.units.get(unit)));
                }
                for(Building building: buildings.keySet()){
                    buildings.put(building, Math.max(buildings.get(building), s.buildings.get(building)));
                }
                upgrades.addAll(s.upgrades);
	}

        public boolean isSatisfied(EcState candidate)
	{
		if (waypoints.size() > 0)
		{
			if( mergedWaypoints == null )
				mergedWaypoints = getMergedState();
			return mergedWaypoints.isSatisfied(candidate);
		}

		for(Unit unit: units.keySet()){
                    if(candidate.units.get(unit) < units.get(unit)){
                        return false;
                    }
                }

		if (candidate.bases() < requiredBases)
			return false;

                for(Building building: buildings.keySet()){
                    if(candidate.buildings.get(building) < buildings.get(building)){
                        return false;
                    }
                }

                if(!candidate.upgrades.containsAll(upgrades)){
                    return false;
                }

		if (candidate.settings.overDrone || candidate.settings.workerParity)
		{
			int overDrones = getOverDrones(candidate);

			if (candidate.settings.overDrone && candidate.units.get(UnitLibrary.Drone) < overDrones)
			{
				return false;
			}
			if (candidate.settings.workerParity)
			{
				int parityDrones = getParityDrones(candidate);

				if (candidate.units.get(UnitLibrary.Drone) < parityDrones)
				{
					return false;
				}
			}
		}

		return true;
	}

	public int getOverDrones(EcState s)
	{
		int overDrones = ((s.productionTime() / 17) + s.usedDrones()) * overDroneEfficiency / 100;

		overDrones = Math.min(overDrones, maxOverDrones);

		return overDrones;
	}

	public int getParityDrones(EcState s)
	{
		int optimalDrones = Math.min((Math.min(s.bases(), 3) * 16) + (s.getGasExtractors() * 3), maxOverDrones);
		int parityDrones = Math.min(s.getOverDrones(s), optimalDrones);

		return parityDrones;
	}

        public int getParityDronesClean(EcState s)
	{
		int optimalDrones = Math.min((Math.min(s.bases(), 3) * 16) +
                        (s.buildings.get(BuildingLibrary.Extractor) * 3), maxOverDrones);
		int parityDrones = Math.min(s.getOverDrones(s), optimalDrones);

		return parityDrones;
	}

	public int bases()
	{
		return getHatcheries() + getLairs() + evolvingHatcheries + evolvingLairs + getHives() + evolvingHives;
	}

        public int basesClean()
	{
		return buildings.get(BuildingLibrary.Hatchery) + buildings.get(BuildingLibrary.Lair)
                        + evolvingHatcheries + evolvingLairs + buildings.get(BuildingLibrary.Hive) + evolvingHives;
	}

	public int productionTime()
	{
		int productionTime = 0;

		// Calculate raw hatchery production time
		for (int i = 0; i < Math.min(hatcheryTimes.size(),4); i++)
		{
			productionTime += seconds - hatcheryTimes.get(i); // TODO: Change to
																// constant
		}

		return productionTime;
	}

	public int usedDrones()
	{
		return (evolvingHatcheries + evolvingLairs + evolvingHives + (getHatcheries() - 1) + getLairs() + getHives() + getSpawningPools()
				+ getEvolutionChambers() + getRoachWarrens() + getHydraliskDen() + getBanelingNest() + getInfestationPit() + getUltraliskCavern()
				+ getGasExtractors() + getSpire() + getSpineCrawlers() + getSporeCrawlers() + getNydusWorm());
	}

        public int usedDronesClean()
	{
                int total = (evolvingHatcheries + evolvingLairs + evolvingHives + -1 );
                for(Building building: buildings.keySet()){
                    if(building.getConsumes() == UnitLibrary.Drone){
                        total += buildings.get(building);
                    }
                }
		return total;
	}

	public int getEstimatedActions()
	{
		if (waypoints.size() > 0)
		{
			if( mergedWaypoints == null )
				mergedWaypoints = getMergedState();
			return mergedWaypoints.getEstimatedActions();
		}

		int i = requiredBases + getLairs() + getHives() + getSpawningPools() + getEvolutionChambers() + getRoachWarrens() + getHydraliskDen()
				+ getBanelingNest() + getInfestationPit() + getGreaterSpire() + getUltraliskCavern() + getGasExtractors() + getSpire()
				+ getSpineCrawlers() + getSporeCrawlers() + getNydusNetwork() + getNydusWorm()

				+ getDrones() + getOverlords() + getOverseers() + getZerglings() + getBanelings() * 2 + getRoaches() + getMutalisks()*2 + getInfestors()*2 + getQueens()
				+ getHydralisks()*2 + getCorruptors()*2 + getUltralisks()*2 + getBroodlords() * 4;

		if (isMetabolicBoost())
			i++;
		if (isAdrenalGlands())
			i++;
		if (isGlialReconstitution())
			i++;
		if (isTunnelingClaws())
			i++;
		if (isBurrow())
			i++;
		if (isPneumatizedCarapace())
			i++;
		if (isVentralSacs())
			i++;
		if (isCentrifugalHooks())
			i++;
		if (isMelee1())
			i++;
		if (isMelee2())
			i++;
		if (isMelee3())
			i++;
		if (isMissile1())
			i++;
		if (isMissile2())
			i++;
		if (isMissile3())
			i++;
		if (isArmor1())
			i++;
		if (isArmor2())
			i++;
		if (isArmor3())
			i++;
		if (isGroovedSpines())
			i++;
		if (isNeuralParasite())
			i++;
		if (isPathogenGlands())
			i++;
		if (isFlyerAttack1())
			i++;
		if (isFlyerAttack2())
			i++;
		if (isFlyerAttack3())
			i++;
		if (isFlyerArmor1())
			i++;
		if (isFlyerArmor2())
			i++;
		if (isFlyerArmor3())
			i++;
		if (isChitinousPlating())
			i++;
		for (EcState s : waypoints)
			i += s.getEstimatedActions();
		return i;
	}
        //TODO do we need to change tier 2 and 3 units
        public int getEstimatedActionsClean()
	{
		if (waypoints.size() > 0)
		{
			if( mergedWaypoints == null )
				mergedWaypoints = getMergedState();
			return mergedWaypoints.getEstimatedActions();
		}

		int i = requiredBases;
                
                for(Integer count: units.values()){
                    i+=count;
                }

                for(Integer count: buildings.values()){
                    i+= count;
                }
		i+= upgrades.size();
		for (EcState s : waypoints)
			i += s.getEstimatedActions();
		return i;
	}

	public EcState getMergedState()
	{
		EcState state = defaultDestination();
		for (EcState s : waypoints)
		{
			state.union(s);
		}
		state.union(this);
		return state;
	}

	public String timestamp()
	{
		return seconds / 60 + ":" + (seconds % 60 < 10 ? "0" : "") + seconds % 60;
	}

	public String toCompleteString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(messages.getString("AtTime")+": " + timestamp());
		sb.append("\n"+messages.getString("Minerals")+": " + (int) minerals + "\t"+messages.getString("Gas")+":      " + (int) gas + "\t"+messages.getString("Supply")+":   " + ((int) supplyUsed)
				+ "/" + supply() + "\t"+messages.getString("Larva")+": " + getLarva());
		appendBuildStuff(sb);
		return sb.toString();
	}

	public String toUnitsOnlyString()
	{
		StringBuilder sb = new StringBuilder();
		appendBuildStuff(sb);
		return sb.toString();
	}

	private void appendBuildStuff(StringBuilder sb)
	{
		append(sb, "Drone", getDrones());
		append(sb, "Overlord", getOverlords());
		append(sb, "Overseer", getOverseers());
		append(sb, "Queen", getQueens());
		append(sb, "Zergling", getZerglings());
		append(sb, "Baneling", getBanelings());
		append(sb, "Roach", getRoaches());
		append(sb, "Hydralisk", getHydralisks());
		append(sb, "Infestor", getInfestors());
		append(sb, "Mutalisk", getMutalisks());
		append(sb, "Corruptor", getCorruptors());
		append(sb, "Ultralisk", getUltralisks());
		append(sb, "Brood Lord", getBroodlords());

		append(sb, "Bases", requiredBases);
		append(sb, "Lair", getLairs());
		append(sb, "Hive", getHives());
		append(sb, "Gas Extractor", getGasExtractors());
		append(sb, "Spawning Pool", getSpawningPools());
		append(sb, "Baneling Nest", getBanelingNest());
		append(sb, "Roach Warren", getRoachWarrens());
		append(sb, "Hydralisk Den", getHydraliskDen());
		append(sb, "Infestation Pit", getInfestationPit());
		append(sb, "Spire", getSpire());
		append(sb, "Ultralisk Cavern", getUltraliskCavern());
		append(sb, "Greater Spire", getGreaterSpire());
		append(sb, "Evolution Chamber", getEvolutionChambers());
		append(sb, "Spine Crawler", getSpineCrawlers());
		append(sb, "Spore Crawler", getSporeCrawlers());
		append(sb, "Nydus Network", getNydusNetwork());
		append(sb, "Nydus Worm", getNydusWorm());

		append(sb, "Melee +1", isMelee1());
		append(sb, "Melee +2", isMelee2());
		append(sb, "Melee +3", isMelee3());
		append(sb, "Missile +1", isMissile1());
		append(sb, "Missile +2", isMissile2());
		append(sb, "Missile +3", isMissile3());
		append(sb, "Carapace +1", isArmor1());
		append(sb, "Carapace +2", isArmor2());
		append(sb, "Carapace +3", isArmor3());
		append(sb, "Flyer Attacks +1", isFlyerAttack1());
		append(sb, "Flyer Attacks +2", isFlyerAttack2());
		append(sb, "Flyer Attacks +3", isFlyerAttack3());
		append(sb, "Flyer Armor +1", isFlyerArmor1());
		append(sb, "Flyer Armor +2", isFlyerArmor2());
		append(sb, "Flyer Armor +3", isFlyerArmor3());
		append(sb, "Metabolic Boost", isMetabolicBoost());
		append(sb, "Adrenal Glands", isAdrenalGlands());
		append(sb, "Glial Reconstitution", isGlialReconstitution());
		append(sb, "Tunneling Claws", isTunnelingClaws());
		append(sb, "Burrow", isBurrow());
		append(sb, "Pneumatized Carapace", isPneumatizedCarapace());
		append(sb, "Ventral Sacs", isVentralSacs());
		append(sb, "Centrifugal Hooks", isCentrifugalHooks());
		append(sb, "Grooved Spines", isGroovedSpines());
		append(sb, "Neural Parasite", isNeuralParasite());
		append(sb, "Pathogen Glands", isPathogenGlands());
		append(sb, "Chitinous Plating", isChitinousPlating());
	}
        private void appendBuildStuffClean(StringBuilder sb)
	{
		for(Unit unit:units.keySet()){
                    append(sb, unit.getName(), units.get(unit));
                }

		append(sb, "Bases", requiredBases);

                for(Building building: buildings.keySet()){
                    append(sb, building.getName(), buildings.get(building));
                }
                //TODO clean that up
                for(Upgrade upgrade: upgrades){
                    append(sb, upgrade.getName(), true);
                }
	}

	private void append(StringBuilder sb, String name, boolean doit)
	{
		if (doit)
			sb.append("\n" + messages.getString(name.replace(' ','.')));
	}

	private void append(StringBuilder sb, String name, int count)
	{
		if (count > 0)
			sb.append("\n" + messages.getString(name.replace(' ','.')) + ": " + count);
	}

	private int currWaypoint = 0;
	public boolean waypointMissed(EcBuildOrder candidate)
	{
		if (waypoints == null || currWaypoint >= waypoints.size())
			return false;
		
		EcState s = waypoints.get(currWaypoint);
		if (candidate.seconds != s.targetSeconds)
			return false;

		if (s.isSatisfied(candidate)) {
			currWaypoint++;
			return false;
		}
			
		return true;
	}
	
	public int getCurrWaypointIndex(EcBuildOrder candidate) {
		if(currWaypoint == 0)
			return -1;

		EcState r = waypoints.get(currWaypoint - 1);
		if (r.targetSeconds == candidate.seconds)
			return currWaypoint - 1;
		return -1;
	}
	
	public int getWaypointActions(int index) {
		return waypoints.get(index).getEstimatedActions();
	}


        public HashSet<Upgrade> getUpgrades(){
            return upgrades;
        }

        public void AddUpgrade(Upgrade upgrade){
            upgrades.add(upgrade);
        }

        public void AddUnits(Unit unit, int number){
            units.put(unit, units.get(unit)+number);
        }

        public void RemoveUnits(Unit unit, int number){
            units.put(unit, units.get(unit)-number);
        }
        public void SetUnits(Unit unit, int number){
            units.put(unit, number);
        }

        public void AddBuilding(Building building){
            buildings.put(building, buildings.get(building)+1);
        }
        public void RemoveBuilding(Building building){
            buildings.put(building, buildings.get(building)-1);
        }
        public void RequireBuilding(Building building){
            if(buildings.get(building) <1 ){
                buildings.put(building, 1);
            }
        }

    void RequireUnit(Unit unit) {
            if(units.get(unit) <1 ){
                units.put(unit, 1);
            }
    }

        public HashMap<Building,Integer> getBuildings(){
            return buildings;
        }
        public HashMap<Unit,Integer> getUnits(){
            return units;
        }
        public void SetBuilding(Building building, int number){
            buildings.put(building, number);
        }
    /**
     * @return the metabolicBoost
     */
    public boolean isMetabolicBoost() {
        return upgrades.contains(UpgradeLibrary.MetabolicBoost);
    }

    /**
     * @return the adrenalGlands
     */
    public boolean isAdrenalGlands() {
        return upgrades.contains(UpgradeLibrary.AdrenalGlands);
    }

    /**
     * @return the glialReconstitution
     */
    public boolean isGlialReconstitution() {
        return upgrades.contains(UpgradeLibrary.GlialReconstitution);
    }

    /**
     * @return the tunnelingClaws
     */
    public boolean isTunnelingClaws() {
        return upgrades.contains(UpgradeLibrary.TunnelingClaws);
    }

    /**
     * @return the burrow
     */
    public boolean isBurrow() {
        return upgrades.contains(UpgradeLibrary.Burrow);
    }

    /**
     * @return the pneumatizedCarapace
     */
    public boolean isPneumatizedCarapace() {
        return upgrades.contains(UpgradeLibrary.PneumatizedCarapace);
    }

    /**
     * @return the ventralSacs
     */
    public boolean isVentralSacs() {
        return upgrades.contains(UpgradeLibrary.VentralSacs);
    }


    /**
     * @return the centrifugalHooks
     */
    public boolean isCentrifugalHooks() {
        return upgrades.contains(UpgradeLibrary.CentrifugalHooks);
    }


    /**
     * @return the melee1
     */
    public boolean isMelee1() {
        return upgrades.contains(UpgradeLibrary.Melee1);
    }

    /**
     * @return the melee2
     */
    public boolean isMelee2() {
        return upgrades.contains(UpgradeLibrary.Melee2);
    }

    /**
     * @return the melee3
     */
    public boolean isMelee3() {
        return upgrades.contains(UpgradeLibrary.Melee3);
    }

    /**
     * @return the missile1
     */
    public boolean isMissile1() {
        return upgrades.contains(UpgradeLibrary.Missile1);
    }

    /**
     * @return the missile2
     */
    public boolean isMissile2() {
        return upgrades.contains(UpgradeLibrary.Melee2);
    }

    /**
     * @return the missile3
     */
    public boolean isMissile3() {
        return upgrades.contains(UpgradeLibrary.Missile3);
    }


    /**
     * @return the armor1
     */
    public boolean isArmor1() {
        return upgrades.contains(UpgradeLibrary.Armor1);
    }

    /**
     * @return the armor2
     */
    public boolean isArmor2() {
        return upgrades.contains(UpgradeLibrary.Armor2);
    }

    /**
     * @return the armor3
     */
    public boolean isArmor3() {
        return upgrades.contains(UpgradeLibrary.Armor3);
    }

    /**
     * @return the groovedSpines
     */
    public boolean isGroovedSpines() {
        return upgrades.contains(UpgradeLibrary.GroovedSpines);
    }

    /**
     * @return the neuralParasite
     */
    public boolean isNeuralParasite() {
        return upgrades.contains(UpgradeLibrary.NeuralParasite);
    }

    /**
     * @return the pathogenGlands
     */
    public boolean isPathogenGlands() {
        return upgrades.contains(UpgradeLibrary.PathogenGlands);
    }
    /**
     * @return the flyerAttack1
     */
    public boolean isFlyerAttack1() {
        return upgrades.contains(UpgradeLibrary.FlyerAttacks1);
    }

    /**
     * @return the flyerAttack2
     */
    public boolean isFlyerAttack2() {
        return upgrades.contains(UpgradeLibrary.FlyerAttacks2);
    }

    /**
     * @return the flyerAttack3
     */
    public boolean isFlyerAttack3() {
        return upgrades.contains(UpgradeLibrary.FlyerAttacks3);
    }

    /**
     * @return the flyerArmor1
     */
    public boolean isFlyerArmor1() {
        return upgrades.contains(UpgradeLibrary.FlyerArmor1);
    }


    /**
     * @return the flyerArmor2
     */
    public boolean isFlyerArmor2() {
        return upgrades.contains(UpgradeLibrary.FlyerArmor2);
    }

    /**
     * @return the flyerArmor3
     */
    public boolean isFlyerArmor3() {
        return upgrades.contains(UpgradeLibrary.FlyerArmor3);
    }


    /**
     * @return the chitinousPlating
     */
    public boolean isChitinousPlating() {
        return upgrades.contains(UpgradeLibrary.ChitinousPlating);
    }

    /**
     * @return the larva
     */
    public int getLarva() {
        return larva;
    }

    /**
     * @param larva the larva to set
     */
    public void setLarva(int larva) {
        this.larva = larva;
    }

    /**
     * @return the drones
     */
    public int getDrones() {
        return units.get(UnitLibrary.Drone);
    }

    /**
     * @return the overlords
     */
    public int getOverlords() {
        return units.get(UnitLibrary.Overlord);
    }

    /**
     * @return the overseers
     */
    public int getOverseers() {
        return units.get(UnitLibrary.Overseer);
    }

    /**
     * @return the zerglings
     */
    public int getZerglings() {
        return units.get(UnitLibrary.Zergling);
    }

    /**
     * @return the banelings
     */
    public int getBanelings() {
        return units.get(UnitLibrary.Baneling);
    }

    /**
     * @return the roaches
     */
    public int getRoaches() {
        return units.get(UnitLibrary.Roach);
    }

    /**
     * @return the mutalisks
     */
    public int getMutalisks() {
        return units.get(UnitLibrary.Mutalisk);
    }

    /**
     * @return the infestors
     */
    public int getInfestors() {
        return units.get(UnitLibrary.Infestor);
    }

    /**
     * @return the queens
     */
    public int getQueens() {
        return units.get(UnitLibrary.Queen);
    }

    /**
     * @return the hydralisks
     */
    public int getHydralisks() {
        return units.get(UnitLibrary.Hydralisk);
    }

    /**
     * @return the corruptors
     */
    public int getCorruptors() {
        return units.get(UnitLibrary.Corruptor);
    }

    /**
     * @return the ultralisks
     */
    public int getUltralisks() {
        return units.get(UnitLibrary.Ultralisk);
    }
    /**
     * @return the broodlords
     */
    public int getBroodlords() {
        return units.get(UnitLibrary.Broodlord);
    }

    /**
     * @return the hatcheries
     */
    public int getHatcheries() {
        return buildings.get(BuildingLibrary.Hatchery);
    }

    /**
     * @return the lairs
     */
    public int getLairs() {
        return buildings.get(BuildingLibrary.Lair);
    }

    /**
     * @return the hives
     */
    public int getHives() {
        return buildings.get(BuildingLibrary.Hive);
    }

    /**
     * @return the spawningPools
     */
    public int getSpawningPools() {
        return buildings.get(BuildingLibrary.SpawningPool);
    }

    /**
     * @return the evolutionChambers
     */
    public int getEvolutionChambers() {
        return buildings.get(BuildingLibrary.EvolutionChamber);
    }

    /**
     * @return the roachWarrens
     */
    public int getRoachWarrens() {
        return buildings.get(BuildingLibrary.RoachWarren);
    }

    /**
     * @return the hydraliskDen
     */
    public int getHydraliskDen() {
        return buildings.get(BuildingLibrary.HydraliskDen);
    }

    /**
     * @return the banelingNest
     */
    public int getBanelingNest() {
        return buildings.get(BuildingLibrary.BanelingNest);
    }

    /**
     * @return the infestationPit
     */
    public int getInfestationPit() {
        return buildings.get(BuildingLibrary.InfestationPit);
    }

    /**
     * @return the greaterSpire
     */
    public int getGreaterSpire() {
        return buildings.get(BuildingLibrary.GreaterSpire);
    }

    /**
     * @return the ultraliskCavern
     */
    public int getUltraliskCavern() {
        return buildings.get(BuildingLibrary.UltraliskCavern);
    }

    /**
     * @return the gasExtractors
     */
    public int getGasExtractors() {
        return buildings.get(BuildingLibrary.Extractor);
    }

    /**
     * @return the spire
     */
    public int getSpire() {
        return buildings.get(BuildingLibrary.Spire);
    }

    /**
     * @return the spineCrawlers
     */
    public int getSpineCrawlers() {
        return buildings.get(BuildingLibrary.SpineCrawler);
    }

    /**
     * @return the sporeCrawlers
     */
    public int getSporeCrawlers() {
        return buildings.get(BuildingLibrary.SporeCrawler);
    }
    /**
     * @return the nydusNetwork
     */
    public int getNydusNetwork() {
        return buildings.get(BuildingLibrary.NydusNetwork);
    }

    /**
     * @return the nydusWorm
     */
    public int getNydusWorm() {
        return buildings.get(BuildingLibrary.NydusWorm);
    }

}
