package fullEconomy;

import aic2021.user.*;


public class Base extends MyUnit {

    int lastWorker = -100;
    int workerCount = 0;

    int techPhase = 0;
    Technology[] preJobTechs = {Technology.DOMESTICATION, Technology.MILITARY_TRAINING, Technology.BOXES, Technology.ROCK_ART};
    Technology[] postJobTechs = {Technology.DOMESTICATION, Technology.TACTICS, Technology.EXPERTISE};
    Technology[] endgameTechs = {Technology.CRYSTALS, Technology.COMBUSTION, Technology.POISON, Technology.WHEEL};

    Base(UnitController uc) {
        super(uc);
    }

    void playRound() {
        generalAttack();

        if(uc.getRound() > 600)
            uc.killSelf();

        if(techPhase == 0) {
            tryResearch(Technology.COIN);
            tryResearch(Technology.UTENSILS);
            if(hasTech(Technology.COIN) && hasTech(Technology.UTENSILS))
                techPhase++;
        }
        else if(techPhase == 1) {
            int jobsWood = Technology.JOBS.getWoodCost();
            int jobsStone = Technology.JOBS.getStoneCost();
            int jobsFood = Technology.JOBS.getFoodCost();
            for(Technology tech : preJobTechs) {
                int totalWood = jobsWood + tech.getWoodCost();
                int totalStone = jobsStone + tech.getStoneCost();
                int totalFood = jobsFood + tech.getFoodCost();

                if(hasResources(totalFood, totalWood, totalStone) && tryResearch(tech))
                {
                    techPhase++;
                    if(tryResearch(Technology.JOBS))
                        techPhase++;
                }
            }
        }
        else if(techPhase == 2)
        {
            if(tryResearch(Technology.JOBS))
                techPhase++;
        }
        else if(techPhase == 3)
        {

        }

        //while(techIdx < techObjective.length && tryResearch(techObjective[techIdx]))
        //    techIdx++;

        if(workerCount < 3) {
            if(trySpawnUnit(UnitType.WORKER))
                workerCount++;
        }
    }

    boolean tryResearch(Technology tech) {
        if(hasTech(tech))
            return true;
        if (uc.canResearchTechnology(tech)) {
            uc.researchTechnology(tech);
            return true;
        }
        return false;
    }

    boolean hasTech(Technology tech) {
        return uc.hasResearched(tech, uc.getTeam());
    }

    boolean hasResources(int food, int wood, int stone) {
        return food <= uc.getResource(Resource.FOOD)
            && wood <= uc.getResource(Resource.WOOD)
            && stone <= uc.getResource(Resource.STONE);
    }
}
