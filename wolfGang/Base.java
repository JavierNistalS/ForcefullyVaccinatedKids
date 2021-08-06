package wolfGang;

import aic2021.user.*;


public class Base extends MyUnit {

    final int FARM_MAX = 7;
    final int SAWMILL_MAX = 8;
    final int QUARRY_MAX = 7;
    final int WORKER_MAX = 15;

    Base(UnitController uc) {
        super(uc);
        comms = new Communications(uc);
        kgb = new TheKGB(uc);
        checkTrollMap();
    }

    Communications comms;
    TheKGB kgb;

    int lastWorkerSeenRound = 0;
    int workerCount = 0;
    int explorerCount = 0;
    int trapperCount = 0;
    int wolfCount = 0;

    int techPhase = 0;
    Technology[] preJobTechs = {Technology.MILITARY_TRAINING};
    Technology[] endgameTechs = {Technology.TACTICS, Technology.EUGENICS, Technology.CRYSTALS, Technology.COMBUSTION, Technology.POISON, Technology.WHEEL};
    int endgameTechIdx = 0;

    boolean buildFoodState = true, buildWoodState = true, buildStoneState = true;
    boolean buildSettlementsForFood = true, buildSettlementsForWood = true, buildSettlementsForStone = true;
    int farmCount = 0, sawmillCount = 0, quarryCount = 0;
    int totalResourcesSeen;
    int lastEnemyBaseTransmission = -100;
    boolean barracksWorker = false;
    boolean raftsRequested = false;
    boolean reinforceRequested = false;
    boolean[] canSpawn = {true, true, true, true, true, true, true, true, true};
    UnitInfo[] enemies;

    boolean fuckingSpearmen = false;

    Technology[] tier1Techs = {Technology.SHARPENERS, Technology.COOKING, Technology.EUGENICS, Technology.NAVIGATION, Technology.OIL, Technology.VOCABULARY, Technology.HUTS, Technology.TACTICS};
    Technology[] tier2Techs = {Technology.CRYSTALS, Technology.COMBUSTION, Technology.POISON, Technology.EXPERTISE, Technology.FLINT, Technology.HOUSES};

    void playRound() {
        generalAttack();
        readSmokeSignals();
        research();
        checkForEnemyAttack();

        totalResourcesSeen = 0;
        ResourceInfo[] resources = uc.senseResources();
        for(ResourceInfo resource : resources) {
            if (uc.senseIllumination(resource.getLocation()) > 0)
                totalResourcesSeen += resource.amount;
        }

        UnitInfo[] allies = uc.senseUnits(uc.getTeam());
        for (UnitInfo ui : allies){
            if (ui.getType() == UnitType.WORKER)
                lastWorkerSeenRound = uc.getRound();
        }

        manageMaxBuildings();

        if(uc.getRound() > 9 && lastEnemyBaseTransmission < uc.getRound() - 40 && enemyBaseLocation != null) {
            if (comms.sendLocationMessage(comms.MSG_TYPE_ENEMY_BASE, enemyBaseLocation))
                lastEnemyBaseTransmission = uc.getRound();
        }

        if (uc.getRound() > 30 && genevaSuggestion && enemies.length < 3) {
            kgb.disruptEveryone(enemyBaseLocation);
        }

        if (hasTech(Technology.MILITARY_TRAINING) && !barracksWorker){
            barracksWorker = trySpawnUnit(UnitType.WORKER);
        }

        if(explorerCount < (enemyBaseLocation == null ? 2 : 1)) // the 1 explorer is to check for rafts
            if(trySpawnUnit(UnitType.EXPLORER))
                explorerCount++;

        if((workerCount < 15 + totalResourcesSeen / 200 && workerCount < WORKER_MAX) || lastWorkerSeenRound < uc.getRound() - 150)
            if(trySpawnUnit(UnitType.WORKER))
                workerCount++;

        if(trapperCount < 5 + uc.getRound() / 300) {
            if (trySpawnUnit(UnitType.TRAPPER))
                trapperCount++;
        }

        if (wolfCount < 10 + uc.getRound() / 300 || enemies.length > 2) {
            if (trySpawnUnit(UnitType.WOLF)){
                wolfCount++;
            }
        }

        if (wolfCount < 5 || (uc.hasResearched(Technology.JOBS, uc.getTeam()) && (wolfCount == 0 || uc.getResource(Resource.FOOD) > 1600) && wolfCount < 10)) {
            if (canBuildUnitWithMargin(UnitType.WOLF, 600, 75, 75) && trySpawnUnit(UnitType.WOLF))
                wolfCount++;
        }
    }

