package wolfGang;

import aic2021.user.*;

public class Settlement extends MyUnit {

    Settlement(UnitController uc) {
        super(uc);
        comms = new Communications(uc);
        kgb = new TheKGB(uc);
    }

    Communications comms;
    TheKGB kgb;


    int toldLocationCountdown = 0;
    int workerCount = 0;
    int totalResourcesSeen = 0;
    int wolfCount = 0;
    int totalWorkersSeen = 0;
    int lastWorkerSeenRound = 0;
    boolean requestedRafts = false;

    int enemyHostiles = 0;
    int enemyWorkers = 0;

    void playRound() {
        readSmokeSignals();

        boolean needsRafts = !(uc.hasResearched(Technology.RAFTS, uc.getTeam())) && requestedRafts;

        // spawning workers (& other units)
        totalResourcesSeen = 0;
        ResourceInfo[] resources = uc.senseResources();
        for(ResourceInfo resource : resources)
            totalResourcesSeen += resource.amount;

        UnitInfo[] allies = uc.senseUnits(uc.getTeam());
        for (UnitInfo ui : allies){
            if (ui.getType() == UnitType.WORKER) {
                totalWorkersSeen++;
                lastWorkerSeenRound = uc.getRound();
            }
        }

        enemyHostiles = 0;
        enemyWorkers = 0;

        UnitInfo[] enemies = uc.senseUnits(uc.getTeam().getOpponent());
        for (UnitInfo ui : enemies){
            if (ui.getType() == UnitType.WORKER)
                enemyWorkers++;
            else if (ui.getType() != UnitType.BASE && ui.getAttack() > 0){
                enemyHostiles++;
            }
        }

        if (!needsRafts && (enemyWorkers > 0 || enemyHostiles > 0) && enemyHostiles < 3 && wolfCount < 4){
            if (trySpawnUnit(UnitType.WOLF))
                wolfCount++;
        }

        if (!needsRafts && enemyWorkers > 0 && enemyWorkers < 5 && enemyHostiles == 0 && workerCount < 10 + uc.getRound() / 200){
            if (trySpawnUnit(UnitType.WORKER))
                workerCount++;
        }

        if(!needsRafts && workerCount < (totalResourcesSeen / 350) && workerCount < 5 + uc.getRound() / 200)
            if(trySpawnUnit(UnitType.WORKER))
                workerCount++;

        if (!needsRafts && ((lastWorkerSeenRound < uc.getRound() - 30 && totalResourcesSeen >= 200) || lastWorkerSeenRound < uc.getRound() - 200)){
            if(canBuildUnitWithMargin(UnitType.WORKER, 0, 75, 75) && trySpawnUnit(UnitType.WORKER))
                workerCount++;
        }

        if(uc.canMakeSmokeSignal())
            toldLocationCountdown--;

        // sending signals
        if(!genevaSuggestion || toldLocationCountdown <= 0) {
            if(comms.sendLocationMessage(comms.MSG_TYPE_ALLIED_SETTLEMENT, uc.getLocation()))
                toldLocationCountdown = 6;
        }
        else {
            kgb.disruptEveryone(enemyBaseLocation);
        }

        if (wolfCount == 0 || (wolfCount < 2 && (uc.getResource(Resource.FOOD) > 1600))){
            if (trySpawnUnit(UnitType.WOLF))
                wolfCount++;
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
                    readMiscMessages(comms.getInfo(msg));
            }
        }
    }

    void readMiscMessages(int info) {
        if(info == comms.MSG_REQUEST_RAFTS) {
            requestedRafts = true;
        }
    }
}
