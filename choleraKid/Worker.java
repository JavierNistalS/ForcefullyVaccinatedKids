package choleraKid;

import aic2021.user.*;

public class Worker extends MyUnit {

    Worker(UnitController uc)
    {
        super(uc);
        pathfinding = new Pathfinding(uc);
    }

    boolean hasBuiltBarracks = false;
    Pathfinding pathfinding;

    void playRound()
    {
        ReadSmokeSignals();

        if (!hasBuiltBarracks){
            if (trySpawnUnit(UnitType.BARRACKS)) {
                hasBuiltBarracks = true;
            }
        }
    }

    boolean isValid(Location loc){
        return (loc.x + loc.y) % 2 == 0;
    }

    boolean trySpawnInValid(UnitType type){
        for (Direction dir : dirs){
            if (isValid(uc.getLocation().add(dir)) && trySpawnUnit(type, dir))
                return true;
        }
        return false;
    }
}
