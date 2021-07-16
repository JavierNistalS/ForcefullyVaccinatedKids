package gonorheaKid;

import aic2021.user.*;

public class Worker extends MyUnit {

    Worker(UnitController uc){
        super(uc);
    }

    boolean hasBuiltBarracks = false;

    void playRound(){
        int[] info = uc.readSmokeSignals();
        if (info.length > 0){
            enemyBaseLocation = new Location(info[0]/100000, info[0]%100000);
        }
        if (uc.getInfo().getTorchRounds() < 100){
            tryLightTorch();
        }
        if (!hasBuiltBarracks){
            if (uc.getInfo().getTorchRounds() < 10){
                if (trySpawnUnit(UnitType.BARRACKS))
                    hasBuiltBarracks = true;
            }
            if (enemyBaseLocation != null && enemyBaseLocation.distanceSquared(uc.getLocation()) < 81){
                if (trySpawnUnit(UnitType.BARRACKS))
                    hasBuiltBarracks = true;
            }
            if (enemyBaseLocation != null && enemyBaseLocation.distanceSquared(uc.getLocation()) >= 81){
                uc.println("moving to " + enemyBaseLocation);
                uc.drawLineDebug(uc.getLocation(), enemyBaseLocation, 0, 0, 0);
                move3(uc.getLocation().directionTo(enemyBaseLocation));
            }
        }
    }

    boolean isValid(Location loc){
        return (loc.x + loc.y)%2 == 0;
    }

    boolean trySpawnInValid(UnitType type){
        for (Direction dir : dirs){
            if (isValid(uc.getLocation().add(dir)) && trySpawnUnit(type, dir))
                return true;
        }
        return false;
    }
}
