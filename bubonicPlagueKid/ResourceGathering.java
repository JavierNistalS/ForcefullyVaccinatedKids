package bubonicPlagueKid;

import aic2021.user.*;

public class ResourceGathering {

    final int MAX_TURNS_CHASING_MULT = 10;
    final int BLACK_LIST_DURATION = 250;

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
    double targetResourceValue = 0;
    int[][] blackList = new int[99][99];
    double[] values;
    int turnsChasing = 0;
    int maxTurnsChasing = 0;
    boolean fuckingWater = false;
    boolean needRafts = false;
    double valueSum = 0;

    public void update(){
        if (uc.canGatherResources())
            uc.gatherResources();

        for (Resource r : Resource.values())
            values[r.ordinal()] = 10000.0 / (uc.getResource(r) + 1); // RESOURCE CURVE

        if (uc.canSenseLocation(targetResource)) {
            ResourceInfo[] resourcesAtTarget = uc.senseResourceInfo(targetResource);
            targetResourceValue = 0;
            for (ResourceInfo ri : resourcesAtTarget){
                if (ri != null){
                    double value = effectiveValue(ri);
                    targetResourceValue = Math.max(targetResourceValue, value);
                }
            }
            if ((uc.senseUnitAtLocation(targetResource) != null && uc.getLocation().distanceSquared(targetResource) > 0) ||
                    (unit.enemyBaseLocation != null && unit.enemyBaseLocation.distanceSquared(targetResource) <= 18 &&
                            (!uc.canSenseLocation(unit.enemyBaseLocation) || !uc.isObstructed(unit.enemyBaseLocation, targetResource)))){
                targetResourceValue = 0;
            }
            if (targetResourceValue == 0) {
                targetResource = null;
                turnsChasing = 0;
                fuckingWater = false;
                resetTurnCount();
            }
        }

        ResourceInfo[] resources = uc.senseResources();
        
        uc.println("c1");
        valueSum = 0;

        for (ResourceInfo ri : resources) {
            Location loc = ri.getLocation();
            if (blackList[loc.x - spawnLocation.x + 49][loc.y - spawnLocation.y + 49] < uc.getRound() && uc.senseUnitAtLocation(loc) == null &&
                (unit.enemyBaseLocation == null || unit.enemyBaseLocation.distanceSquared(loc) > 18 ||
                    (uc.canSenseLocation(unit.enemyBaseLocation) && uc.isObstructed(unit.enemyBaseLocation, loc)))) {

                double value = effectiveValue(ri);
                valueSum += value;
                if (value > targetResourceValue) {
                    if(targetResource != null)
                        blackList[targetResource.x - spawnLocation.x + 49][targetResource.y - spawnLocation.y + 49] = uc.getRound() + 10;
                    targetResourceValue = value;
                    targetResource = loc;
                    turnsChasing = 0;
                    resetTurnCount();
                    fuckingWater = false;
                }
            }
        }
    }

    public void countTurn(){
        turnsChasing++;
        if (uc.hasWater(uc.getLocation().add(uc.getLocation().directionTo(targetResource)))){
            fuckingWater = true;
        }
        if (turnsChasing > maxTurnsChasing){
            blackList[targetResource.x][targetResource.y] = uc.getRound() + BLACK_LIST_DURATION;
            targetResource = null;
            targetResourceValue = 0;
            turnsChasing = 0;
            if (fuckingWater){
                needRafts = true;
                fuckingWater = false;
            }
        }
    }

    public void resetTurnCount(){
        turnsChasing = 0;
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
        return Math.max(amount, 20)*values[resource.ordinal()]/(Math.sqrt(sqrDist) + 1);
    }
}
