package fullEconomy;

import aic2021.user.*;

public class Settlement extends MyUnit {

    Settlement(UnitController uc){
        super(uc);
        comms = new Communications(uc);
        kgb = new TheKGB(uc);
    }

    Communications comms;
    int toldLocationCountdown = 0;
    TheKGB kgb;

    void playRound() {
        readSmokeSignalBuilding(comms);

        if(!genevaSuggestion || toldLocationCountdown <= 0) {
            if(comms.sendLocationMessage(comms.MSG_TYPE_ALLIED_SETTLEMENT, uc.getLocation()))
                toldLocationCountdown = 6;
        }
        else {
            kgb.disruptEveryone(enemyBaseLocation);
            toldLocationCountdown--;
        }
    }
}
