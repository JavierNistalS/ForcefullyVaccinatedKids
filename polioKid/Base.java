package polioKid;

import aic2021.user.*;

public class Base extends MyUnit {

    int workerCount = 0;
    int explorerCount = 0;
    int techIdx = 0;
    int enemyBaseCode = -1;

    Technology[] techObjective = {Technology.COIN, Technology.MILITARY_TRAINING, Technology.RAFTS};

    Base(UnitController uc){
        super(uc);
    }

    void playRound(){
        int[] info = uc.readSmokeSignals();
        if (info.length > 0){
            enemyBaseCode = info[0];
        }
        generalAttack();
        if (explorerCount == 0){
            if (spawnRandom(UnitType.EXPLORER)) explorerCount++;
        }
        else if (techIdx < techObjective.length){
            if (tryResearch(techObjective[techIdx]))
                techIdx++;
        }
        else if (workerCount == 0){
            if (spawnRandom(UnitType.WORKER)) workerCount++;
        }
        else if (enemyBaseCode >= 0){
            trySmokeSignal(enemyBaseCode);
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
