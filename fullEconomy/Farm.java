package fullEconomy;

import aic2021.user.UnitController;

public class Farm extends MyUnit {

    Farm(UnitController uc){
        super(uc);
        comms = new Communications(uc);
        kgb = new TheKGB(uc);
    }

    Communications comms;
    TheKGB kgb;
    boolean toldExistance;

    void playRound(){
        readSmokeSignalBuilding(comms);

        // TODO: check if you're gonna die (& send msg)

        if(!toldExistance) {
            if(comms.sendMiscMessage(comms.MSG_FARM_START))
                toldExistance = true;
        }
        else if(genevaSuggestion){
            kgb.disruptEveryone(enemyBaseLocation);
        }
    }

}
