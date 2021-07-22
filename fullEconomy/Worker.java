package fullEconomy;

import aic2021.user.*;

public class Worker extends MyUnit {

    Worker(UnitController uc){
        super(uc);
        buildBarracks = uc.getRound() > 400;
        pathfinding = new Pathfinding(uc);
        comms = new Communications(uc);
    }

    // adjustable constants
    int SETTLEMENT_DISTANCE = 25;

    // refs
    Pathfinding pathfinding;
    Exploration exploration;
    Communications comms;

    // data
    Location baseLocation;
    Location[] settlements = new Location[256];
    int settlementsLength = 0, settlementTargetIdx = -1;
    int farmCount, quarryCount, sawmillCount;
    int lastValid = 0;
    boolean buildBarracks = false;

    // updateInfo data
    int maxResourceCapacity, carriedRes;
    boolean fullOfResources;
    int[] getResourcesCarried;
    int localFood, localResourceTotal, totalRes;
    boolean anyFood;
    Location closestDeer;
    int deerMinDist;
    UnitInfo[] units;
    ResourceInfo[] resourceInfos;
    ResourceInfo[] localResourceInfos;
    boolean[] resourceInfosOccupied;

    void playRound() {
        sustainTorch();
        updateInfo();
        updateExploration();

        if(!uc.hasResearched(Technology.JOBS, uc.getTeam())) { // NOT researched jobs
            if(!anyFood)
                huntDeer(closestDeer);

            if(localResourceTotal > 0 && !fullOfResources) {
                uc.println("gathering resources");
                uc.gatherResources();
                settlementTargetIdx = -1;
                uc.drawPointDebug(uc.getLocation(), 255, 255, 0);
            }
            else if(uc.canMove()) {
                if(fullOfResources) {
                    updateSettlementTarget();

                    if(uc.getLocation().distanceSquared(settlements[settlementTargetIdx]) > SETTLEMENT_DISTANCE)
                        spawnNewSettlement();
                    else
                        pathfinding.pathfindTo(settlements[settlementTargetIdx]);
                }
                else if(totalRes == 0)
                    explore();
                else { // some resources
                    uc.println("going to resources");

                    Location closestRes = findClosestResource(resourceInfos, resourceInfosOccupied);
                    pathfinding.pathfindTo(closestRes);
                    uc.drawLineDebug(uc.getLocation(), closestRes, 255, 255, 0);
                }
            }
        }
        else { // researched jobs


            if(uc.getLocation().distanceSquared(baseLocation) > 100)
                pathfinding.pathfindTo(baseLocation);
            else {
                if(buildBarracks && trySpawnInValid(UnitType.BARRACKS))
                    buildBarracks = false;

                buildEconBuildings();
            }

        }

        if(tryDeposit())
            settlementTargetIdx = -1;

    }

    void readSmokeSignals() {
        int[] smokeSignals = uc.readSmokeSignals();

        for(int smokeSignal : smokeSignals) {
            int msg = comms.decrypt(smokeSignal);
            if(comms.validate(msg)) {
                int msgType = comms.getType(msg);
                if(msgType == comms.MSG_TYPE_ALLIED_SETTLEMENT)
                    addSettlementChecked(comms.intToLocation(comms.getInfo(msg)));
            }
        }
    }

