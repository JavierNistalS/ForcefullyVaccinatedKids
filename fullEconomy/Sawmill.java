package fullEconomy;

import aic2021.user.UnitController;

public class Sawmill extends MyUnit {

    Sawmill(UnitController uc){
        super(uc);
        comms = new Communications(uc);
        kgb = new TheKGB(uc);
    }

    Communications comms;
    TheKGB kgb;
    boolean toldExistence;

    void playRound(){
        readSmokeSignalBuilding(comms);

        // TODO: check if you're gonna die (& send msg)

        if(!toldExistence) {
            if(comms.sendMiscMessage(comms.MSG_SAWMILL_START))
                toldExistence = true;
        }
        else if(genevaSuggestion){
            kgb.disruptEveryone(enemyBaseLocation);
        }
    }

}
