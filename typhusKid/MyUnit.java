package typhusKid;

import aic2021.user.*;

public abstract class MyUnit {

    int HASH = 13377701;
    int KEY = 17;
    int KEY_MOD = 1_0000_0000;

    Direction[] dirs = Direction.values();
    Direction[] diagDirs = {Direction.NORTHWEST, Direction.NORTHEAST, Direction.SOUTHWEST, Direction.SOUTHEAST};

    Location baseLocation;
    Location enemyBaseLocation;

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

    boolean trySmokeSignal(int x){
        uc.println("trying to smoke " + x);
        if (uc.canMakeSmokeSignal()){
            uc.makeSmokeSignal(x);
            uc.println("smoke done");
            return true;
        }
        return false;
    }

    boolean SendEnemyBaseSignal(Location location)
    {
        int info = location.x + location.y * 10000;
        info += KEY_MOD * KEY;
        return trySmokeSignal(info ^ HASH);
    }

    void ReadSmokeSignals()
    {
        if(uc.canReadSmokeSignals())
        {
            int[] smokes = uc.readSmokeSignals();
            for(int smoke : smokes)
            {
                int info = smoke ^ HASH;
                if((info / KEY_MOD) == KEY)
                {
                    uc.println("allied smoke: " + smoke);
                    info %= KEY_MOD;
                    enemyBaseLocation = new Location(info % 10000, info / 10000);
                }
                else
                    uc.println("enemy smoke: " + smoke);
            }
        }
    }
}
