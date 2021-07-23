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

    void playRound() {
        readSmokeSignalBuilding(comms);

        // spawning workers (& other units)
        totalResourcesSeen = 0;
        ResourceInfo[] resources = uc.senseResources();
        for(ResourceInfo resource : resources)
            totalResourcesSeen += resource.amount;

        if(workerCount < (totalResourcesSeen / 350))
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
    }
}
