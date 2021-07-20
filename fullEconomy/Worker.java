package fullEconomy;

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

    void playRound()
    {
        UnitInfo[] units = uc.senseUnits(uc.getTeam());
        for(UnitInfo unit : units)
            if(unit.getType() == UnitType.BASE)
                baseLocation = unit.getLocation();


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

        if((sawmillCount < 4 || (uc.getResource(Resource.STONE) > 1100 && uc.getResource(Resource.FOOD) > 1100)) && trySpawnInValid(UnitType.SAWMILL))
            sawmillCount++;
        if((farmCount < 3 || (uc.getResource(Resource.STONE) > 1100 && uc.getResource(Resource.WOOD) > 1100)) && trySpawnInValid(UnitType.FARM))
            farmCount++;
        if((quarryCount < 3 || (uc.getResource(Resource.FOOD) > 1100 && uc.getResource(Resource.WOOD) > 1100)) && trySpawnInValid(UnitType.QUARRY))
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
