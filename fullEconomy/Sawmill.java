package fullEconomy;

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

    void playRound(){
        readSmokeSignalBuilding(comms);

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
        else if(genevaSuggestion) {
            kgb.disruptEveryone(enemyBaseLocation);
        }
    }

}
