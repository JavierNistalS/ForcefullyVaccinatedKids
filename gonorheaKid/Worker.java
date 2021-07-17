package gonorheaKid;

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
        //pathfinding.WanderAround(baseLocation, 3);

        if (!hasBuiltBarracks){
            if (uc.getInfo().getTorchRounds() < 10 && trySpawnUnit(UnitType.BARRACKS)) {
                hasBuiltBarracks = true;
            }
            if (enemyBaseLocation != null && trySpawnUnit(UnitType.BARRACKS)) {
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
