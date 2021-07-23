package chineseFluKid;

import aic2021.user.UnitController;
import aic2021.user.UnitInfo;

public class Sawmill extends MyUnit {

    Sawmill(UnitController uc){
        super(uc);
        comms = new Communications(uc);
        kgb = new TheKGB(uc);
    }

    Communications comms;
    TheKGB kgb;
    boolean toldExistence = false;
    boolean revokedExistence = false;
    int totalEnemyAttack;

    boolean beingBuilt = true;
    int updateRound = -10;
    int lastStatusUpdate = -10;

    void playRound(){
        readSmokeSignals();

        totalEnemyAttack = 0;
        UnitInfo[] enemyUnits = uc.senseUnits(uc.getOpponent());
        for(UnitInfo enemyUnit : enemyUnits)
            totalEnemyAttack += enemyUnit.getAttack();

        if(!toldExistence) {
            if(comms.sendMiscMessage(comms.MSG_SAWMILL_START))
                toldExistence = true;
        }
        else if(totalEnemyAttack > 0 && !revokedExistence) {
            if(totalEnemyAttack > uc.getInfo().getHealth() && comms.sendMiscMessage(comms.MSG_SAWMILL_END))
                revokedExistence = true;
        }
        else if (lastStatusUpdate < uc.getRound() - 100 && !beingBuilt){
            comms.sendMiscMessage(comms.MSG_STOP_BUILDING_SAWMILLS);
        }
        else if(genevaSuggestion) {
            kgb.disruptEveryone(enemyBaseLocation);
        }
    }

    void readSmokeSignals() {
        uc.println("reading smoke signals");
        int[] smokeSignals = uc.readSmokeSignals();

        for(int smokeSignal : smokeSignals) {
            int msg = comms.decrypt(smokeSignal);
            if(comms.validate(msg)) {
                int msgType = comms.getType(msg);
                if (msgType == comms.MSG_TYPE_ENEMY_BASE)
                    enemyBaseLocation = comms.intToLocation(msg);
                else if(msgType == comms.MSG_TYPE_MISC)
                    readMiscMessage(comms.getInfo(msg));
            }
        }
    }

    void readMiscMessage(int info){
        if(info == comms.MSG_START_BUILDING_SAWMILLS) {
            beingBuilt = true;
            updateRound = uc.getRound();
        }
        else if (updateRound < uc.getRound() && info == comms.MSG_STOP_BUILDING_SAWMILLS)
            beingBuilt = false;
    }

}