    void manageMaxBuildings(){
        uc.println("sawmillCount: " + sawmillCount);
        uc.println("farmCount: " + farmCount);
        uc.println("quarryCount: " + quarryCount);
        if (buildWoodState && sawmillCount >= SAWMILL_MAX){
            if (comms.sendMiscMessage(comms.MSG_STOP_BUILDING_SAWMILLS)) {
                buildWoodState = false;
                uc.println("no build sawmills, *bonk");
            }
        }
        if (!buildWoodState && sawmillCount < SAWMILL_MAX){
            if (comms.sendMiscMessage(comms.MSG_START_BUILDING_SAWMILLS)) {
                buildWoodState = true;
                uc.println("build sawmills");
            }
        }
        if (buildFoodState && farmCount >= FARM_MAX){
            if (comms.sendMiscMessage(comms.MSG_STOP_BUILDING_FARMS)) {
                buildFoodState = false;
                uc.println("no build farms, *bonk");
            }
            else
                uc.println("CAN'T SEND BC COOLDOWN");
        }
        else if(farmCount >= FARM_MAX)
            uc.println("ALREADY SENT (buildFoodState = false)");
        else
            uc.println("NOT ENOUGH FARMS (farms = " + farmCount + ")");

        if (!buildFoodState && farmCount < FARM_MAX){
            if (comms.sendMiscMessage(comms.MSG_START_BUILDING_FARMS)) {
                buildFoodState = true;
                uc.println("build farms");
            }
        }
        if (buildStoneState && quarryCount >= QUARRY_MAX){
            if (comms.sendMiscMessage(comms.MSG_STOP_BUILDING_QUARRYS)) {
                buildStoneState = false;
                uc.println("no build quarries, *bonk");
            }
        }
        if (!buildStoneState && quarryCount < QUARRY_MAX){
            if (comms.sendMiscMessage(comms.MSG_START_BUILDING_QUARRYS)) {
                buildStoneState = true;
                uc.println("build quarries");
            }
        }
    }

    void research(){

        uc.println("rafts requested: " + raftsRequested);
        if (canResearchWithMargin (Technology.RAFTS, 0, 750, 150) || raftsRequested){
            tryResearch(Technology.RAFTS);
        }

        if(techPhase == 0) { // pre-jobs
            tryResearch(Technology.COIN);
            tryResearch(Technology.UTENSILS);
            tryResearch(Technology.DOMESTICATION);

            if ((!raftsRequested || hasTech(Technology.RAFTS)) && canResearchWithMargin(Technology.MILITARY_TRAINING, 0, 125, 125)) {
                tryResearch(Technology.MILITARY_TRAINING);
            }

            if (raftsRequested)
                tryResearch(Technology.RAFTS);
            if(hasTech(Technology.COIN) && hasTech(Technology.UTENSILS) && hasTech(Technology.MILITARY_TRAINING) && (!raftsRequested || hasTech(Technology.RAFTS)))
                techPhase++;
        }
        else if(techPhase == 1) { // jobs
            tryResearch(Technology.COIN);
            tryResearch(Technology.UTENSILS);
            tryResearch(Technology.DOMESTICATION);

            if(tryResearch(Technology.JOBS)) {
                techPhase++;
            }
        }
        else if(techPhase == 2) {
            if(farmCount >= 2 && quarryCount >= 5 && sawmillCount >= 5 && canResearchWithMargin(Technology.DOMESTICATION, 0, 75, 75))
                tryResearch(Technology.DOMESTICATION);

            int food = uc.getResource(Resource.FOOD), wood = uc.getResource(Resource.WOOD), stone = uc.getResource(Resource.STONE);
            int foodCost = 1500, woodCost = 1500, stoneCost = 1500;
            if(food >= foodCost && wood >= woodCost && stone >= stoneCost) {
                // SCHOOLS
                foodCost += Technology.SCHOOLS.getFoodCost();
                if(food >= foodCost && researchTechInner(food, wood, stone, foodCost, woodCost, stoneCost)) {
                    endgameTechs = new Technology[] {tier1Techs[0], tier1Techs[1], Technology.SCHOOLS, Technology.WHEEL};
                    techPhase++;
                }
                else {
                    foodCost -= Technology.SCHOOLS.getFoodCost();

                    // NO SCHOOLS
                    mainLoop:
                    for(Technology tech2a : tier2Techs) { // 1st T2 tech
                        foodCost += tech2a.getFoodCost();
                        woodCost += tech2a.getWoodCost();
                        stoneCost += tech2a.getStoneCost();

                        if(food >= foodCost && wood >= woodCost && stone >= stoneCost) {
                            for(Technology tech2b : tier2Techs) { // 2nd T2 tech
                                if(tech2a.ordinal() <= tech2b.ordinal())
                                    continue;

                                foodCost += tech2b.getFoodCost();
                                woodCost += tech2b.getWoodCost();
                                stoneCost += tech2b.getStoneCost();

                                if(food >= foodCost && wood >= woodCost && stone >= stoneCost) {
                                    for(Technology tech2c : tier2Techs) { // third T3 tech
                                        if(tech2b.ordinal() <= tech2c.ordinal())
                                            continue;

                                        foodCost += tech2c.getFoodCost();
                                        woodCost += tech2c.getWoodCost();
                                        stoneCost += tech2c.getStoneCost();
                                        if(food >= foodCost && wood >= woodCost && stone >= stoneCost) {
                                            if(researchTechInner(food, wood, stone, foodCost, woodCost, stoneCost)) {
                                                endgameTechs = new Technology[]{tier1Techs[0], tier1Techs[1], tech2a, tech2b, tech2c, Technology.WHEEL};
                                                techPhase++;
                                                break mainLoop;
                                            }
                                        }

                                        foodCost -= tech2c.getFoodCost();
                                        woodCost -= tech2c.getWoodCost();
                                        stoneCost -= tech2c.getStoneCost();
                                    }
                                }
                                foodCost -= tech2b.getFoodCost();
                                woodCost -= tech2b.getWoodCost();
                                stoneCost -= tech2b.getStoneCost();
                            }
                        }
                        foodCost -= tech2a.getFoodCost();
                        woodCost -= tech2a.getWoodCost();
                        stoneCost -= tech2a.getStoneCost();
                    }
                }

            }
        }
        if(uc.getRound() > 1990)
            techPhase = 3;
        if(techPhase >= 3) {
            while(endgameTechIdx < endgameTechs.length && tryResearch(endgameTechs[endgameTechIdx]))
                endgameTechIdx++;
        }
    }

