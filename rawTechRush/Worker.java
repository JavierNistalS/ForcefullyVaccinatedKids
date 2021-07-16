package rawTechRush;

import aic2021.user.*;

public class Worker extends MyUnit {

    Worker(UnitController uc){
        super(uc);
    }

    boolean torchLighted = false;

    int farmCount, quarryCount, sawmillCount;

    void playRound(){
        moveRandom();
        if(farmCount < 1 && trySpawnInValid(UnitType.FARM))
            farmCount++;
        if(quarryCount < 1 && trySpawnInValid(UnitType.QUARRY))
            quarryCount++;
        if(sawmillCount < 1 && trySpawnInValid(UnitType.SAWMILL))
            sawmillCount++;
    }

    void MoveCloseToBase()
    {

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
