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
        uc.println("a");

        if (uc.canGatherResources())
            uc.gatherResources();
        for (Resource r : Resource.values())
            values[r.ordinal()] = 10000.0 / (1 + uc.getResource(r));

        uc.println("b");
        if (uc.canSenseLocation(targetResource)) {
            ResourceInfo[] resourcesAtTarget = uc.senseResourceInfo(targetResource);
            targetResourceValue = 0;
            for (ResourceInfo ri : resourcesAtTarget){
                if (ri != null){
                    double value = value(ri);
                    targetResourceValue = Math.max(targetResourceValue, value);
                }
            }
            if (uc.senseUnitAtLocation(targetResource) != null ||
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
        uc.println("c");

        ResourceInfo[] resources = uc.senseResources();
        uc.println("c1");
        valueSum = 0;
        for (ResourceInfo ri : resources) {
            uc.println("c_loop");

            Location loc = ri.getLocation();
            if (blackList[loc.x - spawnLocation.x + 49][loc.y - spawnLocation.y + 49] < uc.getRound() && uc.senseUnitAtLocation(loc) == null &&
                (unit.enemyBaseLocation == null || unit.enemyBaseLocation.distanceSquared(loc) > 18 ||
                    (uc.canSenseLocation(unit.enemyBaseLocation) && uc.isObstructed(unit.enemyBaseLocation, loc)))) {
                uc.println("c_loop2");

                double value = value(ri);
                valueSum += value;
                if (value > targetResourceValue) {
                    uc.println("c_loop3");
                    if(targetResource != null)
                        blackList[targetResource.x - spawnLocation.x + 49][targetResource.y - spawnLocation.y + 49] = uc.getRound() + 10;
                    targetResourceValue = value;
                    targetResource = loc;
                    turnsChasing = 0;
                    resetTurnCount();
                    fuckingWater = false;
                }
            }
            uc.println("c_loop4");
        }
        uc.println("d");

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

    private double value(ResourceInfo ri){
        return Math.max(ri.amount, 20)*values[ri.resourceType.ordinal()]/(Math.sqrt(uc.getLocation().distanceSquared(ri.getLocation()))+1);
    }
}