    boolean researchTechInner(int food, int wood, int stone, int foodCost, int woodCost, int stoneCost) {
        for(Technology tech1a : tier1Techs) {
            foodCost += tech1a.getFoodCost();
            woodCost += tech1a.getWoodCost();
            stoneCost += tech1a.getStoneCost();

            if(food >= foodCost && wood >= woodCost && stone >= stoneCost) {
                for(Technology tech1b : tier1Techs) {
                    if(tech1a.ordinal() <= tech1b.ordinal())
                        continue;

                    foodCost += tech1b.getFoodCost();
                    woodCost += tech1b.getWoodCost();
                    stoneCost += tech1b.getStoneCost();

                    if(food >= foodCost && wood >= woodCost && stone >= stoneCost) {
                        tier1Techs = new Technology[]{tech1a, tech1b};
                        return true;
                    }
                    foodCost -= tech1b.getFoodCost();
                    woodCost -= tech1b.getWoodCost();
                    stoneCost -= tech1b.getStoneCost();
                }
            }
            foodCost -= tech1a.getFoodCost();
            woodCost -= tech1a.getWoodCost();
            stoneCost -= tech1a.getStoneCost();
        }

        return false;
    }

    void checkForEnemyAttack(){
        int axemanCount = 0;
        enemies = uc.senseUnits(uc.getTeam().getOpponent());
        for (UnitInfo ui : enemies){
            if (ui.getType() == UnitType.AXEMAN)
                axemanCount++;
            if (ui.getType() == UnitType.SPEARMAN && uc.senseIllumination(ui.getLocation()) <= 10){
                fuckingSpearmen = true;
            }
        }
        if (axemanCount >= 4)
            reinforceRequested = comms.sendMiscMessage(comms.MSG_REINFORCE_BASE);
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
                uc.println("validated");
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
        else if (info == comms.MSG_REQUEST_RAFTS)
            raftsRequested = true;
    }

    boolean trySpawnUnit(UnitType type, Direction dir){
        //uc.println("canSpawn[" + dir.toString() + "] = " + canSpawn[dir.ordinal()]);
        if(canSpawn[dir.ordinal()])
            return super.trySpawnUnit(type, dir);
        return false;
    }
    void checkTrollMap() {
        UnitInfo[] enemyUnits = uc.senseUnits(uc.getOpponent());

        for(UnitInfo enemyUnit : enemyUnits) {
            if(enemyUnit.getType() == UnitType.BASE) {
                enemyBaseLocation = enemyUnit.getLocation();

                if(enemyBaseLocation != null) {
                    uc.println("TROLL MAP DETECTED ON AMERICAN SOIL. LETHAL FORCE ENGAGED");

                    for(Direction dir : dirs) {
                        Location loc = uc.getLocation().add(dir);
                        if(!uc.isOutOfMap(loc)) {
                            if(enemyBaseLocation.distanceSquared(loc) <= 18 && !uc.isObstructed(loc, enemyBaseLocation)) {
                                canSpawn[dir.ordinal()] = false;
                                uc.println("canSpawn[" + dir + "] = false");
                                uc.drawPointDebug(loc, 255, 0, 0);
                            }
                            else
                                uc.drawPointDebug(loc, 0, 255, 0);
                        }
                    }
                }
                return;
            }
        }
    }
}