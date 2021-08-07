package wolfGang;

import aic2021.user.*;

public abstract class MyUnit {

    Direction[] dirs = Direction.values();
    Direction[] diagDirs = {Direction.NORTHWEST, Direction.NORTHEAST, Direction.SOUTHWEST, Direction.SOUTHEAST};

    public Location baseLocation, enemyBaseLocation;
    UnitController uc;

    boolean genevaSuggestion = false;

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

    void readRocks(){
        if (baseLocation != null && enemyBaseLocation == null){
            for (Direction dir : dirs){
                Location loc = baseLocation.add(dir);
                if (uc.canRead(loc)){
                    int x = uc.read(loc);
                    enemyBaseLocation = baseLocation.add(x/100-49, x%100-49);
                    uc.println("read " + x);
                    uc.println("location " + enemyBaseLocation);
                }
            }
        }
    }

    boolean trySpawnUnit(UnitType type) {
        for (Direction dir : dirs){
            if (trySpawnUnit(type, dir))
                return true;
        }
        return false;
    }
    boolean generalAttack(){
        float bestScore = 0;
        Location best = null;
        UnitInfo[] units = uc.senseUnits(uc.getTeam().getOpponent());
        for (UnitInfo u : units){
            int hp = u.getHealth();
            float score = u.getAttack() / u.getType().getAttackCooldown() / ((u.getHealth()+ uc.getType().getAttack() - 1 )/ uc.getType().getAttack());
            if (score >= bestScore && uc.canAttack(u.getLocation())){
                bestScore = score;
                best = u.getLocation();
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

    void identifyEnemyBase() {
        if (enemyBaseLocation == null) {
            UnitInfo[] units = uc.senseUnits(uc.getTeam().getOpponent());
            for (UnitInfo ui : units){
                if (ui.getType() == UnitType.BASE){
                    enemyBaseLocation = ui.getLocation();
                    break;
                }
            }
        }
    }

    boolean sustainTorch() {
        int torchLife = uc.getInfo().getTorchRounds();
        if (torchLife < 4 && uc.getResource(Resource.WOOD) > uc.getRound()*0.3) {
            if (!tryLightTorch() && randomTorchThrow())
                tryLightTorch();
        }

        return uc.getInfo().getTorchRounds() > 0;
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
        if(p0.distanceSquared(p1) <= 2)
            return false;

        if(uc.canSenseLocation(p0) && uc.canSenseLocation(p1))
            return uc.isObstructed(p0, p1);


        int dx = p1.x-p0.x, dy = p1.y-p0.y;
        int nx = Math.abs(dx), ny = Math.abs(dy);
        int sign_x = dx > 0 ? 1 : -1, sign_y = dy > 0 ? 1 : -1;

        int px = p0.x, py = p0.y;

        int decision = ny - nx; // (1 + 2*ix) * ny - (1 + 2*iy) * nx;
        int nx2 = 2*nx, ny2 = 2*ny;

        for (int ix = 0, iy = 0; ix < nx || iy < ny;) {

            // int decision = (1 + 2*ix) * ny - (1 + 2*iy) * nx;

            if(decision <= 0) {
                px += sign_x;
                ix++;
                decision += ny2;

            }

            if(decision >= 0) {
                py += sign_y;
                iy++;
                decision -= nx2;
            }

//            if (decision == 0) {
//                // next step is diagonal
//                px += sign_x;
//                py += sign_y;
//                ix++;
//                iy++;
//            } else if (decision < 0) {
//                // next step is horizontal
//                px += sign_x;
//                ix++;
//            } else {
//                // next step is vertical
//                py += sign_y;
//                iy++;
//            }

            Location loc = new Location(px, py);
            if(uc.isOutOfMap(loc) || (uc.canSenseLocation(loc) && uc.hasMountain(loc)))
                return true;
        }
        return false;
    }

    public boolean canBuildUnitWithMargin(UnitType unitType, int foodMargin, int woodMargin, int stoneMargin) {
        return uc.getResource(Resource.FOOD) >= foodMargin + unitType.foodCost
            && uc.getResource(Resource.WOOD) >= woodMargin + unitType.woodCost
            && uc.getResource(Resource.STONE) >= stoneMargin + unitType.stoneCost;
    }

    public boolean canResearchWithMargin(Technology tech, int foodMargin, int woodMargin, int stoneMargin) {
        return uc.getResource(Resource.FOOD) >= foodMargin + tech.getFoodCost()
            && uc.getResource(Resource.WOOD) >= woodMargin + tech.getWoodCost()
            && uc.getResource(Resource.STONE) >= stoneMargin + tech.getStoneCost();
    }
}
