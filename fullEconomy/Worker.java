package fullEconomy;

import aic2021.user.*;

public class Worker extends MyUnit {

    Worker(UnitController uc){
        super(uc);
        pathfinding = new Pathfinding(uc);
    }

    Pathfinding pathfinding;
    Exploration exploration;

    Location baseLocation;
    Location[] settlements = new Location[256];
    int settlementsLength = 0, settlementTargetIdx = -1;
    int farmCount, quarryCount, sawmillCount;

    boolean orbitDirection = false;
    int lastValid = 0;

    int SETTLEMENT_DISTANCE = 10;

    void playRound() {
        sustainTorch();

        int MAX_RESOURCE_CAPACITY = uc.hasResearched(Technology.BOXES, uc.getTeam()) ? GameConstants.MAX_RESOURCE_CAPACITY_BOXES : GameConstants.MAX_RESOURCE_CAPACITY;
        int[] getResourcesCarried = uc.getResourcesCarried();
        int carriedRes = getResourcesCarried[0] + getResourcesCarried[1] + getResourcesCarried[2];

        UnitInfo[] units = uc.senseUnits();
        ResourceInfo[] localResourceInfos = uc.senseResourceInfo(uc.getLocation());
        int localFood = 0;
        int localResourceTotal = 0;

        Location closestDeer = null;
        int deerMinDist = 1000000;

        // units
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
            else if(type == UnitType.SETTLEMENT) {
                addSettlementChecked(loc);
            }
        }

        // local resources
        for(ResourceInfo resourceInfo : localResourceInfos) {
            if(resourceInfo != null) {
                localResourceTotal += resourceInfo.amount;
                if(resourceInfo.resourceType == Resource.FOOD)
                    localFood += resourceInfo.amount;
            }
        }

        // resourceInfosOccupied
        ResourceInfo[] resourceInfos = uc.senseResources();
        boolean[] resourceInfosOccupied = new boolean[resourceInfos.length];
        for(int i = 0; i < resourceInfos.length; i++)
            resourceInfosOccupied[i] = uc.senseUnitAtLocation(resourceInfos[i].location) != null;

        // resourceInfos
        int totalRes = 0;
        boolean anyFood = false;
        for (int i = 0; i < resourceInfos.length; i++) {
            if(!resourceInfosOccupied[i]) {
                totalRes += resourceInfos[i].amount;
                anyFood |= (resourceInfos[i].amount > 100 && resourceInfos[i].resourceType == Resource.FOOD);
            }
        }


        updateExploration();

        if(!anyFood)
            huntDeer(closestDeer);

        //uc.println("e");
        if(localResourceTotal > 0 && carriedRes < MAX_RESOURCE_CAPACITY) {
            uc.println("gathering resources");
            uc.gatherResources();
            settlementTargetIdx = -1;
            uc.drawPointDebug(uc.getLocation(), 255, 255, 0);
        }
        else if(uc.canMove()) {
            if(settlementTargetIdx != -1 || carriedRes >= MAX_RESOURCE_CAPACITY || (totalRes == 0 && carriedRes > MAX_RESOURCE_CAPACITY * 0.75)) {

                updateSettlementTarget();

                if(uc.getLocation().distanceSquared(settlements[settlementTargetIdx]) > SETTLEMENT_DISTANCE && (totalRes + carriedRes) > 150) {
                    uc.println("spawning new settlement");
                    Location newSettlementLoc = trySpawnInValidAndReturnLocation(UnitType.SETTLEMENT);
                    if(newSettlementLoc != null) {
                        uc.drawLineDebug(uc.getLocation(), newSettlementLoc, 0, 255, 255);
                        addSettlementUnchecked(newSettlementLoc);
                        settlementTargetIdx = settlementsLength - 1; // spawn it & make it the target settlement
                    }
                }
                else
                    pathfinding.pathfindTo(settlements[settlementTargetIdx]);
            }
            else if(totalRes == 0) { // no resources
                uc.println("no resources nor deer spotted");
                explore();
            }
            else { // some resources
                uc.println("going to resources");

                Location closestRes = findClosestResource(resourceInfos, resourceInfosOccupied);
                uc.drawLineDebug(uc.getLocation(), closestRes, 255, 255, 0);
            }
        }

        if(tryDeposit())
            settlementTargetIdx = -1;
        buildEconBuildings();
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
        return ((loc.x + loc.y) % 2) == 0;
        //return baseLocation != null && loc.distanceSquared (baseLocation) > 1 || (uc.getRound() > 400 && lastValid + 3< uc.getRound());
    }

    void addSettlementUnchecked(Location settlementLoc) {
        uc.println("Added settlements[" + settlementsLength + "]: [" + settlementLoc.x + ", " + settlementLoc.y + "]");
        settlements[settlementsLength] = settlementLoc; // add base as a 'settlement'
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
                exploration = new Exploration(uc, baseLocation, 3);
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
