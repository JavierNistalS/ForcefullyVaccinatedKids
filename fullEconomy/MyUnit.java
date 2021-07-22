package fullEconomy;

import aic2021.user.*;

public abstract class MyUnit {

    Direction[] dirs = Direction.values();
    Direction[] diagDirs = {Direction.NORTHWEST, Direction.NORTHEAST, Direction.SOUTHWEST, Direction.SOUTHEAST};

    Location baseLocation, enemyBaseLocation;
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

    boolean moveRandom()
    {
        int tries = 10;
        while (uc.canMove() && tries-- > 0){
            int random = (int)(uc.getRandomDouble()*8);
            if (uc.canMove(dirs[random]))
            {
                uc.move(dirs[random]);
                return true;
            }
        }
        return false;
    }
    boolean moveRandomDiagonal()
    {
        int tries = 10;
        while (uc.canMove() && tries-- > 0){
            int random = (int)(uc.getRandomDouble()*diagDirs.length);
            if (uc.canMove(diagDirs[random])){
                uc.move(diagDirs[random]);
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
    boolean generalAttack(){
        int lessHpAggro = 100000000;
        int lessHpNonAggro = 100000000;
        Location best = null;
        UnitInfo[] units = uc.senseUnits(uc.getTeam().getOpponent());
        for (UnitInfo u : units){
            int hp = u.getHealth();
            if (u.getType().attack > 0){
                if (lessHpAggro > hp){
                    lessHpAggro = hp;
                    if (uc.canAttack(u.getLocation()))
                        best = u.getLocation();
                }
            }
            else{
                if (lessHpAggro == 100000000 && hp < lessHpNonAggro){
                    lessHpNonAggro = hp;
                    if (uc.canAttack(u.getLocation()))
                        best = u.getLocation();
                }
            }
        }
        if (best != null) {
            uc.attack(best);
            return true;
        }
        return false;
    }

    boolean tryThrowTorch(Location loc){
        if (uc.canThrowTorch(loc)){
            uc.throwTorch(loc);
            return true;
        }
        return false;
    }

    boolean randomTorchThrow(){
        int k = 10;
        while (uc.getInfo().getTorchRounds() > 0 && k-- > 0){
            uc.println(dirs.length);
            Direction dir = dirs[(int)(uc.getRandomDouble()*8)];
            Location loc = uc.getLocation().add(dir);
            if (tryThrowTorch(loc))
                return true;
        }
        return false;
    }

    void identifyBase(){
        if (baseLocation == null){
            UnitInfo[] units = uc.senseUnits(uc.getTeam());
            for (UnitInfo ui : units){
                if (ui.getType() == UnitType.BASE){
                    baseLocation = ui.getLocation();
                    break;
                }
            }
        }
    }

    void sustainTorch(){
        uc.println("sustaining torch");

        int torchLife = uc.getInfo().getTorchRounds();
        if ((torchLife < 4 && randomTorchThrow()) || torchLife < 10)
            tryLightTorch();
    }
}
