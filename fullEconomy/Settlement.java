package fullEconomy;

import aic2021.user.UnitController;

public class Settlement extends MyUnit {

    Settlement(UnitController uc){
        super(uc);
        comms = new Communications(uc);
    }

    Communications comms;

    void playRound() {
        if(uc.canMakeSmokeSignal())
            comms.sendLocationMessage(comms.MSG_TYPE_ALLIED_SETTLEMENT, uc.getLocation());
    }
}
