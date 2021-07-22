package fullEconomy;

import aic2021.user.*;

public class Explorer extends MyUnit {

    Explorer(UnitController uc){
        super(uc);
        pathfinding = new Pathfinding(uc);
        exploration = new Exploration(uc, uc.getLocation(), 5, 50);
        comms = new Communications(uc);
    }

    Exploration exploration;
    Pathfinding pathfinding;
    Communications comms;

    void playRound(){
        sustainTorch();
        identifyBase();
        readSmokeSignals();
        ResourceInfo[] resources = uc.senseResources();
        UnitInfo[] enemies = uc.senseUnits(uc.getTeam().getOpponent());
        pathfinding.pathfindTo(exploration.getLocation());
        for (UnitInfo ui : enemies){
            if (enemyBaseLocation == null && ui.getType() == UnitType.BASE){
                if (comms.sendLocationMessage(comms.MSG_TYPE_ENEMY_BASE, ui.getLocation())){
                    enemyBaseLocation = ui.getLocation();
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
