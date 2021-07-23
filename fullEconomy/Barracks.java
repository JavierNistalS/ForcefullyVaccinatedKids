package fullEconomy;

import aic2021.user.Technology;
import aic2021.user.UnitController;
import aic2021.user.UnitInfo;
import aic2021.user.UnitType;

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

    int lastUpdate = -100;

    void playRound() {
        readSmokeSignals();
        if (lastUpdate < uc.getRound() - 60 && comms.sendMiscMessage(comms.MSG_BARRACKS_START))
            lastUpdate = uc.getRound();

        if (spawnedUnits < 2 && trySpawnUnit(UnitType.SPEARMAN))
            spawnedUnits++;
        if (spawnedUnits < 10 && !canBuildFarm && !canBuildQuarry && !canBuildSawmill && trySpawnUnit(UnitType.SPEARMAN)){
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
    }
}
