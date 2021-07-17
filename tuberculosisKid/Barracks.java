package tuberculosisKid;

import aic2021.user.UnitController;
import aic2021.user.UnitType;

public class Barracks extends MyUnit {

    Barracks(UnitController uc){
        super(uc);
    }

    void playRound()
    {
        if(uc.getRound() < 625)
            spawnRandom(UnitType.SPEARMAN);
        else
            spawnRandom(UnitType.AXEMAN);
    }

}
