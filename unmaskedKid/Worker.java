package unmaskedKid;

import aic2021.user.*;

public class Worker extends MyUnit {

    Worker(UnitController uc){
        super(uc);
    }

    boolean torchLighted = false;

    void playRound(){
        UnitInfo[] units = uc.senseUnits();
        for (UnitInfo u : units){
            if (u.getType() == UnitType.BASE && u.getTeam() == uc.getTeam())
                baseLocation = u.getLocation();
        }
        UnitInfo myInfo = uc.getInfo();
        if (!torchLighted && myInfo.getTorchRounds() <= 0){
            tryLightTorch();
        }
        moveRandom();
        if (uc.getRound() < 250 && uc.getLocation().distanceSquared(baseLocation) > 16) {
            trySpawnInValid(UnitType.FARM);
            trySpawnInValid(UnitType.QUARRY);
            trySpawnInValid(UnitType.SAWMILL);
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
