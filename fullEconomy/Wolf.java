package fullEconomy;

import aic2021.user.*;


public class Wolf extends MyUnit {

    Wolf(UnitController uc){
        super(uc);
        pathfinding = new Pathfinding(uc, this);
        exploration = new Exploration(uc, 4, 75);
    }

    Exploration exploration;
    Pathfinding pathfinding;

    void playRound() {
        identifyBase();
        generalAttack();
        exploration.updateChunks();

        if (uc.canMove() && !micro()) {
            Location toExplore = exploration.getLocation();
            if (toExplore == null){
                exploration = new Exploration(uc, 4, 75);
                toExplore = exploration.getLocation();
            }
            pathfinding.pathfindTo(toExplore);
        }
        generalAttack();
    }

    boolean micro(){
        if (!uc.canMove())
            return false;
        UnitInfo[] enemies = uc.senseUnits(uc.getTeam().getOpponent());
        if (enemies.length == 0)
            return false;
        int[] score = {0, 0, 0, 0, 0, 0, 0, 0, 0};
        for (Direction dir : dirs){
            if (pathfinding.canMove(dir)){
                Location loc = uc.getLocation().add(dir);
                int light = uc.senseIllumination(loc);
                if (light >= 8){
                    score[dir.ordinal()] -= 100;
                    for (UnitInfo ui : enemies){
                        if (ui.getAttack() > 0 && ui.getLocation().distanceSquared(loc) <= ui.getType().attackRange + 10) {
                            score[dir.ordinal()] -= ui.getAttack() * 100;
                        }
                    }
                }
                score[dir.ordinal()] -= light;
                for (UnitInfo ui : enemies){
                    score[dir.ordinal()] -= (int)(10*Math.sqrt(loc.distanceSquared(ui.getLocation())));
                    if (ui.getType() == UnitType.BASE && loc.distanceSquared(ui.getLocation()) <= 40){
                        score[dir.ordinal()] -= 10000;
                        enemyBaseLocation = ui.getLocation();
                    }
                    else if (ui.getType() == UnitType.WOLF){
                        int dist = loc.distanceSquared(ui.getLocation());
                        if (dist <= 8)
                            score[dir.ordinal()] -= 50;
                        if (dist <= 2)
                            score[dir.ordinal()] -= 50;
                    }
                }
            }
        }
        int bestScore = -100000000;
        Direction best = Direction.ZERO;
        for (Direction dir : dirs){
            if (score[dir.ordinal()] > bestScore){
                bestScore = score[dir.ordinal()];
                best = dir;
            }
        }
        if (best != Direction.ZERO){
            return pathfinding.tryMove(best);
        }
        return true;
    }

}
