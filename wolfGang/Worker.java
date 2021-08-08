package wolfGang;

import aic2021.user.*;

public class Worker extends MyUnit {

    Worker(UnitController uc){
        super(uc);
        pathfinding = new Pathfinding(uc, this);
        comms = new Communications(uc);
        exploration = new Exploration(uc, 3, 4);
        resourceGathering = new ResourceGathering(uc, this);
    }

    // adjustable constants
    int SETTLEMENT_DISTANCE = 64;
    int SETTLEMENT_BUILD_SCORE = 850;

    int STONE_SPAM = 69;

    // refs
    Pathfinding pathfinding;
    Exploration exploration;
    Communications comms;
    ResourceGathering resourceGathering;

    // data
    Location[] settlements = new Location[256];
    int settlementsLength = 0, settlementTargetIdx = -1;
    int farmCount, quarryCount, sawmillCount;
    int lastValid = 0;
    boolean buildBarracks = true;

    // updateInfo data
    int maxResourceCapacity;
    boolean fullOfResources;
    int[] getResourcesCarried;
    int totalRes;
    Location closestDeer;
    int deerMinDist;
    UnitInfo[] units;
    ResourceInfo[] resourceInfos;
    boolean[] resourceInfosOccupied;
    boolean anyEnemyAggroUnits = false;

    boolean canBuildFarm = true, canBuildSawmill = true, canBuildQuarry = true;
    int farmUpdateRound = -10, sawmillUpdateRound = -10, quarryUpdateRound = -10;
    boolean canBuildSettlementForFood = true, canBuildSettlementForWood = true, canBuildSettlementForStone = true;
    int roundsSinceJobs = -1; // >= 0 means already researched
    boolean requestedRafts = false;
    boolean torchLighted = false;

    int timeAlive = 0;
    int lastUpdatedBuildingObstaclesRound = 9;
    boolean[][] buildingObstacles;
    boolean[] isValidBuildingDirection;
    boolean[] isUpdatedBuildingDirection;
    Direction bannedBuildingDirection = Direction.ZERO;
    boolean enemyUnitsPresent = false;
    int totalResources = 0;

