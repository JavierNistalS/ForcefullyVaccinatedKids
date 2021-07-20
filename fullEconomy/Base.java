package fullEconomy;

import aic2021.user.*;

public class Base extends MyUnit {

    int lastWorker = -100;
    int techIdx = 0;

    Technology[] techObjective = {Technology.COIN, Technology.ROCK_ART, Technology.BOXES, Technology.JOBS, Technology.VOCABULARY, Technology.OIL, Technology.SCHOOLS, Technology.WHEEL};

    Base(UnitController uc){
        super(uc);
    }

    void playRound() {
        generalAttack();

        //if(techIdx == techObjective.length)
        //    uc.killSelf();

        while(tryResearch(techObjective[techIdx]) && techIdx < techObjective.length)
            techIdx++;

        if(techIdx >= 2 && lastWorker < 0)
        {
            if(trySpawnUnit(UnitType.WORKER))
                lastWorker = uc.getRound();
        }
    }

    boolean tryResearch(Technology tech){
        if (uc.canResearchTechnology(tech)){
            uc.researchTechnology(tech);
            return true;
        }
        return false;
    }
}
