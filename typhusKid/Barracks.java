package typhusKid;

import aic2021.user.UnitController;
import aic2021.user.UnitType;

public class Barracks extends MyUnit {

    Barracks(UnitController uc){
        super(uc);
    }

    int unitCount = 0;

    void playRound(){
        if (unitCount < 3 && trySpawnUnit(UnitType.SPEARMAN)){
            unitCount++;
        }
    }

}
