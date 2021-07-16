package polioKid;

import aic2021.user.*;

public abstract class MyUnit {

    Direction[] dirs = Direction.values();

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

    boolean trySmokeSignal(int x){
        uc.println("trying to smoke " + x);
        if (uc.canMakeSmokeSignal()){
            uc.makeSmokeSignal(x);
            uc.println("smoke done");
            return true;
        }
        return false;
    }

    boolean tryMove(Direction dir){
        if (uc.canMove(dir)){
            uc.move(dir);
            return true;
        }
        return false;
    }

    boolean move3(Direction dir){
        return (tryMove(dir) || tryMove(dir.rotateLeft()) || tryMove(dir.rotateRight()));
    }

    boolean move5(Direction dir){
        return (tryMove(dir) || tryMove(dir.rotateLeft()) || tryMove(dir.rotateRight()) || tryMove(dir.rotateLeft().rotateLeft()) || tryMove(dir.rotateRight().rotateRight()));
    }

    boolean tryAttack(Location loc){
        if (uc.canAttack(loc)){
            uc.attack(loc);
            return true;
        }
        return false;
    }

    void generalAttack(){
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
        if (best != null)
            uc.attack(best);
    }
}
