package gonorheaKid;

import aic2021.user.*;

public class Pathfinding {
    UnitController uc;
    Location location;
    public Pathfinding(UnitController uc){
        this.uc = uc;
    }
    boolean rotateR = true;
    Location lastObj = new Location(-1, -1);
    boolean obstacle = false;
    Location lastObs;
    int minDistToObj = 1000000;
    boolean rotateRPath = true;

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

    public void WanderAround(Location center, int radius){
        if (!uc.canMove())
            return;
        location = uc.getLocation();
        //uc.println('a');
        if (center.distanceSquared(location) > 2*radius)
            pathfindTo(center);
        //uc.println('b');
        if (center.distanceSquared(location) < radius)
            pathfindTo(location.add(location.x - center.x, location.y - center.y));
        //uc.println('c');
        if (!uc.canMove())
            return;
        Direction dir = location.directionTo(center);
        dir = rotateR ? dir.rotateRight().rotateRight() : dir.rotateLeft().rotateLeft();
        int k = 0;
        while (!uc.canMove(dir) && k++ < 6){
            dir = rotateR ? dir.rotateRight() : dir.rotateLeft();
        }
        if (k < 6) {
            if (k > 2)
                rotateR = !rotateR;
            uc.move(dir);
            resetPathfinding(new Location(-1,-1));
        }
    }

    public boolean tryMove(Direction dir){
        if (uc.canMove(dir)) {
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
}
