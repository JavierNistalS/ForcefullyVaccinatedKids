package rawTechRush;

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

//        if(uc.canMove() && baseLocation != null)
//        {
//            Direction baseDir = uc.getLocation().directionTo(baseLocation);
//            Direction dir1 = orbitDirection ? baseDir.rotateLeft() : baseDir.rotateRight();
//            Direction dir2 = orbitDirection ? baseDir.rotateRight() : baseDir.rotateLeft();
//
//            if(uc.canMove(baseDir))
//                uc.move(baseDir);
//            else if(uc.canMove(dir1))
//                uc.move(dir1);
//            else if(uc.canMove(dir2))
//            {
//                uc.move(dir2);
//                orbitDirection = !orbitDirection;
//            }
//            else
//                moveRandom();
//        }

        if(uc.canMove()) {
            if(baseLocation == null || uc.getLocation().distanceSquared(baseLocation) < 400) {
                if (isValid(uc.getLocation()))
                    moveRandomDiagonal();
                if (uc.canMove())
                    moveRandom();
            }
            else
                pathfinding.pathfindTo(baseLocation);
        }

        if(sawmillCount < 6 && trySpawnInValid(UnitType.SAWMILL))
            sawmillCount++;
        if(farmCount < 5 && trySpawnInValid(UnitType.FARM))
            farmCount++;
        if(quarryCount < 5 && trySpawnInValid(UnitType.QUARRY))
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
