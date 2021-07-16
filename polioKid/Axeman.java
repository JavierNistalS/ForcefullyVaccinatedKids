package polioKid;

import aic2021.user.Location;
import aic2021.user.UnitController;

public class Axeman extends MyUnit {

    Axeman(UnitController uc){
        super(uc);
    }

    int attackRound = 600;

    void playRound(){
        int[] info = uc.readSmokeSignals();
        if (info.length > 0){
            enemyBaseLocation = new Location(info[0]/100000, info[0]%100000);
        }
        generalAttack();
        if (enemyBaseLocation != null && (attackRound < uc.getRound() || uc.getLocation().distanceSquared(enemyBaseLocation) > 50)){
            move3(uc.getLocation().directionTo(enemyBaseLocation));
        }
        else if (enemyBaseLocation != null){
            move5(enemyBaseLocation.directionTo(uc.getLocation()));
        }
        generalAttack();
    }

}
