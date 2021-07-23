package fullEconomy;

import aic2021.user.*;


public class Base extends MyUnit {

    int lastWorker = -100;
    int workerCount = 0;
    int explorerCount = 0;

    int techPhase = 0;
    Technology[] preJobTechs = {Technology.DOMESTICATION, Technology.MILITARY_TRAINING, Technology.BOXES, Technology.ROCK_ART};
    Technology[] endgameTechs = {Technology.TACTICS, Technology.EUGENICS, Technology.CRYSTALS, Technology.COMBUSTION, Technology.POISON, Technology.WHEEL};
    int endgameTechIdx = 0;

    Communications comms;

    boolean buildFoodState = true;
    boolean buildWoodState = true;
    boolean buildStoneState = true;
    int farmCount, sawmillCount, quarryCount;

    Base(UnitController uc) {
        super(uc);
        comms = new Communications(uc);
    }

    void playRound() {
        generalAttack();
        readSmokeSignals();

        // TODO: stop economic growth at a reasonable level


        //if(uc.getRound() > 500)
        //    uc.killSelf();

        if(enemyBaseLocation != null && uc.senseUnits(uc.getOpponent()).length == 0)
            comms.sendLocationMessage(comms.MSG_TYPE_ENEMY_BASE, enemyBaseLocation);

        if(techPhase == 0) { // pre-jobs
            tryResearch(Technology.COIN);
            tryResearch(Technology.UTENSILS);
            if(hasTech(Technology.COIN) && hasTech(Technology.UTENSILS))
                techPhase++;
        }
        else if(techPhase == 1) { // jobs
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
        else if(techPhase == 2) { // jobs 2: electric boogaloo
            if(tryResearch(Technology.JOBS))
                techPhase++;
        }
        else if(techPhase == 3) { // post-jobs
            if(tryResearch(Technology.DOMESTICATION))
                techPhase++;
        }
        else { // endgame
            if(endgameTechIdx == endgameTechs.length - 1)
                uc.killSelf();
            else
                while(endgameTechIdx < endgameTechs.length && tryResearch(endgameTechs[endgameTechIdx]))
                    endgameTechIdx++;
        }

        //while(techIdx < techObjective.length && tryResearch(techObjective[techIdx]))
        //    techIdx++;

        if(workerCount < 1) {
            if(trySpawnUnit(UnitType.EXPLORER))
                workerCount++;
        }
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

    void readSmokeSignals() {
        uc.println("reading smoke signals");
        int[] smokeSignals = uc.readSmokeSignals();

        for(int smokeSignal : smokeSignals) {
            uc.println("smoke: " + smokeSignal);
            int msg = comms.decrypt(smokeSignal);
            uc.println("decrypted: " + msg);
            if(comms.validate(msg)) {
                int msgType = comms.getType(msg);
                uc.println("smoke validated, type = " + msgType);
                if (msgType == comms.MSG_TYPE_ENEMY_BASE)
                    enemyBaseLocation = comms.intToLocation(msg);
            }
        }
    }
}
