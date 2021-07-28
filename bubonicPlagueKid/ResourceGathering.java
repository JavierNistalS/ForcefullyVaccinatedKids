package bubonicPlagueKid;

import aic2021.user.*;

public class ResourceGathering {
    UnitController uc;
    MyUnit unit;
    final int MAX_TURNS_CHASING_MULT = 10;
    final int BLACK_LIST_DURATION = 250;
    public ResourceGathering(UnitController uc, MyUnit unit){
        this.uc = uc;
        this.unit = unit;
    }
    Location targetResource = null;
    double targetResourceValue = 0;
    int[][] blackList = new int[99][99];
    double[] values;
    int turnsChasing = 0;
    int maxTurnsChasing = 0;

    public void update(){
        if (uc.canGatherResources())
            uc.gatherResources();
        for (Resource r : Resource.values()){
            values[r.ordinal()] = 10000d / (1 + uc.getResource(r));
        }
        if (uc.canSenseLocation(targetResource)){
            ResourceInfo[] resourcesAtTarget = uc.senseResourceInfo(targetResource);
            targetResourceValue = 0;
            for (ResourceInfo ri : resourcesAtTarget){
                if (ri != null){
                    double value = value(ri);
                    targetResourceValue = Math.max(targetResourceValue, value);
                }
            }
            if (uc.senseUnitAtLocation(targetResource) != null ||
                    (unit.enemyBaseLocation != null &&
                            unit.enemyBaseLocation.distanceSquared(targetResource) <= 18 &&
                                    (!uc.canSenseLocation(unit.enemyBaseLocation) ||
                                            !uc.isObstructed(unit.enemyBaseLocation, targetResource)))){
                targetResourceValue = 0;
            }
            if (targetResourceValue == 0) {
                targetResource = null;
                turnsChasing = 0;
            }
        }
        ResourceInfo[] resources = uc.senseResources();
        for (ResourceInfo ri : resources){
            Location loc = ri.getLocation();
            if (blackList[loc.x][loc.y] < uc.getRound() &&
                    uc.senseUnitAtLocation(loc) == null &&
                    (unit.enemyBaseLocation == null ||
                            unit.enemyBaseLocation.distanceSquared(loc) > 18 ||
                            (uc.canSenseLocation(unit.enemyBaseLocation) &&
                                    uc.isObstructed(unit.enemyBaseLocation, loc)))){
                double value = value(ri);
                if (value > targetResourceValue){
                    blackList[targetResource.x][targetResource.y] = uc.getRound() + 10;
                    targetResourceValue = value;
                    targetResource = loc;
                    turnsChasing = 0;
                    maxTurnsChasing = (int)(Math.sqrt(uc.getLocation().distanceSquared(targetResource))*MAX_TURNS_CHASING_MULT+3);
                }
            }
        }
    }

    public void countTurn(){
        turnsChasing++;
        if (turnsChasing > maxTurnsChasing){
            blackList[targetResource.x][targetResource.y] = uc.getRound() + BLACK_LIST_DURATION;
            targetResource = null;
            targetResourceValue = 0;
            turnsChasing = 0;
        }
    }

    public Location getLocation(){
        return targetResource;
    }

    private double value(ResourceInfo ri){
        return Math.max(ri.amount, 20)*values[ri.resourceType.ordinal()]/(Math.sqrt(uc.getLocation().distanceSquared(ri.getLocation()))+1);
    }
}
