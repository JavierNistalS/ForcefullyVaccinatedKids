package finalBot;

import aic2021.user.*;

public class ResourceGathering {

    final int MAX_TURNS_CHASING_MULT = 4;
    final int BLACK_LIST_DURATION = 250;
    final double MIN_TARGET_VALUE = 18.5f;

    public ResourceGathering(UnitController uc, MyUnit unit){
        this.uc = uc;
        this.unit = unit;
        values = new double[3];
        spawnLocation = uc.getLocation();
    }

    UnitController uc;
    MyUnit unit;
    Location spawnLocation;
    Location targetResource = null;
    double targetResourceValue = MIN_TARGET_VALUE;
    int[][] blackList = new int[99][99];
    double[] values;
    int turnsChasing = 0;
    int maxTurnsChasing = 0;
    boolean fuckingWater = false;
    boolean needRafts = false;
    double valueForSettlementConstruction = 0;

    public void update(){
        if (targetResource != null){
            uc.drawPointDebug(targetResource, 0,255,255);
        }

        for (Resource r : Resource.values())
            values[r.ordinal()] = 10000.0 / (uc.getResource(r) + 100); // RESOURCE CURVE

        if (uc.canSenseLocation(targetResource)) {
            ResourceInfo[] resourcesAtTarget = uc.senseResourceInfo(targetResource);
            targetResourceValue = MIN_TARGET_VALUE;
            for (ResourceInfo ri : resourcesAtTarget){
                if (ri != null){
                    double value = effectiveValue(ri);
                    targetResourceValue = Math.max(targetResourceValue, value);
                }
            }
            if ((uc.senseUnitAtLocation(targetResource) != null && uc.getLocation().distanceSquared(targetResource) > 0) ||
                    (uc.canSenseLocation(targetResource) && uc.hasTrap(targetResource)) ||
                    (unit.enemyBaseLocation != null && unit.enemyBaseLocation.distanceSquared(targetResource) <= 18 &&
                            (!uc.canSenseLocation(unit.enemyBaseLocation) || !uc.isObstructed(unit.enemyBaseLocation, targetResource)))){
                targetResourceValue = MIN_TARGET_VALUE;
            }
            if (targetResourceValue <= MIN_TARGET_VALUE) {
                targetResource = null;
                turnsChasing = 0;
                fuckingWater = false;
                uc.println("not valuable enough");
                resetTurnCount();
            }
        }

        ResourceInfo[] resources = uc.senseResources();
        
        uc.println("c1");
        valueForSettlementConstruction = 0;

        for (ResourceInfo ri : resources) {
            Location loc = ri.getLocation();
            if (blackList[loc.x - spawnLocation.x + 49][loc.y - spawnLocation.y + 49] < uc.getRound() && uc.senseUnitAtLocation(loc) == null &&
                !uc.hasTrap(loc) &&
                    (unit.enemyBaseLocation == null || unit.enemyBaseLocation.distanceSquared(loc) > 18 ||
                    (uc.canSenseLocation(unit.enemyBaseLocation) && uc.isObstructed(unit.enemyBaseLocation, loc)))) {

                double value = effectiveValue(ri);
                valueForSettlementConstruction += effectiveValueForSettlement(ri);

                if (value > targetResourceValue) {
                    if (targetResource == null || targetResource.distanceSquared(loc) > 2) {
                        targetResourceValue = value;
                        targetResource = loc;
                        turnsChasing = 0;
                        uc.println("change objective");
                        resetTurnCount();
                        fuckingWater = false;
                    }
                    else{
                        targetResource = loc;
                        targetResourceValue = value;
                    }
                }
            }
        }
    }

    public void countTurn(){
        turnsChasing++;
        uc.println("increase turnCount: " + turnsChasing);
        if (uc.hasWater(uc.getLocation().add(uc.getLocation().directionTo(targetResource)))){
            fuckingWater = true;
            uc.println("goddamn water");
        }
        if (turnsChasing > maxTurnsChasing){
            uc.drawPointDebug(targetResource,0,0,0);
            blackList[targetResource.x - spawnLocation.x + 49][targetResource.y - spawnLocation.y + 49] = uc.getRound() + BLACK_LIST_DURATION;
            targetResource = null;
            targetResourceValue = MIN_TARGET_VALUE;
            turnsChasing = 0;
            if (fuckingWater){
                uc.println("rafts would be cool");
                needRafts = true;
                fuckingWater = false;
            }
        }
    }

    public void resetTurnCount(){
        turnsChasing = 0;
        uc.println("reset turn count");
        maxTurnsChasing = (int) (Math.sqrt(uc.getLocation().distanceSquared(targetResource)) * MAX_TURNS_CHASING_MULT + 3);
    }

    public Location getLocation(){
        return targetResource;
    }

    private double effectiveValue(ResourceInfo ri) {
        return effectiveValue(ri.amount, ri.resourceType, ri.location);
    }

    public double effectiveValue(int food, int wood, int stone, Location location) {
        return effectiveValue(food, Resource.FOOD, location) + effectiveValue(wood, Resource.WOOD, location) + effectiveValue(stone, Resource.STONE, location);
    }

    public double effectiveValue(int amount, Resource resource, Location location) {
        return effectiveValue(amount, resource, uc.getLocation().distanceSquared(location));
    }

    public double effectiveValue(int amount, Resource resource, int sqrDist) {
        return Math.max(amount, 20)*values[resource.ordinal()]/(Math.sqrt(sqrDist) + 1) * (resource == Resource.FOOD ? 1.5f : 1);
    }

    private double effectiveValueForSettlement(ResourceInfo ri) {
        return effectiveValue(ri.amount, ri.resourceType, uc.getLocation().distanceSquared(ri.location));
    }
    public double effectiveValueForSettlement(int amount, Resource resource, int sqrDist, int cap) {
        return Math.sqrt(20*amount)*values[resource.ordinal()]/(Math.sqrt(sqrDist) + 1);
    }
}