    void playRound() {
        timeAlive++;
        torchLighted = sustainTorch();

        tryGather();
        updateInfo();
        readSmokeSignals();
        generalAttack();
        resourceGathering.update();

        if (!requestedRafts && resourceGathering.needRafts) {
            requestedRafts = comms.sendMiscMessage(comms.MSG_REQUEST_RAFTS);
            uc.println("requesting rafts");
        }

        if(uc.hasResearched(Technology.MILITARY_TRAINING, uc.getTeam()) && buildBarracks && baseLocation != null && baseLocation.distanceSquared(uc.getLocation()) <= 4) {
            UnitInfo[] nextToBase = uc.senseUnits(4);
            boolean iAmTheChosen = true;
            for (UnitInfo ui : nextToBase) {
                if (ui.getType() == UnitType.WORKER && ui.getLocation().distanceSquared(baseLocation) <= 4){
                    if (ui.getID() < uc.getInfo().getID())
                        iAmTheChosen = false;
                }
            }
            if (iAmTheChosen) {
                uc.println("I AM THE CHOSEN ONE");

                if(uc.getLocation().distanceSquared(baseLocation) <= 2) {
                    int buildingSpots = 0;
                    for(Direction dir : dirs) {
                        Location loc = baseLocation.add(dir);
                        if(dir == Direction.ZERO)
                            continue;
                        if (uc.canSenseLocation(loc) && loc.distanceSquared(uc.getLocation()) <= 2 && (!uc.hasMountain(loc) && !uc.hasWater(loc))) {
                            buildingSpots++;
                            uc.drawLineDebug(uc.getLocation(), loc, 255, 0, 255);
                        }
                    }
                    uc.println("buildingSpots: " + buildingSpots);
                    if(buildingSpots == 1) { // i'm in the only spot
                        uc.println("i'm in the only spot");
                        if(uc.canMove()) {
                            pathfinding.move3(baseLocation.directionTo(uc.getLocation()));
                            for(Direction dir : dirs) {
                                Location loc = uc.getLocation().add(dir);
                                if(loc.distanceSquared(baseLocation) <= 2 && trySpawnUnit(UnitType.BARRACKS, dir))
                                    break;
                            }
                        }
                    }
                    else {
                        pathfinding.move3(uc.getLocation().directionTo(baseLocation));
                        pathfinding.tryMove(Direction.ZERO);
                        for(Direction dir : dirs) {
                            Location loc = uc.getLocation().add(dir);
                            if(loc.distanceSquared(baseLocation) <= 2 && isValidBuildingDirection(dir) && trySpawnUnit(UnitType.BARRACKS, dir))
                                break;
                        }
                    }
                }
            }
        }

        if(uc.canMove()) {
            if (fullOfResources || !torchLighted || (totalResources > 50 && resourceGathering.targetResourceValue == 0)) {
                resourceGathering.resetTurnCount();
                updateSettlementTarget();
                bannedBuildingDirection = Direction.ZERO;

                int[] resources = uc.getResourcesCarried();
                int food = resources[Resource.FOOD.ordinal()];
                int wood = resources[Resource.WOOD.ordinal()];
                int stone = resources[Resource.STONE.ordinal()];

                boolean needsRafts = !uc.hasResearched(Technology.RAFTS, uc.getTeam()) && requestedRafts;
                boolean buildSettlementForFood = !needsRafts && food >= maxResourceCapacity && canBuildSettlementForFood;
                boolean buildSettlementForWood = wood >= maxResourceCapacity && canBuildSettlementForWood;
                boolean buildSettlementForStone = !needsRafts && stone >= maxResourceCapacity && canBuildSettlementForStone;
                boolean buildSettlementForResources = roundsSinceJobs > 75 || buildSettlementForFood || buildSettlementForWood || buildSettlementForStone;

                if (buildSettlementForResources && resourceGathering.valueForSettlementConstruction > SETTLEMENT_BUILD_SCORE && uc.getLocation().distanceSquared(settlements[settlementTargetIdx]) > SETTLEMENT_DISTANCE && spawnNewSettlement())
                    uc.println("no es relleno para no quitar el else");
                else {
                    pathfinding.pathfindTo(settlements[settlementTargetIdx]);
                    bannedBuildingDirection = uc.getLocation().directionTo(settlements[settlementTargetIdx]);
                    uc.drawLineDebug(uc.getLocation(), settlements[settlementTargetIdx], 255, 128, 0);
                }
            }
            else if (enemyUnitsPresent){
                micro();
            }
            else {
                if(closestDeer != null && resourceGathering.effectiveValue(500, Resource.FOOD, closestDeer) > resourceGathering.targetResourceValue) {
                    huntDeer(closestDeer);
                    bannedBuildingDirection = uc.getLocation().directionTo(closestDeer);
                    resourceGathering.resetTurnCount();
                }
                else {
                    Location targetResource = resourceGathering.getLocation();
                    if(targetResource != null) {
                        uc.println("going to resource @ " + targetResource);

                        if (uc.getLocation().distanceSquared(targetResource) > 0) {
                            pathfinding.pathfindTo(targetResource);
                            resourceGathering.countTurn();
                        }


                        if(targetResource.isEqual(uc.getLocation()))
                            resourceGathering.resetTurnCount();
                        else {
                            pathfinding.pathfindTo(targetResource);
                            resourceGathering.countTurn();
                        }
                        bannedBuildingDirection = uc.getLocation().directionTo(targetResource);

                        uc.drawLineDebug(uc.getLocation(), targetResource, 255, 255, 0);
                    }
                    else
                        explore();
                }
            }
        }

        if (!anyEnemyAggroUnits)
            buildEconBuildings();

        tryGather();

        if(tryDeposit())
            settlementTargetIdx = -1;

        tryGather();
        generalAttack();
    }

