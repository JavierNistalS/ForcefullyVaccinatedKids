package bubonicPlagueKid;

import aic2021.user.*;

public class Worker extends MyUnit {

    Worker(UnitController uc){
        super(uc);
        pathfinding = new EvasivePathfinding(uc, this);
        comms = new Communications(uc);
        exploration = new Exploration(uc, 3, 75);
        resourceGathering = new ResourceGathering(uc, this);
        uc.println("im alive!");
    }

    // adjustable constants
    int SETTLEMENT_DISTANCE = 100;

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

    int roundsChasingResource = 0;

    int timeAlive = 0;
    int lastUpdatedBuildingObstaclesRound = 9;
    boolean[][] buildingObstacles;
    boolean[] isValidBuildingDirection;

    void playRound() {
        timeAlive++;
        sustainTorch();
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
            uc.println("can move");
            if (fullOfResources) {
                uc.println("full of resources");
                roundsChasingResource = 0;
                updateSettlementTarget();

                // TODO: use resourceGathering formula & SETTLEMENT_DISTANCE to determine settlement score

                boolean buildSettlementForFood = uc.getResource(Resource.FOOD) >= maxResourceCapacity && canBuildSettlementForFood;
                boolean buildSettlementForWood = uc.getResource(Resource.WOOD) >= maxResourceCapacity && canBuildSettlementForWood;
                boolean buildSettlementForStone = uc.getResource(Resource.STONE) >= maxResourceCapacity && canBuildSettlementForStone;
                boolean buildSettlementForResources = totalRes > 200 || roundsSinceJobs > 75 || buildSettlementForFood || buildSettlementForWood || buildSettlementForStone;

                if (buildSettlementForResources && uc.getLocation().distanceSquared(settlements[settlementTargetIdx]) > SETTLEMENT_DISTANCE && spawnNewSettlement())
                    uc.println("no es relleno para no quitar el else");
                else {
                    pathfinding.pathfindTo(settlements[settlementTargetIdx]);
                    uc.drawLineDebug(uc.getLocation(), settlements[settlementTargetIdx], 255, 128, 0);
                }
            }
            else {
                uc.println("going to resources");
                if(closestDeer != null && resourceGathering.effectiveValue(500, Resource.FOOD, closestDeer) > resourceGathering.targetResourceValue)
                    huntDeer(closestDeer);
                else {
                    Location targetResource = resourceGathering.getLocation();
                    if(targetResource != null) {
                        uc.println("going to resource @ " + targetResource);
                        pathfinding.pathfindTo(targetResource);
                        resourceGathering.countTurn();
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
        updateIsValidBuildingDirection();

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

    void updateIsValidBuildingDirection() {
        int x = uc.getLocation().x, y = uc.getLocation().y;
        buildingObstacles = new boolean[5][5];
        for(int i = x - 2; i <= x + 2; i++) {
            for(int j = y - 2; j <= y + 2; j++) {
                Location loc = uc.getLocation().add(i, j);
                if(uc.canSenseLocation(loc)) {
                    if(uc.hasMountain(loc) || (!uc.hasResearched(Technology.RAFTS, uc.getTeam()) && uc.hasWater(loc))) {
                        buildingObstacles[i + 2 - x][j + 2 - y] = true;
                    }
                    else {
                        UnitInfo unit = uc.senseUnitAtLocation(loc);
                        buildingObstacles[i + 2 - x][j + 2 - y] = unit != null && unit.getType().isStructure() && unit.getType() != UnitType.SETTLEMENT;
                    }
                }

                if(buildingObstacles[i + 2 - x][j + 2 - y])
                    uc.drawPointDebug(loc, 255, 0, 0);
            }
        }

        isValidBuildingDirection = new boolean[9];
        for(Direction dir : dirs) {
            if(isValidBuildingDirection[dir.ordinal()] = updateIsValidBuildingDirection(uc.getLocation().add(dir)))
                return;
        }
    }

    boolean updateIsValidBuildingDirection(Location loc) {
        uc.println("isValid: [" + loc.x + ", " + loc.y + "]"); // TODO: make settlements "traversable", so that they don't get surrounded by buildings

        if (enemyBaseLocation != null && enemyBaseLocation.distanceSquared(loc) <= 18)
            return false;
        if (!uc.isOutOfMap(loc) && uc.canSenseLocation(loc)) {
            ResourceInfo[] res = uc.senseResourceInfo(loc);

            for(ResourceInfo resInfo : res) {
                if(resInfo != null && resInfo.amount > 0)
                    return false;
            }

            //return (loc.x + loc.y) % 2 == 0;

            Direction[] dirs = {Direction.NORTH, Direction.NORTHWEST, Direction.WEST, Direction.SOUTHWEST, Direction.SOUTH, Direction.SOUTHEAST, Direction.EAST, Direction.NORTHEAST};
            boolean[] traversable = {false, false, false, false, false, false, false, false};

            int x = uc.getLocation().x, y = uc.getLocation().y;
            for(int i = 0; i < 8; i++) {
                traversable[i] = buildingObstacles[loc.x + dirs[i].dx - x + 2][loc.y + dirs[i].dy - y + 2];
            }

            return (traversable[1] || !(traversable[0] && traversable[2]))
                && (traversable[3] || !(traversable[2] && traversable[4]))
                && (traversable[5] || !(traversable[4] && traversable[6]))
                && (traversable[7] || !(traversable[6] && traversable[0]))
                && (traversable[0] || !((traversable[6] || traversable[7]) && (traversable[1] || traversable[2])))
                && (traversable[2] || !((traversable[0] || traversable[1]) && (traversable[3] || traversable[4])))
                && (traversable[4] || !((traversable[2] || traversable[3]) && (traversable[5] || traversable[6])))
                && (traversable[6] || !((traversable[4] || traversable[5]) && (traversable[7] || traversable[0])));
        }
        return false;
        //return baseLocation != null && loc.distanceSquared (baseLocation) > 1 || (uc.getRound() > 400 && lastValid + 3< uc.getRound());
    }

    boolean isValidBuildingDirection(Direction dir){
        if(lastUpdatedBuildingObstaclesRound < uc.getRound()) {
            updateIsValidBuildingDirection();
            lastUpdatedBuildingObstaclesRound = uc.getRound();
        }

        return isValidBuildingDirection[dir.ordinal()];
    }

    // prioritizes food
    Location findClosestResource(ResourceInfo[] resourceInfos, boolean[] resourceInfosOccupied) {
        Location closest = null;
        Location closestFood = null;
        int closestDist = 1000000;
        int closestFoodDist = 10000000;

        for(int i = 0; i < resourceInfos.length; i++) {
            if(!resourceInfosOccupied[i] && !uc.hasTrap(resourceInfos[i].getLocation()) && (enemyBaseLocation == null || enemyBaseLocation.distanceSquared(resourceInfos[i].getLocation()) > 18)) {
                Location loc = resourceInfos[i].location;
                int dist = loc.distanceSquared(uc.getLocation());
                if (dist < closestDist) {
                    closest = loc;
                    closestDist = dist;
                }
                if (resourceInfos[i].resourceType == Resource.FOOD && dist < closestFoodDist) {
                    closestFood = loc;
                    closestFoodDist = dist;
                }
            }
        }

        if(closestFood != null)
            return closestFood;
        return closest;
    }

    void explore() {
        uc.println("exploring...");
        Location exploreLoc = exploration.getLocation();
        if(exploreLoc == null) {
            exploration = new Exploration(uc, exploration.CHUNK_SIZE, exploration.RESET_TURNS);
        }
        else {
            pathfinding.pathfindTo(exploreLoc);
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
        for (Direction dir : dirs) {
            if (uc.canSpawn(type, dir) && isValidBuildingDirection(dir)) {
                uc.spawn(type, dir);
                lastValid = uc.getRound();
                return true;
            }
        }
        return false;
    }
    Location trySpawnInValidAndReturnLocation(UnitType type) {
        for (Direction dir : dirs) {
            if (uc.canSpawn(type, dir) && isValidBuildingDirection(dir)) {
                uc.spawn(type, dir);
                lastValid = uc.getRound();
                return uc.getLocation().add(dir);
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