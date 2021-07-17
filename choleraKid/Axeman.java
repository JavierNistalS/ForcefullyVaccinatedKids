package choleraKid;

import aic2021.user.*;

public class Axeman extends MyUnit {

    Axeman(UnitController uc){
        super(uc);
        pathfinding = new Pathfinding(uc);
    }
    Pathfinding pathfinding;
    int attackRound = 800;

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
            boolean aggroPresent = false;
            for (UnitInfo ui : units){
                if (ui.getAttack() > 0 && ui.getType() != UnitType.BASE)
                    aggroPresent = true;
            }
            if(!aggroPresent) {
                if(enemyBaseLocation == null) {
                    if (enemyStructureLocation == null)
                        moveRandom();
                    else
                        pathfinding.wanderAround(enemyStructureLocation, 10);
                }
                else
                    pathfinding.wanderAround(enemyBaseLocation, 18);
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
                        if((unit.getType() == UnitType.SPEARMAN && dist > 5 && dist <= 18) || (unit.getType() == UnitType.AXEMAN && dist <= 5))
                            score = 100f / dist;
                        else if(dist <= 5) {
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

                    if(enemyBaseLocation != null && enemyBaseLocation.distanceSquared(loc) <= 18)
                        score -= 10e10;
                    else
                        score -= 0.01f * enemyBaseLocation.distanceSquared(loc);

                    int light = uc.senseIllumination(loc);
                    score -= light;
                    if(light < 10)
                        score += 700;

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
