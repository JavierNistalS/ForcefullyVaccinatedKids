package polioKid;

import aic2021.user.UnitController;
import aic2021.user.UnitType;

public class Barracks extends MyUnit {

    Barracks(UnitController uc){
        super(uc);
    }

    void playRound(){
        spawnRandom(UnitType.AXEMAN);
    }

}
