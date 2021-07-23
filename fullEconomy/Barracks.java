package fullEconomy;

import aic2021.user.UnitController;
import aic2021.user.UnitInfo;

public class Barracks extends MyUnit {

    Barracks(UnitController uc){
        super(uc);
        comms = new Communications(uc);
        kgb = new TheKGB(uc);
    }

    Communications comms;
    TheKGB kgb;
    int totalEnemyAttack;

    void playRound() {
        readSmokeSignals();

        totalEnemyAttack = 0;
        UnitInfo[] enemyUnits = uc.senseUnits(uc.getOpponent());
        for(UnitInfo enemyUnit : enemyUnits)
            totalEnemyAttack += enemyUnit.getAttack();

        if(genevaSuggestion) {
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
            }
        }
    }
}
