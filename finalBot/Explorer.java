package finalBot;

import aic2021.user.*;

public class Explorer extends MyUnit {

    Explorer(UnitController uc){
        super(uc);
        pathfinding = new EvasivePathfinding(uc, this);
        exploration = new Exploration(uc, 5, 2);
        comms = new Communications(uc);
    }

    Exploration exploration;
    EvasivePathfinding pathfinding;
    Communications comms;
    boolean fuckingWater = false;
    boolean tryingToRequestRafts = false;
    boolean requestedRafts = false;

    void playRound(){
        sustainTorch();
        identifyBase();
        readSmokeSignals();
        exploration.updateChunks();
        pathfinding.updateEnemyUnits();

        uc.println("exploration targetRound: " + exploration.targetRound);

        if (baseLocation != null && uc.getLocation().distanceSquared(baseLocation) <= 2 && enemyBaseLocation != null){
            uc.draw((enemyBaseLocation.x - baseLocation.x + 49)*100 + enemyBaseLocation.y - baseLocation.y + 49);
        }

        ResourceInfo[] resources = uc.senseResources();
        UnitInfo[] enemies = uc.senseUnits(uc.getTeam().getOpponent());

        for (UnitInfo ui : enemies){
            if (ui.getType() == UnitType.BASE){
                if ((enemyBaseLocation == null || enemyBaseLocation.distanceSquared(ui.getLocation()) > 0) && comms.sendLocationMessage(comms.MSG_TYPE_ENEMY_BASE, ui.getLocation())){
                    enemyBaseLocation = ui.getLocation();
                }
            }
        }

        if(tryingToRequestRafts) {
            if(!requestedRafts)
                requestedRafts = comms.sendMiscMessage(comms.MSG_REQUEST_RAFTS);
        }
        else if(exploration.willReset() && fuckingWater) {
            tryingToRequestRafts = true;
            uc.println("AQUATIC MAP DETECTED ON AMERICAN SOIL. LETHAL FORCE ENGAGED.");
        }

        if(uc.canMove()){
            Location toExplore = exploration.getLocation();
            if (toExplore == null) {
                exploration = new Exploration(uc, exploration.CHUNK_SIZE, exploration.MOVEMENT_MULT);
                toExplore = exploration.getLocation();
            }
            pathfinding.pathfindTo(toExplore);
            uc.drawLineDebug(uc.getLocation(), toExplore, 0, 0, 255);

            if(pathfinding.dodgedAnyEnemies)
                exploration.setTargetRound();

            Location nextLoc = uc.getLocation().add(uc.getLocation().directionTo(toExplore));
            if(uc.canSenseLocation(nextLoc) && uc.hasWater(nextLoc)) {
                fuckingWater = true;
                uc.drawPointDebug(nextLoc, 0, 255, 255);
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
