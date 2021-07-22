package fullEconomy;

import aic2021.user.*;


public class Base extends MyUnit {

    int lastWorker = -100;
    int workerCount = 0;
    int techIdx = 0;

    Technology[] initial = {Technology.UTENSILS, Technology.COIN};
    Technology[] preJobs = {Technology.DOMESTICATION, Technology.MILITARY_TRAINING, Technology.BOXES, Technology.ROCK_ART};
    Technology[] postJobs = {Technology.DOMESTICATION, Technology.TACTICS, Technology.EXPERTISE, Technology.CRYSTALS, Technology.COMBUSTION, Technology.POISON, Technology.WHEEL};

    Base(UnitController uc) {
        super(uc);
    }

    void playRound() {
        generalAttack();

        if(uc.getRound() > 600)
            uc.killSelf();

        //while(techIdx < techObjective.length && tryResearch(techObjective[techIdx]))
        //    techIdx++;

        if(workerCount < 3) {
            if(trySpawnUnit(UnitType.WORKER))
                workerCount++;
        }
    }

    boolean tryResearch(Technology tech) {
        if (uc.canResearchTechnology(tech)) {
            uc.researchTechnology(tech);
            return true;
        }
        return false;
    }

    // will return true when all techs in the group have been researched
    boolean tryResearch(TechGroup techGroup) {
        for(Technology technology : techGroup.techs)
            if(tryResearch(technology))
                techGroup.researched++;

        return techGroup.researched == techGroup.techs.length;
    }
}