    void updateInfo() {
        exploration.updateChunks();

        if(uc.hasResearched(Technology.JOBS, uc.getTeam()))
            roundsSinceJobs++;

        // carried resources & max resource capacity
        getResourcesCarried = uc.getResourcesCarried();
        maxResourceCapacity = (uc.hasResearched(Technology.BOXES, uc.getTeam()) ? GameConstants.MAX_RESOURCE_CAPACITY_BOXES : GameConstants.MAX_RESOURCE_CAPACITY) - 4;
        totalResources = getResourcesCarried[0] + getResourcesCarried[1] + getResourcesCarried[2];
        fullOfResources =  getResourcesCarried[0] >= maxResourceCapacity
                        || getResourcesCarried[1] >= maxResourceCapacity
                        || getResourcesCarried[2] >= maxResourceCapacity;

        // units
        units = uc.senseUnits();
        closestDeer = null;
        deerMinDist = 1000000;
        anyEnemyAggroUnits = false;
        enemyUnitsPresent = false;

    unitLoop:
        for (UnitInfo unit : units) {
            Location loc = unit.getLocation();
            int dist = loc.distanceSquared(uc.getLocation());
            UnitType type = unit.getType();

            if (type == UnitType.DEER && dist < deerMinDist) {
                closestDeer = loc;
                deerMinDist = dist;
            }
            else if(type == UnitType.BASE){
                 if(unit.getTeam() == uc.getTeam()) {
                     baseLocation = loc;
                     addSettlementChecked(loc);
                 }
                 else if(enemyBaseLocation == null) { // el worker ve la base enemiga (posible f)
                     if (comms.sendLocationMessage(comms.MSG_TYPE_ENEMY_BASE, loc)){
                         enemyBaseLocation = loc;
                     }
                 }
            }
            else if(type == UnitType.SETTLEMENT && unit.getTeam() == uc.getTeam())
                addSettlementChecked(loc);
            else if(unit.getTeam() == uc.getOpponent() && !uc.isObstructed(loc, uc.getLocation())) {
                anyEnemyAggroUnits |= type.attack > 0;
                enemyUnitsPresent = true;
            }
            else if (type == UnitType.BARRACKS){
                buildBarracks = false;
            }
        }

        readRocks();

        // resourceInfosOccupied
        resourceInfos = uc.senseResources();
        resourceInfosOccupied = new boolean[resourceInfos.length];
        for(int i = 0; i < resourceInfos.length; i++)
            resourceInfosOccupied[i] = uc.senseUnitAtLocation(resourceInfos[i].location) != null;

        // resourceInfos
        totalRes = 0;
        for (int i = 0; i < resourceInfos.length; i++) {
            if(!resourceInfosOccupied[i] && (baseLocation == null || baseLocation.distanceSquared(resourceInfos[i].getLocation()) > 18) && !uc.hasTrap(resourceInfos[i].getLocation())) {
                totalRes += resourceInfos[i].amount;
            }
        }
    }

    void updateBuildingObstacles() {
        if(uc.getRound() > lastUpdatedBuildingObstaclesRound) {
            uc.println("updateBuildingObstacles");

            int x = uc.getLocation().x, y = uc.getLocation().y;
            buildingObstacles = new boolean[5][5];

            for(int i = 0; i < 5; i++) {
                for(int j = 0; j < 5; j++) {
                    Location loc = uc.getLocation().add(i - 2, j - 2);
                    if(uc.isOutOfMap(loc))
                        buildingObstacles[i][j] = true;
                    else if(uc.canSenseLocation(loc)) {
                        if(uc.hasMountain(loc) || (!uc.hasResearched(Technology.RAFTS, uc.getTeam()) && uc.hasWater(loc))) {
                            buildingObstacles[i][j] = true;
                        }
                        else {
                            UnitInfo unit = uc.senseUnitAtLocation(loc);
                            buildingObstacles[i][j] = unit != null && unit.getType().isStructure() && unit.getType() != UnitType.SETTLEMENT;
                        }
                    }

                    if(buildingObstacles[i][j])
                        uc.drawPointDebug(loc, 255, 0, 0);
                    else
                        uc.drawPointDebug(loc, 0, 0, 0);
                }
            }

            isValidBuildingDirection = new boolean[9];
            isUpdatedBuildingDirection = new boolean[9];
            lastUpdatedBuildingObstaclesRound = uc.getRound();
        }
    }

