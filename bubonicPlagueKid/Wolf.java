package bubonicPlagueKid;

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

        Direction bestDir = Direction.ZERO;
        int bestScore = -10000000;

        for (Direction dir : dirs) {
            if (!pathfinding.canMove(dir))
                continue;

            int score = 0;
            Location loc = uc.getLocation().add(dir);
            int light = uc.senseIllumination(loc);

            if (light >= 8) { // visible
                score -= 50;
                for (UnitInfo enemy : enemies) {
                    if (enemy.getAttack() > 0 && enemy.getLocation().distanceSquared(loc) <= enemy.getType().attackRange + 10) {
                        score -= enemy.getAttack() * 10;
                    }
                }
            }
            score -= light / 2;
            int canHitCount = 0;
            boolean canHitSpearman = false;
            int minDist = 0;

            for (UnitInfo enemy : enemies) {

                int dist = loc.distanceSquared(enemy.getLocation());
                double linDist10 = (int)(10*Math.sqrt(dist));

                if(uc.getType() == UnitType.AXEMAN)
                    score += linDist10;
                else if(uc.getType() == UnitType.SPEARMAN)
                    score -= linDist10 * 10;
                else
                    score -= linDist10 * 6;

                if(dist <= 2) {
                    canHitCount++;
                    canHitSpearman |= uc.getType() == UnitType.SPEARMAN;
                }

                if (enemy.getType() == UnitType.BASE) {
                    enemyBaseLocation = enemy.getLocation();
                }
            }

            if(canHitSpearman)
                score += 10000;
            if(canHitCount > 0)
                score += 5000 - canHitCount;

            if(enemyBaseLocation != null && enemyBaseLocation.distanceSquared(loc) <= 18)
                score -= 100000;

            if(score > bestScore) {
                bestScore = score;
                bestDir = dir;
            }
        }

        if (bestDir != Direction.ZERO)
            return pathfinding.tryMove(bestDir);

        return true;
    }

}
