package chineseFluKid;

import aic2021.user.*;

public class Pathfinding {
    UnitController uc;
    Location location;
    public Pathfinding(UnitController uc, MyUnit unit){
        this.uc = uc;
        rotateRPath = uc.getRandomDouble() > 0.5;
        this.unit = unit;
    }
    Location lastObj = new Location(-1, -1);
    boolean obstacle = false;
    Location lastObs;
    int minDistToObj = 1000000;
    boolean rotateRPath;
    MyUnit unit;

    public boolean pathfindTo(Location obj){
        if (!uc.canMove())
            return false;
        location = uc.getLocation();
        //uc.println("position: " + position);
        //uc.println("obj: " + obj);
        //uc.println("lastobj: " + lastObj);
        //uc.println("min dist to obj: "+minDistToObj);
        if (!equals(lastObj,obj)){
            resetPathfinding(obj);
        }
        if (!obstacle){
            //uc.println("no obstacle?");
            Direction dir = location.directionTo(obj);
            if (move3(dir))
                return true;
            obstacle = true;
            //uc.println("new obstacle");
            lastObs = location.add(dir);
        }
        if (obstacle){
            if (uc.isOutOfMap(lastObs)) {
                resetPathfinding(obj);
                rotateRPath = !rotateRPath;
            }
            //uc.println("obstacle: " + lastObs);
            Direction dir = location.directionTo(lastObs);
            //uc.println("Dir = " + dir);
            if (tryMove(dir)){
                obstacle = false;
                //uc.println("obstacle disappeared");
                return true;
            }
            for (int i = 1; i < 8; i++){
                dir = rotateRPath ? dir.rotateRight() : dir.rotateLeft();
                //uc.println("Dir = " + dir);
                if (tryMove(dir)){
                    //uc.println("moved " + dir);
                    lastObs = location.add(rotateRPath ? dir.rotateLeft() : dir.rotateRight());
                    int distToObj = location.distanceSquared(obj);
                    //uc.println("dist to obj: " + distToObj);
                    if (minDistToObj > distToObj){
                        minDistToObj = distToObj;
                        obstacle = false;
                    }
                    //uc.println("surrounding");
                    return true;
                }
                //uc.println("cant move " + dir);
            }
            return false;
        }
        //uc.println("wtf pathfinding");
        return false;
    }
    public void resetPathfinding(Location newobj){
        //uc.println("reset");
        lastObj = newobj;
        obstacle = false;
        minDistToObj = 1000000;
    }

    public boolean tryMoveSafe(Direction dir, Location danger, int radius){
        return location.add(dir).distanceSquared(danger) > radius && tryMove(dir);
    }

    public boolean move3Safe(Direction dir, Location danger, int radius){
        if (tryMoveSafe(dir, danger, radius))
            return true;
        if (tryMoveSafe(dir.rotateRight(), danger, radius))
            return true;
        if (tryMoveSafe(dir.rotateLeft(), danger, radius))
            return true;
        return false;
    }

    public boolean wanderAround(Location obj, int radius){
        if (!uc.canMove())
            return false;
        location = uc.getLocation();
        //uc.println("position: " + position);
        //uc.println("obj: " + obj);
        //uc.println("lastobj: " + lastObj);
        //uc.println("min dist to obj: "+minDistToObj);
        if (!equals(lastObj,obj)){
            resetPathfinding(obj);
        }
        if (!obstacle){
            //uc.println("no obstacle?");
            Direction dir = location.directionTo(obj);
            if (move3Safe(dir, obj, radius))
                return true;
            obstacle = true;
            //uc.println("new obstacle");
            lastObs = location.add(dir);
        }
        if (obstacle){
            if (uc.isOutOfMap(lastObs)) {
                resetPathfinding(obj);
                rotateRPath = !rotateRPath;
            }
            //uc.println("obstacle: " + lastObs);
            Direction dir = location.directionTo(lastObs);
            //uc.println("Dir = " + dir);
            if (tryMoveSafe(dir, obj, radius)){
                obstacle = false;
                //uc.println("obstacle disappeared");
                return true;
            }
            for (int i = 1; i < 8; i++){
                dir = rotateRPath ? dir.rotateRight() : dir.rotateLeft();
                //uc.println("Dir = " + dir);
                if (tryMoveSafe(dir, obj, radius)){
                    //uc.println("moved " + dir);
                    lastObs = location.add(rotateRPath ? dir.rotateLeft() : dir.rotateRight());
                    int distToObj = location.distanceSquared(obj);
                    //uc.println("dist to obj: " + distToObj);
                    if (minDistToObj > distToObj){
                        minDistToObj = distToObj;
                        obstacle = false;
                    }
                    //uc.println("surrounding");
                    return true;
                }
                //uc.println("cant move " + dir);
            }
            return false;
        }
        //uc.println("wtf pathfinding");
        return false;
    }

    public boolean tryMove(Direction dir){
        if (canMove(dir)) {
            uc.move(dir);
            return true;
        }
        return false;
    }
    public boolean move3(Direction dir){
        if (tryMove(dir))
            return true;
        if (tryMove(dir.rotateRight()))
            return true;
        if (tryMove(dir.rotateLeft()))
            return true;
        return false;
    }
    public boolean equals(Location a, Location b){
        return a.x == b.x && a.y == b.y;
    }

    public boolean canMove(Direction dir){
        if (uc.canMove(dir)){
            Location loc = uc.getLocation().add(dir);
            if (unit.enemyBaseLocation != null && unit.enemyBaseLocation.distanceSquared(loc) <= 18){
                return false;
            }
            if (uc.canSenseLocation(loc)){
                return !uc.hasTrap(loc);
            }
            else{
                return loc.x % 3 != 0 || loc.y % 3 != 0;
            }
        }
        return false;
    }
}
