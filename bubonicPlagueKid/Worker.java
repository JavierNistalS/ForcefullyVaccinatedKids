package bubonicPlagueKid;

import aic2021.user.*;

public class Worker extends MyUnit {

    Worker(UnitController uc){
        super(uc);
        pathfinding = new EvasivePathfinding(uc, this);
        comms = new Communications(uc);
        exploration = new Exploration(uc, 3, 75);
        resourceGathering = new ResourceGathering(uc, this);
    }

    // adjustable constants
    int SETTLEMENT_DISTANCE = 100;
    int SETTLEMENT_BUILD_SCORE = 850;

    // refs
    EvasivePathfinding pathfinding;
    Exploration exploration;
    Communications comms;
    ResourceGathering resourceGathering;

    // data
    Location baseLocation;
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

    void playRound() {
        timeAlive++;
        torchLighted = sustainTorch();

        updateInfo();
        readSmokeSignals();
        generalAttack();
        resourceGathering.update();
        pathfinding.updateEnemyUnits();

        if (!requestedRafts && resourceGathering.needRafts) {
            requestedRafts = comms.sendMiscMessage(comms.MSG_REQUEST_RAFTS);
            uc.println("requesting rafts");
        }

        if(uc.hasResearched(Technology.MILITARY_TRAINING, uc.getTeam()) && buildBarracks && baseLocation != null && baseLocation.distanceSquared(uc.getLocation()) <= 2) {
            UnitInfo[] nextToBase = uc.senseUnits(4);
            boolean iAmTheChosen = true;
            for (UnitInfo ui : nextToBase){
                if (ui.getType() == UnitType.WORKER && ui.getLocation().distanceSquared(baseLocation) <= 2){
                    if (ui.getID() < uc.getInfo().getID())
                        iAmTheChosen = false;
                }
            }
            if (iAmTheChosen) {
                trySpawnBarracks();
                pathfinding.move3(uc.getLocation().directionTo(baseLocation));
                pathfinding.tryMove(Direction.ZERO);
            }
        }

        if(uc.canMove()) {
            if (fullOfResources || !torchLighted) {
                resourceGathering.resetTurnCount();
                updateSettlementTarget();
                bannedBuildingDirection = Direction.ZERO;

                // TODO: use resourceGathering formula & SETTLEMENT_DISTANCE to determine settlement score

                boolean buildSettlementForFood = uc.getResource(Resource.FOOD) >= maxResourceCapacity && canBuildSettlementForFood;
                boolean buildSettlementForWood = uc.getResource(Resource.WOOD) >= maxResourceCapacity && canBuildSettlementForWood;
                boolean buildSettlementForStone = uc.getResource(Resource.STONE) >= maxResourceCapacity && canBuildSettlementForStone;
                boolean buildSettlementForResources = roundsSinceJobs > 75 || buildSettlementForFood || buildSettlementForWood || buildSettlementForStone;

                if (resourceGathering.valueForSettlementConstruction > SETTLEMENT_BUILD_SCORE && buildSettlementForResources && uc.getLocation().distanceSquared(settlements[settlementTargetIdx]) > SETTLEMENT_DISTANCE && spawnNewSettlement())
                    uc.println("no es relleno para no quitar el else");
                else {
                    pathfinding.pathfindTo(settlements[settlementTargetIdx]);
                    bannedBuildingDirection = uc.getLocation().directionTo(settlements[settlementTargetIdx]);
                    uc.drawLineDebug(uc.getLocation(), settlements[settlementTargetIdx], 255, 128, 0);
                }
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

        if(tryDeposit())
            settlementTargetIdx = -1;
    }

    void updateInfo() {
        exploration.updateChunks();

        if(uc.hasResearched(Technology.JOBS, uc.getTeam()))
            roundsSinceJobs++;

        // carried resources & max resource capacity
        getResourcesCarried = uc.getResourcesCarried();
        maxResourceCapacity = (uc.hasResearched(Technology.BOXES, uc.getTeam()) ? GameConstants.MAX_RESOURCE_CAPACITY_BOXES : GameConstants.MAX_RESOURCE_CAPACITY) - 4;
        fullOfResources =  getResourcesCarried[0] >= maxResourceCapacity
                        || getResourcesCarried[1] >= maxResourceCapacity
                        || getResourcesCarried[2] >= maxResourceCapacity;

        // units
        units = uc.senseUnits();
        closestDeer = null;
        deerMinDist = 1000000;
        anyEnemyAggroUnits = false;

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
            else if(type == UnitType.SETTLEMENT)
                addSettlementChecked(loc);
            else if(unit.getTeam() == uc.getOpponent())
                anyEnemyAggroUnits |= type.attack > 0;
            else if (type == UnitType.BARRACKS){
                buildBarracks = false;
            }
        }

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

    boolean isValidBuildingDirection(Direction dir) {
        updateBuildingObstacles();
        if(!isUpdatedBuildingDirection[dir.ordinal()])
            updateIsValidBuildingDirection(dir);

        return isValidBuildingDirection[dir.ordinal()];
    }

    void explore() {
        uc.println("exploring...");
        Location exploreLoc = exploration.getLocation();
        if(exploreLoc == null) {
            exploration = new Exploration(uc, exploration.CHUNK_SIZE, exploration.RESET_TURNS);
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
    }

    boolean spawnNewSettlement() {
        uc.println("spawning new settlement");
        Location newSettlementLoc = trySpawnInValidAndReturnLocation(UnitType.SETTLEMENT);
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
    mainLoop:
        for (Direction dir : dirs) {
            if(!dir.isEqual(bannedBuildingDirection)) {
                Location loc = uc.getLocation().add(dir);
                if ((enemyBaseLocation == null || enemyBaseLocation.distanceSquared(loc) > 18 || (uc.canSenseLocation(enemyBaseLocation) && uc.isObstructed(enemyBaseLocation, loc))) && uc.canSpawn(type, dir) && uc.canSenseLocation(loc) && !uc.hasTrap(loc)) {
                    ResourceInfo[] locResources = uc.senseResourceInfo(loc);
                    for(ResourceInfo resource : locResources)
                        if(resource != null && resource.amount > 9)
                            continue mainLoop;
                    if(isValidBuildingDirection(dir)) {
                        uc.spawn(type, dir);
                        lastValid = uc.getRound();
                        return loc;
                    }
                }
            }
        }
        return null;
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
        if (timeAlive < 100 || uc.getRandomDouble() > 0.1)
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
}