    void updateIsValidBuildingDirection(Direction dir) {
        uc.println("updateIsValidBuildingDirection: " + dir);
        isUpdatedBuildingDirection[dir.ordinal()] = true;

        Direction[] dirs = {Direction.NORTH, Direction.NORTHWEST, Direction.WEST, Direction.SOUTHWEST, Direction.SOUTH, Direction.SOUTHEAST, Direction.EAST, Direction.NORTHEAST};

        int offsetX = dir.dx + 2;
        int offsetY = dir.dy + 2;

        boolean[] obstacle = new boolean[8];
        for(int i = 0; i < 8; i++)
            obstacle[i] = buildingObstacles[dirs[i].dx + offsetX][dirs[i].dy + offsetY];

        isValidBuildingDirection[dir.ordinal()] =
               (obstacle[1] || !(obstacle[0] && obstacle[2]))
            && (obstacle[3] || !(obstacle[2] && obstacle[4]))
            && (obstacle[5] || !(obstacle[4] && obstacle[6]))
            && (obstacle[7] || !(obstacle[6] && obstacle[0]))
            && (obstacle[0] || !((obstacle[6] || obstacle[7]) && (obstacle[1] || obstacle[2])))
            && (obstacle[2] || !((obstacle[0] || obstacle[1]) && (obstacle[3] || obstacle[4])))
            && (obstacle[4] || !((obstacle[2] || obstacle[3]) && (obstacle[5] || obstacle[6])))
            && (obstacle[6] || !((obstacle[4] || obstacle[5]) && (obstacle[7] || obstacle[0])));
    }

    void explore() {
        uc.println("exploring...");
        Location exploreLoc = exploration.getLocation();
        if(exploreLoc == null) {
            exploration = new Exploration(uc, exploration.CHUNK_SIZE, exploration.MOVEMENT_MULT);
            bannedBuildingDirection = Direction.ZERO;
        }
        else {
            pathfinding.pathfindTo(exploreLoc);
            bannedBuildingDirection = uc.getLocation().directionTo(exploreLoc);
            uc.drawLineDebug(uc.getLocation(), exploreLoc, 0, 0, 255);
        }

    }

    void readSmokeSignals() {
        uc.println("reading smoke signals");
        int[] smokeSignals = uc.readSmokeSignals();

        for(int smokeSignal : smokeSignals) {
            int bytecode = uc.getEnergyLeft();
            int msg = comms.decrypt(smokeSignal);
            if(comms.validate(msg)) {
                int msgType = comms.getType(msg);
                if(msgType == comms.MSG_TYPE_ALLIED_SETTLEMENT)
                    addSettlementChecked(comms.intToLocation(comms.getInfo(msg)));
                else if (msgType == comms.MSG_TYPE_ENEMY_BASE)
                    enemyBaseLocation = comms.intToLocation(msg);
                else if(msgType == comms.MSG_TYPE_MISC)
                    readMiscMessage(comms.getInfo(msg));
            }
            int bytecodeLeft = uc.getEnergyLeft();
            uc.println("bytecode spent in comms: " + (bytecode - bytecodeLeft));
        }
    }

