package fullEconomy;

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

    void playRound() {
        readSmokeSignals();

        // spawning workers (& other units)
        totalResourcesSeen = 0;
        ResourceInfo[] resources = uc.senseResources();
        for(ResourceInfo resource : resources)
            totalResourcesSeen += resource.amount;

        if(workerCount < (totalResourcesSeen / 350) && workerCount < 2)
            if(trySpawnUnit(UnitType.WORKER))
                workerCount++;

        // sending signals
        if(!genevaSuggestion || toldLocationCountdown <= 0) {
            if(comms.sendLocationMessage(comms.MSG_TYPE_ALLIED_SETTLEMENT, uc.getLocation()))
                toldLocationCountdown = 6;
        }
        else {
            kgb.disruptEveryone(enemyBaseLocation);
            toldLocationCountdown--;
        }

        if (wolfCount == 0 || uc.getResource(Resource.FOOD) > 4000){
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
            }
        }
    }
}
