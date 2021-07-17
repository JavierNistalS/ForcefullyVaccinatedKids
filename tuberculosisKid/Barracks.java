package tuberculosisKid;

import aic2021.user.UnitController;
import aic2021.user.UnitType;

public class Barracks extends MyUnit {

    Barracks(UnitController uc){
        super(uc);
    }

    int spearmanCount = 0;

    void playRound()
    {
        if(spearmanCount < 5 && spawnRandom(UnitType.SPEARMAN))
            spearmanCount++;
        else
            spawnRandom(UnitType.AXEMAN);
    }
}
