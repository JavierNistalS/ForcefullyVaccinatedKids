package bubonicPlagueKid;

import aic2021.user.*;

// remember to call updateEnemyUnits() !!!!!
public class EvasivePathfinding extends Pathfinding {

    public EvasivePathfinding(UnitController uc, MyUnit unit) {
        super(uc, unit);
    }

    UnitInfo[] enemyUnits;

    public void updateEnemyUnits() {
        enemyUnits = uc.senseUnits(uc.getOpponent());
    }

    public boolean canMove(Direction dir) {
        if(super.canMove(dir)) {
            Location loc = uc.getLocation().add(dir);

            for(UnitInfo enemyUnit : enemyUnits) {
                int dangerRange = 0;
                int minDangerRange = 0;

                if(enemyUnit.getType() == UnitType.AXEMAN || enemyUnit.getType() == UnitType.WORKER)
                    dangerRange = 13;
                else if(enemyUnit.getType() == UnitType.SPEARMAN) {
                    dangerRange = 32;
                    minDangerRange = 5;
                }

                int dist = enemyUnit.getLocation().distanceSquared(loc);
                if(dist <= dangerRange && dist > minDangerRange && dist <= uc.getLocation().distanceSquared(enemyUnit.getLocation()))
                    return false;
            }
            return true;
        }
        return false;
    }
}
