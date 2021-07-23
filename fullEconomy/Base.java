package fullEconomy;

import aic2021.user.*;


public class Base extends MyUnit {

    final int FARM_MAX = 8;
    final int SAWMILL_MAX = 7;
    final int QUARRY_MAX = 7;

    Base(UnitController uc) {
        super(uc);
        comms = new Communications(uc);
        kgb = new TheKGB(uc);
    }

    int lastWorker = -100;
    int workerCount = 0;
    int explorerCount = 0;
    int trapperCount = 0;

    int techPhase = 0;
    Technology[] preJobTechs = {Technology.DOMESTICATION, Technology.MILITARY_TRAINING, Technology.BOXES, Technology.ROCK_ART};
    Technology[] endgameTechs = {Technology.TACTICS, Technology.EUGENICS, Technology.CRYSTALS, Technology.COMBUSTION, Technology.POISON, Technology.WHEEL};
    int endgameTechIdx = 0;

    Communications comms;
    TheKGB kgb;

    boolean buildFoodState = true;
    boolean buildWoodState = true;
    boolean buildStoneState = true;
    int farmCount = 0, sawmillCount = 0, quarryCount = 0;
    boolean genevaSuggestion = false;
    int totalResourcesSeen;
    int lastEnemyBaseTransmission = -100;

    void playRound() {
        generalAttack();
        readSmokeSignals();
        research();

        // TODO: stop economic growth at a reasonable level

        totalResourcesSeen = 0;
        ResourceInfo[] resources = uc.senseResources();
        for(ResourceInfo resource : resources)
            totalResourcesSeen += resource.amount;

        manageMaxBuildings();

        if(lastEnemyBaseTransmission < uc.getRound() - 60 && enemyBaseLocation != null && uc.senseUnits(uc.getOpponent()).length == 0) {
            if (comms.sendLocationMessage(comms.MSG_TYPE_ENEMY_BASE, enemyBaseLocation))
                lastEnemyBaseTransmission = uc.getRound();
        }

        if (genevaSuggestion) {
            kgb.disruptEveryone(enemyBaseLocation);
        }

        if(explorerCount < 1)
            if(trySpawnUnit(UnitType.EXPLORER))
                explorerCount++;

        if(workerCount < 3 + totalResourcesSeen / 150)
            if(trySpawnUnit(UnitType.WORKER))
                workerCount++;

        if(trapperCount < 2) {
            if (trySpawnUnit(UnitType.TRAPPER))
                trapperCount++;
        }

    }

    void manageMaxBuildings(){
        uc.println("sawmillCount: " + sawmillCount);
        uc.println("farmCount: " + farmCount);
        uc.println("quarryCount: " + quarryCount);
        if (buildWoodState && sawmillCount >= SAWMILL_MAX){
            if (comms.sendMessage(comms.MSG_TYPE_MISC, comms.MSG_STOP_BUILDING_SAWMILLS)) {
                buildWoodState = false;
                uc.println("no build sawmills, *bonk");
            }
        }
        if (!buildWoodState && sawmillCount < SAWMILL_MAX){
            if (comms.sendMessage(comms.MSG_TYPE_MISC, comms.MSG_START_BUILDING_SAWMILLS)) {
                buildWoodState = true;
                uc.println("build sawmills");
            }
        }
        if (buildFoodState && farmCount >= FARM_MAX){
            if (comms.sendMessage(comms.MSG_TYPE_MISC, comms.MSG_STOP_BUILDING_FARMS)) {
                buildFoodState = false;
                uc.println("no build farms, *bonk");
            }
        }
        if (!buildFoodState && farmCount < FARM_MAX){
            if (comms.sendMessage(comms.MSG_TYPE_MISC, comms.MSG_START_BUILDING_FARMS)) {
                buildFoodState = true;
                uc.println("build farms");
            }
        }
        if (buildStoneState && quarryCount >= QUARRY_MAX){
            if (comms.sendMessage(comms.MSG_TYPE_MISC, comms.MSG_STOP_BUILDING_QUARRYS)) {
                buildStoneState = false;
                uc.println("no build quarries, *bonk");
            }
        }
        if (!buildStoneState && quarryCount < QUARRY_MAX){
            if (comms.sendMessage(comms.MSG_TYPE_MISC, comms.MSG_START_BUILDING_QUARRYS)) {
                buildStoneState = true;
                uc.println("build quarries");
            }
        }
    }

    void research(){
        if (uc.getResource(Resource.FOOD) > 2000){
            tryResearch(Technology.DOMESTICATION);
        }
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

            int bestMissingMax = 1000000000;
            int bestMissingFood = 1000000000;
            int bestMissingWood = 1000000000;
            int bestMissingStone = 1000000000;

            for(Technology tech : preJobTechs) {
                int missingFood = jobsFood + tech.getFoodCost() - uc.getResource(Resource.FOOD);
                int missingWood = jobsWood + tech.getWoodCost() - uc.getResource(Resource.WOOD);
                int missingStone = jobsStone + tech.getStoneCost() - uc.getResource(Resource.STONE);
                int missingMax = Math.max(missingFood, Math.max(missingWood, missingStone));

                if(missingMax <= 0 && tryResearch(tech)) {
                    techPhase++;
                    if(tryResearch(Technology.JOBS))
                        techPhase++;
                }
                else if(missingMax < bestMissingMax) {
                    bestMissingMax = missingMax;
                    bestMissingFood = missingFood;
                    bestMissingWood = missingWood;
                    bestMissingStone = missingStone;
                }
            }

            if(bestMissingFood < 100 && buildFoodState)
                buildFoodState = !comms.sendMiscMessage(comms.MSG_STOP_BUILDING_SETTLEMENT_TO_COLLECT_FOOD);
            else if(bestMissingWood < 100 && buildWoodState)
                buildFoodState = !comms.sendMiscMessage(comms.MSG_STOP_BUILDING_SETTLEMENT_TO_COLLECT_WOOD);
            else if(bestMissingStone < 100 && buildStoneState)
                buildFoodState = !comms.sendMiscMessage(comms.MSG_STOP_BUILDING_SETTLEMENT_TO_COLLECT_STONE);
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
            /*if(endgameTechIdx == endgameTechs.length - 1 && uc.canResearchTechnology(Technology.WHEEL))
                uc.killSelf();
            else*/
            while(endgameTechIdx < endgameTechs.length && tryResearch(endgameTechs[endgameTechIdx]))
                endgameTechIdx++;
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

    void readSmokeSignals() {
        uc.println("reading smoke signals");
        int[] smokeSignals = uc.readSmokeSignals();

        for(int smokeSignal : smokeSignals) {
            uc.println("reading " + smokeSignal);
            int msg = comms.decrypt(smokeSignal);
            if(comms.validate(msg)) {
                int msgType = comms.getType(msg);
                if (msgType == comms.MSG_TYPE_ENEMY_BASE)
                    enemyBaseLocation = comms.intToLocation(msg);
                else if(msgType == comms.MSG_TYPE_MISC)
                    readMiscMessage(comms.getInfo(msg));
            }
        }
    }

    void readMiscMessage(int info){
        if(info == comms.MSG_FARM_START)
            farmCount++;
        else if(info == comms.MSG_FARM_END)
            farmCount--;
        else if(info == comms.MSG_SAWMILL_START)
            sawmillCount++;
        else if(info == comms.MSG_SAWMILL_END)
            sawmillCount--;
        else if(info == comms.MSG_QUARRY_START)
            quarryCount++;
        else if(info == comms.MSG_QUARRY_END)
            quarryCount--;
    }
}
