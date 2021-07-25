package chineseFluKid;

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

    int lastUpdate = -100;

    void playRound() {
        identifyBase();
        readSmokeSignals();
        if (lastUpdate < uc.getRound() - 60 && comms.sendMiscMessage(comms.MSG_BARRACKS_START))
            lastUpdate = uc.getRound();

        if ((spawnedUnits < 8 || (reinforceRequested && spawnedUnits < 10)) && trySpawnSpearman())
            spawnedUnits++;
        if (spawnedUnits < 15 && !canBuildFarm && !canBuildQuarry && !canBuildSawmill && trySpawnSpearman()){
            spawnedUnits++;
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
    }

    boolean trySpawnSpearman(){
        for (Direction dir : dirs){
            if (uc.canSpawn(UnitType.SPEARMAN, dir)){
                Location loc = uc.getLocation().add(dir);
                if (enemyBaseLocation != null && loc.distanceSquared(enemyBaseLocation) <= 18)
                    continue;
                if (baseLocation.distanceSquared(loc) > 2)
                    continue;
                if (trySpawnUnit(UnitType.SPEARMAN, dir)){
                    return true;
                }
            }
        }
        return false;
    }
}