    void readMiscMessage(int info) {
        if (info == comms.MSG_STOP_BUILDING_SETTLEMENT_TO_COLLECT_FOOD)
            canBuildSettlementForFood = false;
        if (info == comms.MSG_STOP_BUILDING_SETTLEMENT_TO_COLLECT_WOOD)
            canBuildSettlementForWood = false;
        if (info == comms.MSG_STOP_BUILDING_SETTLEMENT_TO_COLLECT_STONE)
            canBuildSettlementForStone = false;
        if (info == comms.MSG_START_BUILDING_SAWMILLS) {
            canBuildSawmill = true;
            sawmillUpdateRound = uc.getRound();
        }
        if (info == comms.MSG_STOP_BUILDING_SAWMILLS && sawmillUpdateRound < uc.getRound())
            canBuildSawmill = false;
        if (info == comms.MSG_START_BUILDING_FARMS) {
            canBuildFarm = true;
            farmUpdateRound = uc.getRound();
        }
        if (info == comms.MSG_STOP_BUILDING_FARMS && farmUpdateRound < uc.getRound())
            canBuildFarm = false;
        if (info == comms.MSG_START_BUILDING_QUARRYS) {
            canBuildQuarry = true;
            quarryUpdateRound = uc.getRound();
        }
        if (info == comms.MSG_STOP_BUILDING_QUARRYS && quarryUpdateRound < uc.getRound())
            canBuildQuarry = false;
        if (info == comms.MSG_BARRACKS_START)
            buildBarracks = false;
        if (info == comms.MSG_REQUEST_RAFTS)
            requestedRafts = true;
    }

    boolean spawnNewSettlement() {
        uc.println("spawning new settlement");
        Location newSettlementLoc = trySpawnInValidWithMarginAndReturnLocation(UnitType.SETTLEMENT);
        if(newSettlementLoc != null) {
            uc.drawLineDebug(uc.getLocation(), newSettlementLoc, 0, 255, 255);
            addSettlementUnchecked(newSettlementLoc);
            return true;
        }
        return false;
    }

    boolean huntDeer(Location closestDeer) {
        if (uc.canMove() && closestDeer != null) {
            uc.println("attacking deer");
            uc.drawLineDebug(uc.getLocation(), closestDeer, 0, 255, 0);
            pathfinding.pathfindTo(closestDeer);
            if(uc.canAttack(closestDeer))
                uc.attack(closestDeer);
            return true;
        }
        return false;
    }

    void addSettlementUnchecked(Location settlementLoc) {
        uc.println("Added settlements[" + settlementsLength + "]: [" + settlementLoc.x + ", " + settlementLoc.y + "]");

        // if we have a target & this settlement is closer, change target to this settlement
        if(settlementTargetIdx != -1 && settlementLoc.distanceSquared(uc.getLocation()) < settlements[settlementTargetIdx].distanceSquared(uc.getLocation())) {
            settlementTargetIdx = settlementsLength;
        }

        settlements[settlementsLength] = settlementLoc;
        settlementsLength++;
    }

    boolean addSettlementChecked(Location settlementLoc) {
        for(int i = 0; i < settlementsLength; i++) // check if the settlement is new
            if(settlements[i].isEqual(settlementLoc))
                return false;

        addSettlementUnchecked(settlementLoc);
        return true;
    }

    boolean removeSettlement(Location settlementLoc) {
        for(int i = 0; i < settlementsLength; i++) // check if the settlement is new
            if(settlements[i].isEqual(settlementLoc)) {
                removeSettlement(i);
                return true;
            }
        return false;
    }
    void removeSettlement(int idx) {
        uc.println("Removed settlements[" + idx + "]: [" + settlements[idx].x + ", " + settlements[idx].y + "]");
        settlementsLength--;
        settlements[idx] = settlements[settlementsLength];
        settlements[settlementsLength] = null;
        if(settlementTargetIdx == settlementsLength)
            settlementTargetIdx = idx;
    }

