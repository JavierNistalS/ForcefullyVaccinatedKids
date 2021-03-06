package finalBot;

import aic2021.user.*;

public class Spearman extends MyUnit {

    Spearman(UnitController uc){
        super(uc);
        pathfinding = new Pathfinding(uc, this);
        comms = new Communications(uc);
        exploration = new Exploration(uc, 6, 8);
        minSpearmanID = uc.getInfo().getID();
        UnitInfo[] enemies = uc.senseUnits(uc.getTeam().getOpponent());
        if (enemies.length >= 2) {
            stayDefensive = true;
        }
    }
    Pathfinding pathfinding;
    Communications comms;
    Exploration exploration;
    int minSpearmanID;

    boolean rotateRight = true;
    boolean stayDefensive = false;
    boolean reinforceRequested = false;

    void playRound() {
        /*if (uc.getRound() == 300)
            debugObstructed();*/
        identifyBase();
        identifyEnemyBase();
        readSmokeSignals();
        readRocks();
        generalAttack();
        updateMinSpearmanID();
        exploration.updateChunks();

        if (uc.getInfo().getID() == minSpearmanID || stayDefensive) {

            int baseBuildingSpots = 0;

            for(Direction dir : dirs) {
                if(dir == Direction.ZERO)
                    continue;

                Location loc = baseLocation.add(dir);
                if(!uc.canSenseLocation(loc))
                    continue;

                int dist = loc.distanceSquared(uc.getLocation());
                if(dist > 0 && (uc.hasMountain(loc) || (!uc.hasResearched(Technology.RAFTS, uc.getTeam()) && uc.hasWater(loc))))
                    continue;

                UnitInfo unit = uc.senseUnitAtLocation(loc);
                if(unit != null && unit.getType().isStructure())
                    continue;

                baseBuildingSpots++;
                uc.drawPointDebug(loc, 125, 0, 255);
            }
            uc.println("baseBuildingSpots: " + baseBuildingSpots);
            if(baseBuildingSpots > 1)
                camperMicro();
            else {
                stayDefensive = false;
                minSpearmanID = -1;
                idleMicro();
            }
        }
        else {
            idleMicro();
        }

        generalAttack();
    }

    void updateMinSpearmanID() {
        if(minSpearmanID == uc.getInfo().getID()) {
            UnitInfo[] units = uc.senseUnits(uc.getTeam());
            for (UnitInfo unit : units) {
                if (unit.getType() == UnitType.SPEARMAN)
                    minSpearmanID = Math.min(minSpearmanID, unit.getID());
            }
        }
    }

    boolean willAttack(Direction dir, Location loc) {
        boolean obstructed;
        if(uc.canSenseLocation(uc.getLocation().add(dir)))
            obstructed = uc.isObstructed(uc.getLocation().add(dir), loc);
        else
            obstructed = isObstructedNice(uc.getLocation().add(dir), loc);

        return !obstructed;
    }

