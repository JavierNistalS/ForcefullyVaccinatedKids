package wolfGang;

import aic2021.user.*;


public class Wolf extends MyUnit {

    public int PUSH_DISTANCE = 5;

    Wolf(UnitController uc) {
        super(uc);
        pathfinding = new Pathfinding(uc, this);
        exploration = new Exploration(uc, 4, 75);
    }

    Exploration exploration;
    Pathfinding pathfinding;

    UnitInfo[] alliedUnits, enemyUnits;
    boolean pushing = false;


    void playRound() {
        alliedUnits = uc.senseUnits(uc.getTeam());
        enemyUnits = uc.senseUnits(uc.getOpponent());

        float totalWolfPower = uc.getInfo().getHealth();
        for(UnitInfo units : alliedUnits) {
            if(units.getType() == UnitType.WOLF && units.getLocation().distanceSquared(uc.getLocation()) <= 10)
                totalWolfPower += units.getHealth() + 80;
        }
        float totalEnemyPower = 0f;
        for(UnitInfo enemy : enemyUnits) {
            totalEnemyPower += enemy.getHealth() + enemy.getAttack() / enemy.getType().attackCooldown * 40;
        }

        if(!pushing) {
            if(totalWolfPower > totalEnemyPower * 1.5f)
                pushing = true;
            else {
                alliedUnitLoop:
                for (UnitInfo unit : alliedUnits) {
                    Location loc = unit.getLocation();
                    for (UnitInfo enemy : enemyUnits) {
                        int dist = enemy.getLocation().distanceSquared(loc);
                        if (dist <= PUSH_DISTANCE && uc.senseIllumination(loc) > 6) {
                            pushing = true;
                            break alliedUnitLoop;
                        }
                    }
                }
            }
        }
        else {
            if(totalWolfPower < totalEnemyPower * 0.9f)
                pushing = false;
        }
        if(!pushing) {
            if(enemyUnits.length > 0) {
                float bestScore = 0f;
                Direction bestDir = Direction.ZERO;
                mainLoop:
                for(Direction dir : dirs) {
                    Location loc = uc.getLocation().add(dir);
                    if(!pathfinding.canMove(dir))
                        continue;

                    float score = 0f;

                    for(UnitInfo unit : enemyUnits) {
                        UnitType type = unit.getType();
                        int light = uc.senseIllumination(loc);
                        int dist = loc.distanceSquared(unit.getLocation());
                        int attackDangerMult = Math.min(light - 6, 4) * 150;

                        if(type == UnitType.WORKER) {
                            if(!uc.canSenseLocation(loc) || uc.isAccessible(loc))
                                score -= dist * 2;

                            if(light > 6) {
                                if (dist <= 5)
                                    score -= unit.getAttack() * 1000;
                            }

                            if (dist <= 13)
                                score += unit.getAttack() * 500;
                        }
                        else if(type == UnitType.AXEMAN) {
                            if(!uc.canSenseLocation(loc) || uc.isAccessible(loc))
                                score -= dist;
                            if(light > 6) {
                                if (dist <= 5)
                                    score -= unit.getAttack() * 1000;
                                else if (dist <= 13)
                                    score -= unit.getAttack() * attackDangerMult;
                                score -= light * 10;
                            }
                        }
                        else if(type == UnitType.SPEARMAN) {
                            if(!uc.canSenseLocation(loc) || uc.isAccessible(loc))
                                score -= dist * 3;

                            if(light > 6) {
                                if (dist <= 18 && dist >= 9)
                                    score -= unit.getAttack() * 2 * attackDangerMult;
                                else
                                    score -= unit.getAttack() * attackDangerMult;
                                score -= light * 20;
                            }
                        }
                        else if(type == UnitType.WOLF) {
                            if(!uc.canSenseLocation(loc) || uc.isAccessible(loc))
                                score -= dist;

                            if(dist <= 2)
                                score -= unit.getAttack() * 1000;
                            if(dist <= 8)
                                score -= unit.getAttack() * 500;
                        }
                        else
                            score -= dist;
                    }

                    for(UnitInfo unit : alliedUnits) {
                        if(unit.getType() == UnitType.WOLF)
                            score -= loc.distanceSquared(unit.getLocation()) * 0.4f;
                    }

                    if(score > bestScore) {
                        bestScore = score;
                        bestDir = dir;
                    }
                }

                if(bestDir != Direction.ZERO)
                    pathfinding.tryMove(bestDir);
            }
            else {
                Location loc = exploration.getLocation();
                if(loc == null) {
                    exploration = new Exploration(uc, exploration.CHUNK_SIZE, exploration.RESET_TURNS);
                    loc = exploration.getLocation();
                }
                pathfinding.pathfindTo(loc);
            }
        }
        else {
            // aggro micro

            for(Direction dir : dirs) {
                Location loc = uc.getLocation().add(dir);
                if(!pathfinding.canMove(dir))
                    continue;

                float score = 0f;

                for(UnitInfo unit : enemyUnits) {

                    UnitType type = unit.getType();
                    int light = uc.senseIllumination(loc);
                    int dist = loc.distanceSquared(unit.getLocation());
                    int attackDangerMult = Math.min(light - 6, 4) * 150;

                    float value = unit.getAttack() / type.attackCooldown / unit.getHealth();
                    score -= Math.sqrt(dist) * value * 10000;

                    if(type == UnitType.WORKER) {
                        if(!uc.canSenseLocation(loc) || uc.isAccessible(loc))
                            score -= dist * 2;

                        if(light > 6) {
                            if (dist <= 5)
                                score -= unit.getAttack() / unit.getType().attackCooldown * 1000;
                        }
                    }
                    else if(type == UnitType.AXEMAN) {
                        if(!uc.canSenseLocation(loc) || uc.isAccessible(loc))
                            score -= dist;
                        if(light > 6) {
                            if (dist <= 5)
                                score -= unit.getAttack() / unit.getType().attackCooldown * 1000;
                            else if (dist <= 13)
                                score -= unit.getAttack() / unit.getType().attackCooldown * attackDangerMult;
                            score -= light * 10;
                        }
                    }
                    else if(type == UnitType.SPEARMAN) {
                        if(!uc.canSenseLocation(loc) || uc.isAccessible(loc))
                            score -= dist * 3;

                        if(light > 6) {
                            if (dist <= 18 && dist >= 9)
                                score -= unit.getAttack() * 2 * attackDangerMult;
                            else
                                score -= unit.getAttack() * attackDangerMult;
                            score -= light * 20;
                        }
                    }
                    else if(type == UnitType.WOLF) {
                        if(!uc.canSenseLocation(loc) || uc.isAccessible(loc))
                            score -= dist;

                        if(dist <= 2)
                            score -= unit.getAttack() * 1000;
                        if(dist <= 8)
                            score -= unit.getAttack() * 500;
                    }
                    else
                        score -= dist;
                }
            }
        }

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

    boolean micro() {
        if (!uc.canMove())
            return false;

        UnitInfo[] enemies = uc.senseUnits(uc.getTeam().getOpponent());
        if (enemies.length == 0)
            return false;

        boolean inRangeOfAny = false;

        Direction bestDir = Direction.ZERO;
        int bestScore = -10000000;

        for (Direction dir : dirs) {
            if (!pathfinding.canMove(dir))
                continue;

            int score = 0;
            Location loc = uc.getLocation().add(dir);
            int light = uc.senseIllumination(loc);

            boolean visible = light >= 8;

            if (visible) { // visible
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

                if(!visible || !isObstructedNice(loc, enemy.getLocation())) {
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
                    inRangeOfAny = true;
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

        if(!inRangeOfAny)
            return false;

        if (bestDir != Direction.ZERO)
            return pathfinding.tryMove(bestDir);

        return true;
    }
}
