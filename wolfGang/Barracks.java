package wolfGang;

import aic2021.user.*;

public class Barracks extends MyUnit {

    Barracks(UnitController uc){
        super(uc);
        comms = new Communications(uc);
        kgb = new TheKGB(uc);
    }

    Communications comms;
    TheKGB kgb;
    int totalEnemyAttack;
    int spawnedUnits = 0;

    boolean canBuildFarm = true;
    int farmUpdateRound = -10;
    boolean canBuildSawmill = true;
    int sawmillUpdateRound = -10;
    boolean canBuildQuarry = true;
    int quarryUpdateRound = -10;

    boolean reinforceRequested = false;
    boolean raftsRequested = false;

    int lastUpdate = -100;

    void playRound() {
        identifyBase();
        readSmokeSignals();
        readRocks();
        if (lastUpdate < uc.getRound() - 60 && comms.sendMiscMessage(comms.MSG_BARRACKS_START))
            lastUpdate = uc.getRound();

        if (!raftsRequested || uc.hasResearched(Technology.RAFTS, uc.getTeam())) {
            if (uc.getTotalUnits() <= 45 && (((spawnedUnits < 3 + uc.getRound() / 250) || (reinforceRequested && spawnedUnits < 8 + uc.getRound() / 250)) && trySpawnSpearman()))
                spawnedUnits++;
            if (spawnedUnits < 10 && !canBuildFarm && !canBuildQuarry && !canBuildSawmill && trySpawnSpearman()) {
                spawnedUnits++;
            }
        }

        if(genevaSuggestion) {
            kgb.disruptEveryone(enemyBaseLocation);
        }
    }

    void readSmokeSignals() {
        uc.println("reading smoke signals");
        int[] smokeSignals = uc.readSmokeSignals();

        for(int smokeSignal : smokeSignals) {
            int msg = comms.decrypt(smokeSignal);
            if(comms.validate(msg)) {
                int msgType = comms.getType(msg);
                if (msgType == comms.MSG_TYPE_ENEMY_BASE)
                    enemyBaseLocation = comms.intToLocation(msg);
                else if(msgType == comms.MSG_TYPE_MISC)
                    readMiscMessage(comms.getInfo(msg));
            }
        }
    }

    void readMiscMessage(int info) {
        if (info == comms.MSG_START_BUILDING_SAWMILLS) {
            canBuildSawmill = true;
            sawmillUpdateRound = uc.getRound();
        }
        if (info == comms.MSG_STOP_BUILDING_SAWMILLS && sawmillUpdateRound < uc.getRound())
            canBuildSawmill = false;
        if (info == comms.MSG_START_BUILDING_FARMS) {
            canBuildFarm = true;
            farmUpdateRound = uc.getRound();
        }
        if (info == comms.MSG_STOP_BUILDING_FARMS && farmUpdateRound < uc.getRound())
            canBuildFarm = false;
        if (info == comms.MSG_START_BUILDING_QUARRYS) {
            canBuildQuarry = true;
            quarryUpdateRound = uc.getRound();
        }
        if (info == comms.MSG_STOP_BUILDING_QUARRYS && quarryUpdateRound < uc.getRound())
            canBuildQuarry = false;
        if (info == comms.MSG_REINFORCE_BASE){
            reinforceRequested = true;
        }
        if (info == comms.MSG_REQUEST_RAFTS)
            raftsRequested = true;
    }

    boolean trySpawnSpearman() {
        if(canBuildUnitWithMargin(UnitType.SPEARMAN, 140, 30, 30)) {
            for (Direction dir : dirs) {
                if (uc.canSpawn(UnitType.SPEARMAN, dir)) {
                    Location loc = uc.getLocation().add(dir);
                    if (enemyBaseLocation != null && loc.distanceSquared(enemyBaseLocation) <= 18 && (!uc.canSenseLocation(enemyBaseLocation) || !uc.isObstructed(enemyBaseLocation, loc)))
                        continue;
                    if (baseLocation.distanceSquared(loc) > 2)
                        continue;
                    if (trySpawnWithMargin(UnitType.SPEARMAN, dir)) {
                        return true;
                    }
                }
            }

            for (Direction dir : dirs) {
                if (uc.canSpawn(UnitType.SPEARMAN, dir)) {
                    Location loc = uc.getLocation().add(dir);
                    if (enemyBaseLocation != null && loc.distanceSquared(enemyBaseLocation) <= 18 && (!uc.canSenseLocation(enemyBaseLocation) || !uc.isObstructed(enemyBaseLocation, loc)))
                        continue;
                    if (trySpawnWithMargin(UnitType.SPEARMAN, dir)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
