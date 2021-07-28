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

    void playRound(){
        /*if (uc.getRound() == 35)
            debugObstructed();*/
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

        Location toExplore = exploration.getLocation();
        if (toExplore == null){
            exploration = new Exploration(uc, 5, 50);
            toExplore = exploration.getLocation();
        }
        pathfinding.pathfindTo(toExplore);
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
