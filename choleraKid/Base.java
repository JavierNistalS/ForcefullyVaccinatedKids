package choleraKid;

import aic2021.user.*;

public class Base extends MyUnit {

    int workerCount = 0;
    int explorerCount = 0;
    int techIdx = 0;
    int enemyBaseCode = -1;

    Technology[] techObjective = {Technology.COIN, Technology.MILITARY_TRAINING};

    Base(UnitController uc){
        super(uc);
    }

    void playRound(){

        baseLocation = uc.getLocation();
        if(enemyBaseLocation == null || Math.abs(baseLocation.x - enemyBaseLocation.x) < 50 || Math.abs(baseLocation.y - enemyBaseLocation.y) < 50)
            ReadSmokeSignals();
        generalAttack();
        if (explorerCount == 0){
            if (spawnRandom(UnitType.EXPLORER))
                explorerCount++;
        }
        else if (techIdx < techObjective.length){
            if (tryResearch(techObjective[techIdx]))
                techIdx++;
        }
        else if (workerCount == 0){
            if (spawnRandom(UnitType.WORKER))
                workerCount++;
        }
        else if (enemyBaseLocation != null){
            SendEnemyBaseSignal(enemyBaseLocation);
        }
    }


    boolean tryResearch(Technology tech){
        if (uc.canResearchTechnology(tech)){
            uc.researchTechnology(tech);
            return true;
        }
        return false;
    }
}
