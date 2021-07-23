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
    boolean toldExistence;

    void playRound(){
        readSmokeSignalBuilding(comms);

        UnitInfo[] enemyUnits = uc.senseUnits(uc.getOpponent());

        for(UnitInfo enemyUnit : enemyUnits) {

        }

        if(!toldExistence) {
            if(comms.sendMiscMessage(comms.MSG_QUARRY_START))
                toldExistence = true;
        }
        else if(genevaSuggestion){
            kgb.disruptEveryone(enemyBaseLocation);
        }
    }

}
