package unmaskedKid;

import aic2021.user.*;

public class Base extends MyUnit {

    int lastWorker = -100;
    int techIdx = 0;

    Technology[] techObjective = {Technology.COIN, Technology.ROCK_ART, Technology.UTENSILS, Technology.JOBS, Technology.EUGENICS, Technology.VOCABULARY, Technology.SCHOOLS, Technology.WHEEL};

    Base(UnitController uc){
        super(uc);
    }

    void playRound(){
        if (techIdx < techObjective.length){
            if (tryResearch(techObjective[techIdx]))
                techIdx++;
        }
        if (techIdx > 3 && uc.getRound() < 200 && lastWorker < uc.getRound() - 150){
            if (spawnRandom(UnitType.WORKER)) lastWorker = uc.getRound();
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
