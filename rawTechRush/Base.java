package rawTechRush;

import aic2021.user.*;

public class Base extends MyUnit {

    int lastWorker = -100;
    int techIdx = 0;

    Technology[] techObjective = {Technology.COIN, Technology.ROCK_ART, Technology.BOXES, Technology.JOBS, Technology.EUGENICS, Technology.VOCABULARY, Technology.SCHOOLS, Technology.WHEEL};

    Base(UnitController uc){
        super(uc);
    }

    void playRound() {
        if(techIdx == techObjective.length)
            uc.killSelf();

        while(tryResearch(techObjective[techIdx]) && techIdx < techObjective.length)
            techIdx++;

        if(techIdx >= 30 && lastWorker < 0)
        {
            trySpawnUnit(UnitType.WORKER);
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
