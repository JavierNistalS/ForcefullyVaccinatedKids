package chineseFluKid;

import aic2021.user.*;


public class TheKGB { // I only know that I know nothing, and so do you.
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
                return trySmokeSignal(431036316);
            if(dir == Direction.EAST || dir == Direction.SOUTHEAST) // +-
                return trySmokeSignal(431381955);
            if(dir == Direction.SOUTH || dir == Direction.SOUTHWEST) // --
                return trySmokeSignal(139518736);
            //if(dir == Direction.WEST || dir == Direction.NORTHWEST) // -+
                return trySmokeSignal(433894293);
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
