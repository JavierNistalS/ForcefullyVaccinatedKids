package gonorheaKid;

import aic2021.user.Location;
import aic2021.user.UnitController;

public class Axeman extends MyUnit {

    Axeman(UnitController uc){
        super(uc);
    }

    void playRound(){
        int[] info = uc.readSmokeSignals();
        if (info.length > 0){
            enemyBaseLocation = new Location(info[0]/100000, info[0]%100000);
        }
        generalAttack();
        if (enemyBaseLocation != null){
            move3(uc.getLocation().directionTo(enemyBaseLocation));
        }
        generalAttack();
    }

}