    void idleMicro() {
        uc.println("idleMicro");
        if(uc.canMove()) {
            UnitInfo[] units = uc.senseUnits(uc.getTeam().getOpponent());

            for (UnitInfo ui : units) {
                if (ui.getType() == UnitType.BASE) {
                    enemyBaseLocation = ui.getLocation();
                    break;
                }
            }

            float bestScore = -10e20f;
            Direction best = Direction.ZERO;
            boolean anyTargetPresent = false;

            for(Direction dir : Direction.values()) {
                if(!pathfinding.canMove(dir) && dir != Direction.ZERO)
                    continue;

                Location loc = uc.getLocation().add(dir);
                float score = 0;
                boolean canShootAnyAggro = false;
                boolean canShootAny = false;

                for(UnitInfo unit : units) {
                    Location unitLoc = unit.getLocation();
                    int dist = unitLoc.distanceSquared(loc);
                    if((unit.getType() == UnitType.AXEMAN || unit.getType() == UnitType.WOLF) && dist <= 13) {
                        score -= 10e8f / dist;
                        anyTargetPresent = true;
                        uc.drawLineDebug(loc, unitLoc, 255, 0,0);
                    }
                    else if (unit.getType() == UnitType.SPEARMAN && !uc.isObstructed(unitLoc, loc)){
                        if (dist <= 18)
                            score -= 25000;
                        if (dist <= 32)
                            score -= 10000;
                    }
                    if(!canShootAnyAggro && dist <= 18 && dist >= 9 && willAttack(dir, unitLoc)){ //!uc.isObstructed(loc, unitLoc)) { WTF
                        canShootAny = true;
                        anyTargetPresent = true;
                        if(unit.getType() == UnitType.AXEMAN || unit.getType() == UnitType.SPEARMAN) {
                            canShootAnyAggro = true;
                            score -= dist * 10;
                        }
                        uc.drawLineDebug(loc, unitLoc, 255, 128,0);
                    }
                }
                if(canShootAnyAggro)
                    score += 20000;
                else if(canShootAny)
                    score += 10000;
                else if(dir == Direction.NORTHEAST || dir == Direction.NORTHWEST || dir == Direction.SOUTHEAST || dir == Direction.SOUTHWEST)
                    score -= 8000;
                if(enemyBaseLocation != null){
                    if(enemyBaseLocation.distanceSquared(loc) <= 18)
                        score -= 10e10;
                    score -= 0.01 * enemyBaseLocation.distanceSquared(loc);
                }
                if (baseLocation != null && baseLocation.distanceSquared(loc) <= 2){
                    score += 100;
                }

                int light = uc.senseIllumination(loc);
                score -= light;
                if(light < 10)
                    score += 500;

                uc.println("score " + dir + ": " + score);
                if(score > bestScore) {
                    bestScore = score;
                    best = dir;
                }
            }

            uc.println(anyTargetPresent);

            if(!anyTargetPresent) {
                if (reinforceRequested && baseLocation != null){
                    uc.println("reinforcing");
                    pathfinding.pathfindTo(baseLocation);
                }
                else if(enemyBaseLocation == null) {
                    uc.println("exploring enemy base");
                    Location obj = exploration.getLocation();
                    if (obj == null){
                        exploration = new Exploration(uc, exploration.CHUNK_SIZE, exploration.MOVEMENT_MULT);
                        obj = exploration.getLocation();
                    }
                    pathfinding.pathfindTo(obj);
                }
                else {
                    uc.println("wandering around enemy base");
                    pathfinding.wanderAround(enemyBaseLocation, 18);
                }
            }
            else if (best != Direction.ZERO)
                uc.move(best);
        }
    }

    void camperMicro(){
        uc.println("camperMicro");
        if (uc.canMove()){
            UnitInfo[] enemies = uc.senseUnits(uc.getTeam().getOpponent());
            if (enemies.length > 0){
                idleMicro();
            }
            else if (uc.getLocation().distanceSquared(baseLocation) > 2){
                pathfinding.pathfindTo(baseLocation);
            }
            else{
                Direction dir = baseLocation.directionTo(uc.getLocation());
                int k = 4;
                while (uc.canMove() && k-- > 0){
                    if (pathfinding.canMove(dir) && uc.getLocation().add(dir).distanceSquared(baseLocation) <= 2) {
                        uc.move(dir);
                    }
                    else{
                        dir = rotateRight ? dir.rotateRight() : dir.rotateLeft();
                    }
                }
                if (uc.canMove())
                    rotateRight = !rotateRight;

            }
        }
    }

    void readSmokeSignals() {
        uc.println("reading smoke signals");
        int[] smokeSignals = uc.readSmokeSignals();

        for(int smokeSignal : smokeSignals) {
            int msg = comms.decrypt(smokeSignal);
            if(comms.validate(msg)) {
                int msgType = comms.getType(msg);
                if (msgType == comms.MSG_TYPE_ENEMY_BASE){
                    enemyBaseLocation = comms.intToLocation(msg);
                }
                if (msgType == comms.MSG_TYPE_MISC){
                    readMiscMessage(msgType);
                }
            }
        }
    }

    void readMiscMessage(int info) {
        if (info == comms.MSG_REINFORCE_BASE){
            reinforceRequested = true;
        }
    }

}
