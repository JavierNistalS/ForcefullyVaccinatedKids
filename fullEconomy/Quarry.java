package fullEconomy;

import aic2021.user.*;

public class Quarry extends MyUnit {

    Quarry(UnitController uc){
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
            if(comms.sendMiscMessage(comms.MSG_QUARRY_START))
                toldExistence = true;
        }
        else if(totalEnemyAttack > 0 && !revokedExistence) {
            if(totalEnemyAttack > uc.getInfo().getHealth() && comms.sendMiscMessage(comms.MSG_QUARRY_END))
                revokedExistence = true;
        }
        else if(genevaSuggestion) {
            kgb.disruptEveryone(enemyBaseLocation);
        }
    }


}