    void updateInfo() {
        // carried resources & max resource capacity
        getResourcesCarried = uc.getResourcesCarried();
        int carriedRes = getResourcesCarried[0] + getResourcesCarried[1] + getResourcesCarried[2];
        maxResourceCapacity = uc.hasResearched(Technology.BOXES, uc.getTeam()) ? GameConstants.MAX_RESOURCE_CAPACITY_BOXES : GameConstants.MAX_RESOURCE_CAPACITY;
        fullOfResources = getResourcesCarried[0] >= maxResourceCapacity
                        || getResourcesCarried[1] >= maxResourceCapacity
                        || getResourcesCarried[2] >= maxResourceCapacity;

        // units
        units = uc.senseUnits();
        closestDeer = null;
        deerMinDist = 1000000;
    unitLoop:
        for (UnitInfo unit : units) {
            Location loc = unit.getLocation();
            int dist = loc.distanceSquared(uc.getLocation());
            UnitType type = unit.getType();

            if (type == UnitType.DEER && dist < deerMinDist) {
                closestDeer = loc;
                deerMinDist = dist;
            }
            else if(type == UnitType.BASE && unit.getTeam() == uc.getTeam())
                baseLocation = loc;
            else if(type == UnitType.SETTLEMENT)
                addSettlementChecked(loc);
        }

        // local resources
        localResourceInfos = uc.senseResourceInfo(uc.getLocation());
        localFood = 0;
        localResourceTotal = 0;
        for(ResourceInfo resourceInfo : localResourceInfos) {
            if(resourceInfo != null) {
                localResourceTotal += resourceInfo.amount;
                if(resourceInfo.resourceType == Resource.FOOD)
                    localFood += resourceInfo.amount;
            }
        }

        // resourceInfosOccupied
        resourceInfos = uc.senseResources();
        resourceInfosOccupied = new boolean[resourceInfos.length];
        for(int i = 0; i < resourceInfos.length; i++)
            resourceInfosOccupied[i] = uc.senseUnitAtLocation(resourceInfos[i].location) != null;

        // resourceInfos
        totalRes = 0;
        anyFood = false;
        for (int i = 0; i < resourceInfos.length; i++) {
            if(!resourceInfosOccupied[i]) {
                totalRes += resourceInfos[i].amount;
                anyFood |= (resourceInfos[i].amount > 100 && resourceInfos[i].resourceType == Resource.FOOD);
            }
        }
    }

    // prioritizes food
    Location findClosestResource(ResourceInfo[] resourceInfos, boolean[] resourceInfosOccupied) {
        Location closest = null;
        Location closestFood = null;
        int closestDist = 1000000;
        int closestFoodDist = 10000000;

        for(int i = 0; i < resourceInfos.length; i++) {
            if(!resourceInfosOccupied[i]) {
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
            moveRandom();
        }
        else {
            pathfinding.pathfindTo(exploreLoc);
            uc.drawLineDebug(uc.getLocation(), exploreLoc, 0, 0, 255);
        }

    }

    void spawnNewSettlement() {
        uc.println("spawning new settlement");
        Location newSettlementLoc = trySpawnInValidAndReturnLocation(UnitType.SETTLEMENT);
        if(newSettlementLoc != null) {
            uc.drawLineDebug(uc.getLocation(), newSettlementLoc, 0, 255, 255);
            addSettlementUnchecked(newSettlementLoc);
        }
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

    boolean isValid(Location loc) {
        uc.println("is valid?: [" + loc.x + ", " + loc.y + "]");
        if (((loc.x + loc.y) % 2) == 0 && !uc.isOutOfMap(loc) && uc.canSenseLocation(loc)) {
            //uc.println("is preemptively valid");

            ResourceInfo[] res = uc.senseResourceInfo(loc);
            //uc.println("res.length: " + res.length);

            for(ResourceInfo resInfo : res) {
                //uc.println("is valid? resInfo == null: " + resInfo == null);
                if(resInfo != null && resInfo.amount > 0)
                    return false;
            }
            return true;
        }
        return false;
        //return baseLocation != null && loc.distanceSquared (baseLocation) > 1 || (uc.getRound() > 400 && lastValid + 3< uc.getRound());
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
            if (isValid(uc.getLocation().add(dir)) && trySpawnUnit(type, dir)) {
                lastValid = uc.getRound();
                return true;
            }
        }
        return false;
    }
    Location trySpawnInValidAndReturnLocation(UnitType type) {
        for (Direction dir : dirs) {
            if (isValid(uc.getLocation().add(dir)) && trySpawnUnit(type, dir)) {
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

    // also initializes it
    void updateExploration(){
        if(exploration == null) { // init. exploration & baseLocation
            if(baseLocation != null) {
                exploration = new Exploration(uc, baseLocation, 3, 75);
                addSettlementUnchecked(baseLocation);
            }
        }
        else
            exploration.updateChunks();
    }

    void buildEconBuildings() {
        if(trySpawnInValid(UnitType.SAWMILL))
            sawmillCount++;
        if(trySpawnInValid(UnitType.QUARRY))
            quarryCount++;
        if(trySpawnInValid(UnitType.FARM))
            farmCount++;
    }
}