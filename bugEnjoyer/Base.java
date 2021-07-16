package bugEnjoyer;

import aic2021.user.*;

public class Base extends MyUnit {

    int lastWorker = -100;
    int techIdx = 0;

    Technology[] techObjective = {Technology.COIN, Technology.ROCK_ART, Technology.BOXES, Technology.JOBS, Technology.EUGENICS, Technology.VOCABULARY, Technology.SCHOOLS, Technology.WHEEL};

    Base(UnitController uc){
        super(uc);
    }

    void playRound() {
        for (Technology T : Technology.values()){
            tryResearch(T);
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