    boolean trySpawnInValid(UnitType type) {
        return trySpawnInValidAndReturnLocation(type) != null;
    }
    Location trySpawnInValidAndReturnLocation(UnitType type) {
        int distMargin = (int)(baseLocation == null ? 50d : Math.sqrt(uc.getLocation().distanceSquared(baseLocation))) * 2;
        if(canBuildUnitWithMargin(type, distMargin, distMargin, distMargin)) {
            for (Direction dir : dirs) {
                if(uc.canSpawn(type, dir) && isValidBuildingDirection(dir)) {
                    uc.spawn(type, dir);
                    lastValid = uc.getRound();
                    return uc.getLocation();
                }
            }
        }
        return null;
    }

    boolean trySpawnInValidWithMargin(UnitType type) {
        return trySpawnInValidAndReturnLocation(type) != null;
    }
    Location trySpawnInValidWithMarginAndReturnLocation(UnitType type) {
        for (Direction dir : dirs) {
            if(uc.canSpawn(type, dir) && isValidBuildingDirection(dir) && trySpawnWithMargin(type, dir)) {
                lastValid = uc.getRound();
                return uc.getLocation();
            }
        }
        return null;
    }

    boolean isValidBuildingDirection(Direction dir) {
        if (!dir.isEqual(bannedBuildingDirection)) {
            Location loc = uc.getLocation().add(dir);
            if ((enemyBaseLocation == null || enemyBaseLocation.distanceSquared(loc) > 18 || (uc.canSenseLocation(enemyBaseLocation) && uc.isObstructed(enemyBaseLocation, loc))) && uc.canSenseLocation(loc) && !uc.hasTrap(loc)) {
                ResourceInfo[] locResources = uc.senseResourceInfo(loc);
                for (ResourceInfo resource : locResources)
                    if (resource != null && resource.amount > 9)
                        return false;

                updateBuildingObstacles();
                if (!isUpdatedBuildingDirection[dir.ordinal()])
                    updateIsValidBuildingDirection(dir);
                return isValidBuildingDirection[dir.ordinal()];
            }
        }
        return false;
    }


    boolean tryDeposit() {
        if(uc.canDeposit()) {
            uc.deposit();
            return true;
        }
        return false;
    }

    void updateSettlementTarget() {
        if(settlementTargetIdx != -1 && uc.canSenseLocation(settlements[settlementTargetIdx])) {
            UnitInfo unitInfo = uc.senseUnitAtLocation(settlements[settlementTargetIdx]);
            if(unitInfo == null || (unitInfo.getType() != UnitType.SETTLEMENT && unitInfo.getType() != UnitType.BASE) || unitInfo.getTeam() != uc.getTeam()) {
                removeSettlement(settlementTargetIdx);
                settlementTargetIdx = -1;
            }
        }

        if(settlementTargetIdx == -1) {
            int minDist = 10000000;
            for (int i = 0; i < settlementsLength; i++) {
                int dist = settlements[i].distanceSquared(uc.getLocation());
                if (dist < minDist) {
                    settlementTargetIdx = i;
                    minDist = dist;
                }
            }
        }
    }

    void trySpawnBarracks(){
        for (Direction dir : dirs){
            if (uc.canSpawn(UnitType.BARRACKS, dir)){
                Location loc = uc.getLocation().add(dir);
                if (loc.distanceSquared(baseLocation) <= 2 && trySpawnUnit(UnitType.BARRACKS, dir)) {
                    buildBarracks = false;
                    break;
                }
            }
        }
    }

    void buildEconBuildings() {
        if (timeAlive < 10)
            return;
        if (uc.getRound() > 1650)
            return;
        uc.println("canBuildSawmill: " + canBuildSawmill);
        uc.println("canBuildFarm: " + canBuildFarm);
        uc.println("canBuildQuarry: " + canBuildQuarry);
        if(canBuildSawmill && trySpawnInValid(UnitType.SAWMILL))
            sawmillCount++;
        if(canBuildQuarry && trySpawnInValid(UnitType.QUARRY))
            quarryCount++;
        if(canBuildFarm && trySpawnInValid(UnitType.FARM))
            farmCount++;
    }

