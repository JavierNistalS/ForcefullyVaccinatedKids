package bubonicPlagueKid;

import aic2021.user.*;

public class Spearman extends MyUnit {

    Spearman(UnitController uc){
        super(uc);
        pathfinding = new Pathfinding(uc, this);
        comms = new Communications(uc);
        exploration = new Exploration(uc, 6, 100);
        minSpearmanID = uc.getInfo().getID();
        UnitInfo[] enemies = uc.senseUnits(uc.getTeam().getOpponent());
        if (enemies.length >= 2){
            stayDefensive = true;
        }
    }
    Pathfinding pathfinding;
    Communications comms;
    Exploration exploration;
    int minSpearmanID;

    boolean rotateRight = true;
    boolean stayDefensive = false;

    void playRound() {
        /*if (uc.getRound() == 300)
            debugObstructed();*/
        identifyBase();
        identifyEnemyBase();
        readSmokeSignals();
        generalAttack();
        updateMinSpearmanID();
        exploration.updateChunks();

        if (uc.getInfo().getID() == minSpearmanID || stayDefensive)
            camperMicro();
        else
            idleMicro();

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
            obstructed = !uc.isObstructed(uc.getLocation().add(dir), loc);
        else if(uc.canSenseLocation(uc.getLocation()))
            obstructed = !uc.isObstructed(uc.getLocation().add(dir), loc);
        else
            obstructed = isObstructedNice(uc.getLocation().add(dir), loc);

        if (obstructed)
            uc.drawPointDebug(loc, 0,0,0);
        else
            uc.drawPointDebug(loc, 255,255,255);

        return !obstructed;
    }

    void idleMicro() {
        if(uc.canMove()) {
            if(enemyBaseLocation != null)
                uc.println("enemy base: " + enemyBaseLocation.toString());
            else
                uc.println("no c");
            UnitInfo[] units = uc.senseUnits(uc.getTeam().getOpponent());
            boolean aggroPresent = false;
            for (UnitInfo ui : units){
                if (ui.getAttack() > 0 && ui.getType() != UnitType.BASE)
                    aggroPresent = true;
                if (ui.getType() == UnitType.BASE)
                    enemyBaseLocation = ui.getLocation();
            }
            if(!aggroPresent) {
                if(enemyBaseLocation == null) {
                    Location obj = exploration.getLocation();
                    if (obj == null){
                        exploration = new Exploration(uc, 5, 100);
                        obj = exploration.getLocation();
                    }
                    pathfinding.pathfindTo(obj);
                }
                else
                    pathfinding.wanderAround(enemyBaseLocation, 50);
            }
            else {
                float bestScore = -10e20f;
                Direction best = Direction.ZERO;

                for(Direction dir : Direction.values()) {
                    if(!pathfinding.canMove(dir) && dir != Direction.ZERO)
                        continue;

                    uc.println("evaluating " + dir);
                    Location loc = uc.getLocation().add(dir);
                    float score = 0;
                    boolean canShootAnyAggro = false;
                    boolean canShootAny = false;

                    for(UnitInfo unit : units) {
                        uc.println("aiming at " + unit.getType() + ' ' + unit.getID());
                        Location unitLoc = unit.getLocation();
                        int dist = unitLoc.distanceSquared(loc);
                        uc.println("dist = " + dist);
                        if(unit.getType() == UnitType.AXEMAN && dist <= 13)
                            score -= 10e8f / dist;
                        else if(!canShootAnyAggro && dist <= 18 && dist >= 9 && willAttack(dir, unitLoc)){ //!uc.isObstructed(loc, unitLoc)) { WTF
                            uc.println("can shoot at it");
                            canShootAny = true;
                            if(unit.getType() == UnitType.AXEMAN || unit.getType() == UnitType.SPEARMAN) {
                                canShootAnyAggro = true;
                                score -= dist * 10;
                            }
                        }
                    }

                    uc.println("canShootAnyAggro: " + canShootAnyAggro);
                    uc.println("canShootAny: " + canShootAny);
                    if(canShootAnyAggro)
                        score += 20000;
                    else if(canShootAny)
                        score += 10000;
                    else if(dir == Direction.NORTHEAST || dir == Direction.NORTHWEST || dir == Direction.SOUTHEAST || dir == Direction.SOUTHWEST)
                        score -= 8000;
                    if(enemyBaseLocation != null){
                        if(enemyBaseLocation.distanceSquared(loc) <= 50)
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
                if (best != Direction.ZERO)
                    uc.move(best);
            }
        }
    }

    void camperMicro(){
        if (uc.canMove()){
            if (uc.getLocation().distanceSquared(baseLocation) > 2){
                pathfinding.pathfindTo(baseLocation);
            }
            else{
                boolean canAttack = false;
                UnitInfo[] enemies = uc.senseUnits(uc.getTeam().getOpponent());
                for (UnitInfo ui : enemies){
                    Location loc = ui.getLocation();
                    if (loc.distanceSquared(uc.getLocation()) <= 18 && !uc.isObstructed(loc, uc.getLocation()))
                        canAttack = true;
                }
                if (canAttack)
                    return;
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
            }
        }
    }

}
