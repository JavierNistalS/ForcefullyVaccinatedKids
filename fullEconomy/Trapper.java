package fullEconomy;

import aic2021.user.*;



public class Trapper extends MyUnit {

    Trapper(UnitController uc){
        super(uc);
        exploration = new Exploration(uc, 3, 75);
        pathfinding = new Pathfinding(uc, this);
    }

    Exploration exploration;
    Pathfinding pathfinding;

    void playRound() {
        identifyBase();
        setTraps();
        exploration.updateChunks();

        Location target = exploration.getLocation();
        if (target == null){
            exploration = new Exploration(uc, 3, 75);
            target = exploration.getLocation();
        }
        pathfinding.pathfindTo(target);
    }

    void setTraps() {
        if (uc.canAttack()) {
            for (Direction dir : dirs) {
                Location loc = uc.getLocation().add(dir);
                if (uc.canAttack(loc) && loc.x % 3 == 0 && loc.y % 3 == 0){
                    uc.attack(loc);
                }
            }
        }
    }
}
