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
        readSmokeSignalBuilding(comms);

        totalEnemyAttack = 0;
        UnitInfo[] enemyUnits = uc.senseUnits(uc.getOpponent());
        for(UnitInfo enemyUnit : enemyUnits)
            totalEnemyAttack += enemyUnit.getAttack();

        if(genevaSuggestion) {
            kgb.disruptEveryone(enemyBaseLocation);
        }
    }
}
