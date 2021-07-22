package fullEconomy;

import aic2021.user.*;

public class Base extends MyUnit {

    int lastWorker = -100;
    int workerCount = 0;
    int techIdx = 0;

    Technology[] techObjective = {Technology.BOXES, Technology.COIN, Technology.ROCK_ART, Technology.JOBS};

    Base(UnitController uc){
        super(uc);
    }

    void playRound() {
        generalAttack();

        if(uc.getRound() > 600)
            uc.killSelf();

        while(techIdx < techObjective.length && tryResearch(techObjective[techIdx]))
            techIdx++;

        if(workerCount < 3) {
            if(trySpawnUnit(UnitType.WORKER))
                workerCount++;
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
