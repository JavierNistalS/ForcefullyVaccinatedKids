package wolfGang;

import aic2021.user.*;



public class Trapper extends MyUnit {

    Trapper(UnitController uc){
        super(uc);
        exploration = new Exploration(uc, 3, 75);
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
        pathfinding.updateEnemyUnits();
        exploration.updateChunks();
        enemyUnits = uc.senseUnits(uc.getOpponent());


        if(uc.canMove()) {
            // super-evasion micro
            for(Direction dir : dirs) {
                if (pathfinding.canMove(dir)) {
                    int score = 0;
                    for(UnitInfo unit : enemyUnits) {
                        score += unit.getType().attack;
                    }
                }
            }
        }

        if (enemyBaseLocation == null) {
            Location target = exploration.getLocation();
            if (target == null) {
                exploration = new Exploration(uc, 3, 75);
                target = exploration.getLocation();
            }
            pathfinding.pathfindTo(target);
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
                    if (baseLocation != null && baseLocation.distanceSquared(loc) <= 2)
                        continue;
                    if (uc.canSenseLocation(loc) && uc.hasTrap(loc))
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
