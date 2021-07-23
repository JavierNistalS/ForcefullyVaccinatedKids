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
        if (genevaSuggestion){
            Location loc;
            if (enemyBaseLocation != null)
                loc = new Location(2*enemyBaseLocation.x - uc.getLocation().x, 2*enemyBaseLocation.y - uc.getLocation().y);
            else
                loc = new Location((int)(1050*uc.getRandomDouble()), (int)(1050*uc.getRandomDouble()));
            uc.drawLineDebug(uc.getLocation(), loc, 0,0,0);
            double random = uc.getRandomDouble();
            if (random < 0.2)
                kgb.disruptCarbassots(loc);
            else if (random < 0.4)
                kgb.disruptRosa(loc);
            else if (random < 0.6)
                kgb.disruptWololo(loc, uc.getRandomDouble() < 0.5);
            else if (random < 0.8){
                comms.sendLocationMessage(comms.MSG_TYPE_ALLIED_SETTLEMENT, uc.getLocation());
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