    void tryGather(){
        if (uc.canGatherResources()){
            int total = 0;
            ResourceInfo[] resources = uc.senseResourceInfo(uc.getLocation());
            for (ResourceInfo ri : resources){
                if (ri != null)
                    total += ri.amount;
            }
            if (total > 0)
                uc.gatherResources();
        }
    }

    float unitCount(){
        UnitInfo[] units = uc.senseUnits();
        float score = 1;
        for (UnitInfo ui : units){
            if (uc.getTeam() == ui.getTeam()){
                if (ui.getType() == UnitType.WORKER){
                    score += 1;
                }
                else if (ui.getType() == UnitType.WOLF){
                    score += 3;
                }
            }
            else if (uc.getTeam() == ui.getTeam().getOpponent()){
                if (ui.getType() == UnitType.WORKER){
                    score -= 1;
                }
                else if (ui.getType() == UnitType.WOLF){
                    score -= 4;
                }
                else if (ui.getAttack() > 0){
                    score -= 1000;
                }
            }
        }
        return score;
    }

    void getReinforcements(){
        UnitInfo[] allies = uc.senseUnits(uc.getTeam());
        boolean settlement = false;
        for (UnitInfo ui : allies){
            if (ui.getType() == UnitType.SETTLEMENT)
                settlement = true;
        }
        if (!settlement){
            trySpawnUnit(UnitType.SETTLEMENT);
        }
    }

    void micro(){
        uc.println("doing micro");
        int bytecode = uc.getEnergyLeft();
        if (uc.canMove()){
            UnitInfo[] enemies = uc.senseUnits(uc.getTeam().getOpponent());
            double bestScore = -1e9;
            Direction bestDir = Direction.ZERO;
            double countUnitsScore = unitCount();
            uc.println("countUnitsScore: " + countUnitsScore);
            if (countUnitsScore < 0 && countUnitsScore >= -3){
                getReinforcements();
            }
            for (Direction dir : dirs){
                if (pathfinding.canMove(dir)){
                    Location loc = uc.getLocation().add(dir);
                    double score = 0;
                    boolean canHitAggro = false;
                    boolean canHit = false;
                    for (UnitInfo ui : enemies){
                        boolean obstructed = uc.isObstructed(loc, ui.getLocation());
                        int distSqr = loc.distanceSquared(ui.getLocation());
                        UnitType type = ui.getType();
                        if (type == UnitType.WOLF || type == UnitType.SPEARMAN || type == UnitType.AXEMAN){
                            score += Math.sqrt(distSqr)*100;
                        }
                        else{
                            score -= Math.sqrt(distSqr)*30*countUnitsScore;
                        }
                        if (type == UnitType.WORKER && !ui.isBeingConstructed() && countUnitsScore < 0){
                            score += Math.sqrt(distSqr)*100;
                        }
                        if (!obstructed && loc.distanceSquared(ui.getLocation()) <= type.attackRange && !ui.isBeingConstructed())
                            score -= ui.getAttack()*100;
                        if (!obstructed && loc.distanceSquared(ui.getLocation()) <= type.attackRange + 8 && countUnitsScore < 3 && !ui.isBeingConstructed()){
                            score -= ui.getAttack()*50;
                        }
                        if (distSqr <= 5 && !obstructed && uc.canAttack()) {
                            canHit = true;
                            if (ui.getAttack() > 0)
                                canHitAggro = true;
                        }
                    }
                    uc.println("canHit: " + canHit);
                    uc.println("canHitAggro: " + canHitAggro);
                    if (canHit)
                        score += 200;
                    if (canHitAggro && countUnitsScore > 0)
                        score += 1000;
                    if (score > bestScore){
                        bestScore = score;
                        bestDir = dir;
                    }
                    uc.println(dir + " score: " + score);
                }
            }
            if (bestDir != Direction.ZERO){
                uc.move(bestDir);
            }
        }
        int bytecodeUsed = bytecode - uc.getEnergyLeft();
        uc.println("micro bytecode: " + (bytecodeUsed > 0 ? bytecodeUsed : 15000+bytecodeUsed));
    }
}
