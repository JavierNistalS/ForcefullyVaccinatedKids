package tuberculosisKid;

import aic2021.user.*;

public class Spearman extends MyUnit {

    Spearman(UnitController uc){
        super(uc);
        pathfinding = new Pathfinding(uc);
    }
    Pathfinding pathfinding;
    int attackRound = 860;

    void playRound(){
        ReadSmokeSignals();
        generalAttack();

        if(uc.getRound() < attackRound)
            idleMicro();
        else
            pathfinding.pathfindTo(enemyBaseLocation);

        generalAttack();
    }

    void idleMicro() {
        if(uc.canMove()) {
            if(enemyBaseLocation != null)
                uc.println("enemy base: " + enemyBaseLocation.toString());
            else
                uc.println("no c");
            UnitInfo[] units = uc.senseUnits(uc.getTeam().getOpponent());
            if(units.length == 0) {
                if(enemyBaseLocation == null)
                    moveRandom();
                else
                    pathfinding.pathfindTo(enemyBaseLocation);
            }
            else {
                float bestScore = -10e20f;
                Direction best = Direction.ZERO;

                for(Direction dir : Direction.values()) {
                    if(!uc.canMove(dir))
                        continue;

                    Location loc = uc.getLocation().add(dir);
                    float score = 0;
                    boolean canShootAnyAggro = false;
                    boolean canShootAny = false;

                    for(UnitInfo unit : units) {
                        Location unitLoc = unit.getLocation();
                        int dist = unitLoc.distanceSquared(loc);
                        if(unit.getType() == UnitType.AXEMAN && dist <= 13)
                            score = 10e8f / dist;
                        else if(dist <= 18) {
                            canShootAny = true;
                            if(unit.getType() == UnitType.AXEMAN || unit.getType() == UnitType.SPEARMAN) {
                                canShootAnyAggro = true;
                                score -= dist * 10;
                            }
                        }
                    }

                    if(canShootAnyAggro)
                        score += 10000;
                    else if(canShootAny)
                        score += 1000;
                    else if(dir == Direction.NORTHEAST || dir == Direction.NORTHWEST || dir == Direction.SOUTHEAST || dir == Direction.SOUTHWEST)
                        score -= 12000;
                    if(enemyBaseLocation != null && enemyBaseLocation.distanceSquared(loc) <= 18)
                        score -= 10e10;
                    else
                        score -= 0.01 * enemyBaseLocation.distanceSquared(loc);

                    int light = uc.senseIllumination(loc);
                    score -= light;
                    if(light < 10)
                        score += 500;

                    if(score > bestScore) {
                        bestScore = score;
                        best = dir;
                    }
                }

                uc.move(best);
            }
        }
    }

}