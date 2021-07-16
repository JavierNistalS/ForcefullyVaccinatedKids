package rawTechRush;

import aic2021.user.*;

public abstract class MyUnit {

    Direction[] dirs = Direction.values();

    Location baseLocation;

    UnitController uc;

    MyUnit(UnitController uc){
        this.uc = uc;
    }

    abstract void playRound();

    boolean spawnRandom(UnitType t){
        for (Direction dir : dirs){
            if (uc.canSpawn(t, dir)){
                uc.spawn(t, dir);
                return true;
            }
        }
        return false;
    }

    boolean moveRandom(){
        int tries = 10;
        while (uc.canMove() && tries-- > 0){
            int random = (int)(uc.getRandomDouble()*8);
            if (uc.canMove(dirs[random])){
                uc.move(dirs[random]);
                return true;
            }
        }
        return false;
    }

    boolean tryLightTorch(){
        if (uc.canLightTorch()){
            uc.lightTorch();
            return true;
        }
        return false;
    }

    boolean trySpawnUnit(UnitType type, Direction dir){
        if (uc.canSpawn(type, dir)){
            uc.spawn(type, dir);
            return true;
        }
        return false;
    }

    boolean trySpawnUnit(UnitType type){
        for (Direction dir : dirs){
            if (trySpawnUnit(type, dir))
                return true;
        }
        return false;
    }

}
