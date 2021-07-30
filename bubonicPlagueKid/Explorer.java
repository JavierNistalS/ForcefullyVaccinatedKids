package bubonicPlagueKid;

import aic2021.user.*;

public class Explorer extends MyUnit {

    Explorer(UnitController uc){
        super(uc);
        pathfinding = new EvasivePathfinding(uc, this);
        exploration = new Exploration(uc, 5, 50);
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

        ResourceInfo[] resources = uc.senseResources();
        UnitInfo[] enemies = uc.senseUnits(uc.getTeam().getOpponent());

        for (UnitInfo ui : enemies){
            if (enemyBaseLocation == null && ui.getType() == UnitType.BASE){
                if (comms.sendLocationMessage(comms.MSG_TYPE_ENEMY_BASE, ui.getLocation())){
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

        Location toExplore = exploration.getLocation();
        if (toExplore == null){
            exploration = new Exploration(uc, 5, 50);
            toExplore = exploration.getLocation();
        }
        pathfinding.pathfindTo(toExplore);

        Location nextLoc = uc.getLocation().add(uc.getLocation().directionTo(toExplore));
        if(uc.canSenseLocation(nextLoc))
            fuckingWater |= uc.hasWater(nextLoc);
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
