package typhusKid;

import aic2021.user.*;

public class Worker extends MyUnit {

    Worker(UnitController uc){
        super(uc);
        pathfinding = new Pathfinding(uc);
    }

    boolean torchLighted = false;
    Location baseLocation;
    Pathfinding pathfinding;

    int farmCount, quarryCount, sawmillCount;
    boolean orbitDirection = false;
    int lastValid = 0;
    int barrackCount = 0;

    void playRound()
    {
        if(enemyBaseLocation == null || Math.abs(baseLocation.x - enemyBaseLocation.x) < 50 || Math.abs(baseLocation.y - enemyBaseLocation.y) < 50)
            ReadSmokeSignals();
        UnitInfo[] units = uc.senseUnits(uc.getTeam());
        for(UnitInfo unit : units)
            if(unit.getType() == UnitType.BASE)
                baseLocation = unit.getLocation();

        if (sawmillCount >= 2 && farmCount >= 1 && quarryCount >= 1 && barrackCount <= 0 && (enemyBaseLocation != null || enemyStructureLocation != null)){
            if (trySpawnUnit(UnitType.BARRACKS))
                barrackCount++;
        }
        else
        {
            if(sawmillCount < 2 && trySpawnInValid(UnitType.SAWMILL))
                sawmillCount++;
            if(farmCount < 1 && trySpawnInValid(UnitType.FARM))
                farmCount++;
            if(quarryCount < 1 && trySpawnInValid(UnitType.QUARRY))
                quarryCount++;

            if(sawmillCount < 5 && uc.getResource(Resource.WOOD) > 300 && trySpawnInValid(UnitType.SAWMILL))
                sawmillCount++;
            if(farmCount < 4 && uc.getResource(Resource.FOOD) > 200 && trySpawnInValid(UnitType.FARM) )
                farmCount++;
            if(quarryCount < 5 && uc.getResource(Resource.STONE) > 200 && trySpawnInValid(UnitType.QUARRY) )
                quarryCount++;
        }

        if(uc.canMove()) {

            UnitInfo[] enemyUnits = uc.senseUnits(uc.getTeam().getOpponent());
            if(enemyUnits.length > 0)
                pathfinding.pathfindTo(baseLocation);

            if(baseLocation == null || uc.getLocation().distanceSquared(baseLocation) < 64) {
                if (isValid(uc.getLocation()))
                    moveRandomDiagonal();
                if (uc.canMove())
                    moveRandom();
            }
            else
                pathfinding.pathfindTo(baseLocation);
        }


    }

    boolean isValid(Location loc)
    {
        return ((loc.x + loc.y) % 2) == 0;
        //return baseLocation != null && loc.distanceSquared (baseLocation) > 1 || (uc.getRound() > 400 && lastValid + 3< uc.getRound());
    }

    boolean trySpawnInValid(UnitType type)
    {
        for (Direction dir : dirs){
            if (isValid(uc.getLocation().add(dir)) && trySpawnUnit(type, dir))
            {
                lastValid = uc.getRound();
                return true;
            }

        }
        return false;
    }
}
