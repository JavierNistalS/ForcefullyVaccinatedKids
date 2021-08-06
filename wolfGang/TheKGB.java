package wolfGang;

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

//    public boolean disruptViper(Location loc) {
//        int msg = 241*(1051*loc.x + loc.y);
//        return trySmokeSignal(msg);
//    }

    public boolean disruptRosa(int dx, int dy) {
        int inner = (100+dx) % 100 * 100 + (100+dy) % 100;

        int msg = 137969968 + 219 * inner;
        if(uc.getRandomDouble() > 0.5)
            msg = 430350021 + 453 * inner;
        return trySmokeSignal(msg);
    }

    int[] tastosisNumbers = {-1649760027, 1470683252, -1046765349};
    public boolean disruptTastosis(Location loc) {
        int num = (int)(uc.getRandomDouble() * tastosisNumbers.length);
        return trySmokeSignal(tastosisNumbers[num]);
    }

    public Location readTastosis(int msg, Location alliedBase) {
        Location loc = readTastosisConstant(msg, -1384120320, alliedBase);
        if(loc == null)
            loc = readTastosisConstant(msg, -847249408, alliedBase);

        return loc;
    }
    private Location readTastosisConstant(int msg, int constant, Location alliedBase) {
        msg -= constant;
        int x = msg / 2048;
        int y = msg % 2048;

        Location myLoc = uc.getLocation();
        if(Math.abs(x - myLoc.x) >= 50 || Math.abs(y - myLoc.y) >= 50)
            return null; // location too far away, must've been another msg

        if(alliedBase != null && (Math.abs(x - alliedBase.x) >= 50 || Math.abs(y - alliedBase.y) >= 50))
            return null;  // location too far away from base, must've been another msg

        return new Location(x, y);
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

        double random = uc.getRandomDouble();
        if (random < 0.66) {
            if (enemyBaseLocation == null)
                enemyBaseLocation = new Location((int)(1050*uc.getRandomDouble()), (int)(1050*uc.getRandomDouble()));

            uc.drawLineDebug(uc.getLocation(), enemyBaseLocation, 120,0,0);

            return disruptTastosis(enemyBaseLocation);
        }
        else {
            Direction dir = Direction.ZERO;
            if(enemyBaseLocation != null)
                dir = uc.getLocation().directionTo(enemyBaseLocation);

            return disruptRosa(dir.dx * 40, dir.dy * 40);
        }
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
