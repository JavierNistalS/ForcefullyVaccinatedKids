package chineseFluKid;

import aic2021.user.*;


public class TheKGB {
    // "All war is deception"
    //   ― Sun tzu, The Art of War

    UnitController uc;
    Communications comms;

    public TheKGB(UnitController uc){
        this.uc = uc;
        comms = new Communications(uc);

    }

    public boolean disruptRosa(Location loc){
        //int message = 1050*loc.x + loc.y + 180500000;
        //return trySmokeSignal(message);

        if(loc == null) {
            return trySmokeSignal(139518736); // -- ~= [-30,-30]
        }
        else {
            Direction dir = uc.getLocation().directionTo(loc);

            if(dir == Direction.NORTH || dir == Direction.NORTHEAST) // ++
                return trySmokeSignal(139787668);
            if(dir == Direction.EAST || dir == Direction.SOUTHEAST) // +-
                return trySmokeSignal(139518736);
            if(dir == Direction.SOUTH || dir == Direction.SOUTHWEST) // --
                return trySmokeSignal(139518736);
            //if(dir == Direction.WEST || dir == Direction.NORTHWEST) // -+
                return trySmokeSignal(139518736);
        }
    }

    public boolean disruptViper(Location loc) {
        Location loc1 = new Location(974, 456);
        Location loc2 = new Location(642, 762);
        Location loc3 = new Location(656, 106);

        int dist1 = loc1.distanceSquared(uc.getLocation());
        int dist2 = loc2.distanceSquared(uc.getLocation());
        int dist3 = loc3.distanceSquared(uc.getLocation());

        if(dist1 > dist2 && dist1 > dist3)
            return trySmokeSignal(7543);
        else if(dist2 > dist3)
            return trySmokeSignal(11243);
        else
            return trySmokeSignal(11223);
    }

    public void disruptRelativeCoords(Location enemyBaseLocation, P<Direction, Integer>[] relativeDataFromEnemyPerspective) {
        if(enemyBaseLocation == null) {
            if(relativeDataFromEnemyPerspective.length > 0)
                trySmokeSignal(relativeDataFromEnemyPerspective[0].y); // assumes at least 1 element
        }
        else {
            int bestDist = 100000000;
            int best = 0;

            Direction dir = uc.getLocation().directionTo(enemyBaseLocation);

            for(P<Direction, Integer> p : relativeDataFromEnemyPerspective) {
                int dist = ((p.x.dx - dir.dx) * (p.x.dx - dir.dx)) + ((p.x.dy - dir.dy) * (p.x.dy - dir.dy));
                if(dist < bestDist) {
                    bestDist = dist;
                    best = p.y;
                }
            }

            if(bestDist != 100000000)
                trySmokeSignal(best);
        }

    }
    public void disruptAbsoluteCoords(Location enemyBaseLocation, P<Location, Integer>[] absoluteData) {
        Location targetLoc = getTargetLoc(enemyBaseLocation);

        if(targetLoc == null) {
            int best = 0;
            int bestDist = 0;
            for(P<Location, Integer> p : absoluteData) {
                int dist = uc.getLocation().distanceSquared(p.x);
                if (dist > bestDist) {
                    bestDist = dist;
                    best = p.y;
                }
            }

            if(bestDist != 0)
                trySmokeSignal(best);
        }
        else {
            int best = 0;
            int bestDist = 100000000;

            for(P<Location, Integer> p : absoluteData) {
                int dist = targetLoc.distanceSquared(p.x);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = p.y;
                }
            }

            if(bestDist != 100000000)
                trySmokeSignal(best);
        }
    }

    public Location getTargetLoc(Location enemyBaseLocation) {

        int dx = 0, dy = 0;

        if(enemyBaseLocation != null) {
            Direction dir = uc.getLocation().directionTo(enemyBaseLocation);
            dx = dir.dx;
            dy = dir.dy;
        }
        else {
            Location loc = uc.getLocation();

            if(spotEndOfMapInDir(0, 1))
                dy++;
            if(spotEndOfMapInDir(0, -1))
                dy--;
            if(spotEndOfMapInDir(1, 1))
                dx++;
            if(spotEndOfMapInDir(-1, 1))
                dx--;
        }

        if(dx == 0 && dy == 0)
            return null;
        else
            return uc.getLocation().add(dx * 100, dy * 100);
    }

    public boolean spotEndOfMapInDir(int dx, int dy){
        Location loc;
        UnitType type = uc.getType();

        if(type == UnitType.BASE)
            loc = uc.getLocation().add(dx * 6, dy * 6);
        else if(type == UnitType.SETTLEMENT || type == UnitType.BARRACKS)
            loc = uc.getLocation().add(dx * 5, dy * 5);
        else
            loc = uc.getLocation().add(dx * 4, dy * 4);

        return uc.isOutOfMap(loc);
    }

    public boolean disruptCarbassots(Location loc){
        int message = 2*loc.x + 8192*loc.y - 1073741823;
        return trySmokeSignal(message);
    }

    public boolean disruptWololo(Location loc, boolean explorer){
        int message = 32768*loc.x + 16*loc.y - 1006632960;
        if (explorer)
            message += 4;
        return trySmokeSignal(message);
    }

    public boolean disruptEveryone(Location enemyBaseLocation) {
        Location loc;
        if (enemyBaseLocation != null)
            loc = new Location(2*enemyBaseLocation.x - uc.getLocation().x, 2*enemyBaseLocation.y - uc.getLocation().y);
        else
            loc = new Location((int)(1050*uc.getRandomDouble()), (int)(1050*uc.getRandomDouble()));

        uc.drawLineDebug(uc.getLocation(), loc, 120,0,0);
        double random = uc.getRandomDouble();

        if (random < 0.5)
            return disruptRosa(loc);
        else
            return disruptViper(loc);
    }

    public boolean trySmokeSignal(int x){
        if (comms.validate(comms.decrypt(x)))
            return false;
        if (uc.canMakeSmokeSignal()){
            uc.makeSmokeSignal(x);
            return true;
        }
        return false;
    }
}
