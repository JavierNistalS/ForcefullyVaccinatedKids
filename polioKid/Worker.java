package polioKid;

import aic2021.user.*;

public class Worker extends MyUnit {

    Worker(UnitController uc){
        super(uc);
    }

    boolean hasBuiltBarracks = false;

    void playRound(){
        ReadSmokeSignals();

        if (uc.getInfo().getTorchRounds() < 100){
            tryLightTorch();
        }
        if (!hasBuiltBarracks){
            if (uc.getInfo().getTorchRounds() < 10 && trySpawnUnit(UnitType.BARRACKS)) {
                hasBuiltBarracks = true;
            }
            if (enemyBaseLocation != null && enemyBaseLocation.distanceSquared(uc.getLocation()) < 100 && trySpawnUnit(UnitType.BARRACKS)) {
                hasBuiltBarracks = true;
            }
            if (enemyBaseLocation != null && enemyBaseLocation.distanceSquared(uc.getLocation()) >= 100) {
                uc.println("moving to " + enemyBaseLocation);
                uc.drawLineDebug(uc.getLocation(), enemyBaseLocation, 0, 0, 0);
                move3(uc.getLocation().directionTo(enemyBaseLocation));
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
