package wolfGang;

import aic2021.user.*;



public class Trapper extends MyUnit {

    Trapper(UnitController uc){
        super(uc);
        exploration = new Exploration(uc, 3, 6);
        pathfinding = new EvasivePathfinding(uc, this);
        comms = new Communications(uc);
        initialLocation = uc.getLocation();
    }

    Exploration exploration;
    EvasivePathfinding pathfinding;
    Communications comms;
    UnitInfo[] enemyUnits;
    Location initialLocation;
    boolean[] hasTrap = new boolean[99*99];

    void playRound() {
        identifyBase();
        setTraps();
        readSmokeSignals();
        readRocks();
        pathfinding.updateEnemyUnits();
        exploration.updateChunks();
        enemyUnits = uc.senseUnits(uc.getOpponent());


        if(uc.canMove()) {
            // super-evasion micro
            int bestScore = 0;
            Direction bestDir = Direction.ZERO;
            boolean any = false;

            for(Direction dir : dirs) {
                if (pathfinding.canMove(dir)) {
                    Location loc = uc.getLocation().add(dir);
                    int score = 0;
                    for(UnitInfo unit : enemyUnits) {
                        score += unit.getType().attack * unit.getLocation().distanceSquared(loc);
                        any |= unit.getType().attack > 0;
                    }

                    if(score > bestScore) {
                        bestDir = dir;
                        bestScore = score;
                    }
                }
            }

            if(any)
                pathfinding.tryMove(bestDir);
            else {
                Location target = exploration.getLocation();
                if (target == null) {
                    exploration = new Exploration(uc, exploration.CHUNK_SIZE, exploration.MOVEMENT_MULT);
                    target = exploration.getLocation();
                }
                pathfinding.pathfindTo(target);
            }
        }

        if (enemyBaseLocation == null) {

        }
        else {
            pathfinding.wanderAround(enemyBaseLocation, 18);
        }
    }

    void setTraps() {
        if (uc.canAttack()) {
            for (Direction dir : dirs) {
                Location loc = uc.getLocation().add(dir);
                if (uc.canAttack(loc)) {
                    if (baseLocation != null && baseLocation.distanceSquared(loc) <= 18)
                        continue;
                    if (uc.canSenseLocation(loc) && (uc.hasTrap(loc) || uc.hasMountain(loc)))
                        continue;
                    if (enemyBaseLocation != null && enemyBaseLocation.distanceSquared(loc) <= 18)
                        uc.attack(loc);
                    boolean resourcePresent = false;
                    if (uc.canSenseLocation(loc)) {
                        ResourceInfo[] resources = uc.senseResourceInfo(loc);
                        for (ResourceInfo ri : resources) {
                            if (ri != null)
                                resourcePresent = true;
                        }
                    }
                    if (!resourcePresent && pathfinding.isTrapLocation(loc)) {
                        uc.attack(loc);
                    }
                }
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
