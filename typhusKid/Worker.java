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

        if (sawmillCount == 4 && farmCount == 3 && quarryCount == 3 && enemyBaseLocation != null && barrackCount == 0){
            if (enemyBaseLocation.distanceSquared(uc.getLocation()) < 150 && trySpawnUnit(UnitType.BARRACKS))
                barrackCount++;
            pathfinding.wanderAround(enemyBaseLocation, 64);
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

        if(sawmillCount < 4 && trySpawnInValid(UnitType.SAWMILL))
            sawmillCount++;
        if(farmCount < 3 && trySpawnInValid(UnitType.FARM))
            farmCount++;
        if(quarryCount < 3 && trySpawnInValid(UnitType.QUARRY))
            quarryCount++;
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
