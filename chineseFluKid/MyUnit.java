package chineseFluKid;

import aic2021.user.*;

public abstract class MyUnit {

    Direction[] dirs = Direction.values();
    Direction[] diagDirs = {Direction.NORTHWEST, Direction.NORTHEAST, Direction.SOUTHWEST, Direction.SOUTHEAST};

    public Location baseLocation, enemyBaseLocation;
    UnitController uc;

    boolean genevaSuggestion = true;

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
    boolean tryLightTorch(){
        if (uc.canLightTorch()){
            uc.lightTorch();
            return true;
        }
        return false;
    }

    boolean trySpawnUnit(UnitType type, Direction dir){
        Location loc = uc.getLocation().add(dir);
        if (uc.canSpawn(type, dir) && (!uc.canSenseLocation(loc) || !uc.hasTrap(loc)) && (enemyBaseLocation == null || enemyBaseLocation.distanceSquared(loc) > 18 || uc.isObstructed(enemyBaseLocation, loc))){
            uc.spawn(type, dir);
            return true;
        }
        return false;
    }

    boolean trySpawnUnit(UnitType type) {
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

    void identifyEnemyBase(){
        if (enemyBaseLocation != null){
            UnitInfo[] units = uc.senseUnits(uc.getTeam().getOpponent());
            for (UnitInfo ui : units){
                if (ui.getType() == UnitType.BASE){
                    enemyBaseLocation = ui.getLocation();
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

    int totalResourcesAtLocation(Location loc) {
        if(uc.canSenseLocation(loc) && uc.senseUnitAtLocation(loc) == null && !uc.hasTrap(loc)) {
            ResourceInfo[] ris = uc.senseResourceInfo(loc);
            int total = 0;

            for(ResourceInfo ri : ris)
                 if(ri != null)
                     total += ri.amount;

             return total;
        }
        return 0;
    }

    void debugObstructed(){
        for (Location loc : uc.getVisibleLocations()){
            if (isObstructedNice(uc.getLocation(), loc)){
                uc.drawPointDebug(loc, 0,0,0);
            }
            else{
                uc.drawPointDebug(loc, 255, 255, 255);
            }
        }
    }

    boolean isObstructedNice(Location p0, Location p1) {
        int dx = p1.x-p0.x, dy = p1.y-p0.y;
        int nx = Math.abs(dx), ny = Math.abs(dy);
        int sign_x = dx > 0? 1 : -1, sign_y = dy > 0? 1 : -1;

        int px = p0.x, py = p0.y;

        for (int ix = 0, iy = 0; ix < nx || iy < ny;) {
            int decision = (1 + 2*ix) * ny - (1 + 2*iy) * nx;
            if (decision == 0) {
                // next step is diagonal
                px += sign_x;
                py += sign_y;
                ix++;
                iy++;
            } else if (decision < 0) {
                // next step is horizontal
                px += sign_x;
                ix++;
            } else {
                // next step is vertical
                py += sign_y;
                iy++;
            }

            Location loc = new Location(px, py);
            if(uc.isOutOfMap(loc) || (uc.canSenseLocation(loc) && uc.hasMountain(loc)))
                return true;
        }
        return false;
    }
}
