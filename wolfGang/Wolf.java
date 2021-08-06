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
        identifyEnemyBase();
        identifyBase();
        generalAttack();

        if(uc.canMove()) {
            float totalWolfPower = uc.getInfo().getHealth() + 80;
            for (UnitInfo unit : alliedUnits) {
                if (unit.getType() == UnitType.WOLF && unit.getLocation().distanceSquared(uc.getLocation()) <= 10)
                    totalWolfPower += unit.getHealth() + 80;
            }
            float totalEnemyPower = 0f;
            for (UnitInfo enemy : enemyUnits) {
                totalEnemyPower += enemy.getHealth() + enemy.getAttack() / enemy.getType().attackCooldown * 40;
            }

            if (!pushing) {
                if (totalWolfPower > totalEnemyPower * 1.5f)
                    pushing = enemyUnits.length > 0;
//            else {
//                alliedUnitLoop:
//                for (UnitInfo unit : alliedUnits) {
//                    Location loc = unit.getLocation();
//                    for (UnitInfo enemy : enemyUnits) {
//                        int dist = enemy.getLocation().distanceSquared(loc);
//                        if (dist <= PUSH_DISTANCE && uc.senseIllumination(loc) > 6) {
//                            pushing = true;
//                            break alliedUnitLoop;
//                        }
//                    }
//                }
//            }
            } else {
                if (totalWolfPower < totalEnemyPower * 0.9f || enemyUnits.length == 0)
                    pushing = false;
            }

            if(!pushing) {
                for (UnitInfo unit : enemyUnits) {
                    if ((unit.getType() == UnitType.WORKER || unit.getType() == UnitType.SPEARMAN) && unit.getLocation().distanceSquared(uc.getLocation()) <= 8)
                        pushing = true;
                }
            }

            uc.println("allied: " + totalWolfPower + ", enemy: " + totalEnemyPower + ", pushing: " + pushing);

            if (!pushing) {
                if (enemyUnits.length > 0) {
                    uc.println("camp micro");
                    float bestScore = -10e30f;
                    Direction bestDir = Direction.ZERO;

                    for (Direction dir : dirs) {
                        if (!pathfinding.canMove(dir))
                            continue;

                        Location loc = uc.getLocation().add(dir);
                        int light = uc.senseIllumination(loc);
                        int attackDangerMult = Math.min(light - 6, 4) * 150;

                        float score = 0f;

                        for (UnitInfo unit : enemyUnits) {
                            UnitType type = unit.getType();
                            int dist = loc.distanceSquared(unit.getLocation());

                            if (type == UnitType.WORKER) {
                                if (!uc.canSenseLocation(loc) || uc.isAccessible(loc))
                                    score -= dist * 2;

                                if (light > 6) {
                                    if (dist <= 5)
                                        score -= unit.getAttack() * 1000;
                                }

                                if (dist <= 13)
                                    score += unit.getAttack() * 500;
                            } else if (type == UnitType.AXEMAN) {
                                if (!uc.canSenseLocation(loc) || uc.isAccessible(loc))
                                    score -= dist;
                                if (light > 6) {
                                    if (dist <= 5)
                                        score -= unit.getAttack() * 1000;
                                    else if (dist <= 13)
                                        score -= unit.getAttack() * attackDangerMult;
                                    score -= light * 10;
                                }
                            } else if (type == UnitType.SPEARMAN) {
                                if (!uc.canSenseLocation(loc) || uc.isAccessible(loc))
                                    score -= dist * 3;

                                if (light > 6) {
                                    if (dist <= 18 && dist >= 9)
                                        score -= unit.getAttack() * 2 * attackDangerMult;
                                    else
                                        score -= unit.getAttack() * attackDangerMult;
                                    score -= light * 20;
                                }
                            } else if (type == UnitType.WOLF) {
                                if (!uc.canSenseLocation(loc) || uc.isAccessible(loc))
                                    score -= dist;

                                if (dist <= 2)
                                    score -= unit.getAttack() * 1000;
                                if (dist <= 8)
                                    score -= unit.getAttack() * 500;
                            } else
                                score -= dist;
                        }

                        for (UnitInfo unit : alliedUnits) {
                            if (unit.getType() == UnitType.WOLF)
                                score -= loc.distanceSquared(unit.getLocation()) * 0.4f;
                        }

                        uc.println(dir + ": " + score);

                        if (score > bestScore) {
                            bestScore = score;
                            bestDir = dir;
                        }
                    }

                    uc.drawLineDebug(uc.getLocation(), uc.getLocation().add(bestDir), 0, 255, 0);
                    if (bestDir != Direction.ZERO)
                        pathfinding.tryMove(bestDir);

                } else {
                    uc.println("exploring");

                    Location loc = exploration.getLocation();
                    if (loc == null) {
                        exploration = new Exploration(uc, exploration.CHUNK_SIZE, exploration.RESET_TURNS);
                        loc = exploration.getLocation();
                    }
                    pathfinding.pathfindTo(loc);
                }
            } else {
                uc.println("aggro micro");

                Location mostValuableTarget = null;
                float mostValuableTargetValue = 0;

                for(UnitInfo unit : enemyUnits) {
                    Location loc = unit.getLocation();
                    int dist = uc.getLocation().distanceSquared(loc);
                    float value = unit.getAttack() / uc.getType().attackCooldown / unit.getHealth() / dist;
                    int light = uc.senseIllumination(loc);

                    if(light < 6)
                        value *= 4f;
                    else if(light < 8)
                        value *= 2.5f;
                    else if(light < 10)
                        value *= 1.8f;

                    if(value > mostValuableTargetValue && (enemyBaseLocation == null || loc.distanceSquared(enemyBaseLocation) > 18)) {
                        mostValuableTargetValue = value;
                        mostValuableTarget = loc;
                    }
                }

                float bestScore = -10e30f;
                Direction bestDir = Direction.ZERO;

                for (Direction dir : dirs) {
                    Location loc = uc.getLocation().add(dir);

                    if (!pathfinding.canMove(dir))
                        continue;

                    int light = uc.senseIllumination(loc);
                    int attackDangerMult = Math.min(light - 6, 4) * 150;
                    float score = 0f;

                    for (UnitInfo unit : enemyUnits) {
                        UnitType type = unit.getType();
                        int dist = loc.distanceSquared(unit.getLocation());

                        if (type == UnitType.WORKER) {
                            if (!uc.canSenseLocation(loc) || uc.isAccessible(loc))
                                score -= dist * 20;

                            if (light > 6) {
                                if (dist <= 5)
                                    score -= unit.getAttack() / unit.getType().attackCooldown * 1000;
                            }
                        } else if (type == UnitType.AXEMAN) {
                            if (!uc.canSenseLocation(loc) || uc.isAccessible(loc))
                                score -= dist * 10;
                            if (light > 6) {
                                if (dist <= 5)
                                    score -= unit.getAttack() / unit.getType().attackCooldown * 1000;
                                else if (dist <= 13)
                                    score -= unit.getAttack() / unit.getType().attackCooldown * attackDangerMult;
                                score -= light * 10;
                            }
                        } else if (type == UnitType.SPEARMAN) {
                            if (!uc.canSenseLocation(loc) || uc.isAccessible(loc))
                                score -= dist * 30;

                            if (light > 6) {
                                if (dist <= 18 && dist >= 9)
                                    score -= unit.getAttack() * 2 * attackDangerMult;
                                else
                                    score -= unit.getAttack() * attackDangerMult;
                                score -= light * 20;
                            }
                        } else if (type == UnitType.WOLF) {
                            if (!uc.canSenseLocation(loc) || uc.isAccessible(loc))
                                score -= dist * 10;

                            if (dist <= 2)
                                score -= unit.getAttack() * 1000;
                            if (dist <= 8)
                                score -= unit.getAttack() * 500;
                        } else
                            score -= dist * 10;
                    }

                    if(mostValuableTarget != null)
                        score -= mostValuableTarget.distanceSquared(loc) * 10000;

                    uc.println(dir + ": " + score);

                    if (score > bestScore) {
                        bestScore = score;
                        bestDir = dir;
                    }
                }
                uc.drawLineDebug(uc.getLocation(), uc.getLocation().add(bestDir), 255, 255, 0);

                if (bestDir != Direction.ZERO)
                    pathfinding.tryMove(bestDir);
            }
        }

        identifyBase();
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
