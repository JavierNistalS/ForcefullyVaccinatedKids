package fullEconomy;

import aic2021.user.*;

public class Settlement extends MyUnit {

    Settlement(UnitController uc){
        super(uc);
        comms = new Communications(uc);
        kgb = new TheKGB(uc);
    }

    Communications comms;
    boolean genevaSuggestion = false;
    boolean toldLocation = false;
    TheKGB kgb;

    void playRound() {
        readSmokeSignals();
        if(uc.canMakeSmokeSignal() && !toldLocation) {
            toldLocation = comms.sendLocationMessage(comms.MSG_TYPE_ALLIED_SETTLEMENT, uc.getLocation());
        }
        if (genevaSuggestion && enemyBaseLocation != null){
            Location loc = new Location(2*enemyBaseLocation.x - uc.getLocation().x, 2*enemyBaseLocation.y - uc.getLocation().y);
            if (uc.getRound()%3 == 0)
                kgb.disruptCarbassots(loc);
            if (uc.getRound()%3 == 1)
                kgb.disruptRosa(loc);
            if (uc.getRound()%3 == 2)
                kgb.disruptWololo(loc, uc.getRandomDouble() < 0.5);
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
