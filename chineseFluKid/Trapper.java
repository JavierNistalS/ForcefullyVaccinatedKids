package chineseFluKid;

import aic2021.user.*;



public class Trapper extends MyUnit {

    Trapper(UnitController uc){
        super(uc);
        exploration = new Exploration(uc, 3, 75);
        pathfinding = new Pathfinding(uc, this);
        comms = new Communications(uc);
    }

    Exploration exploration;
    Pathfinding pathfinding;
    Communications comms;

    void playRound() {
        identifyBase();
        setTraps();
        readSmokeSignals();
        exploration.updateChunks();

        Location target = exploration.getLocation();
        if (target == null){
            exploration = new Exploration(uc, 3, 75);
            target = exploration.getLocation();
        }
        pathfinding.pathfindTo(target);
    }

    void setTraps() {
        if (uc.canAttack()) {
            for (Direction dir : dirs) {
                Location loc = uc.getLocation().add(dir);
                if (baseLocation != null && baseLocation.distanceSquared(loc) <= 2)
                    continue;
                boolean resourcePresent = false;
                if (uc.canSenseLocation(loc)){
                    ResourceInfo[] resources = uc.senseResourceInfo(loc);
                    for (ResourceInfo ri : resources){
                        if (ri != null)
                            resourcePresent = true;
                    }

                }
                if (!resourcePresent && uc.canAttack(loc) && loc.x % 3 == 0 && loc.y % 3 == 0){
                    uc.attack(loc);
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
