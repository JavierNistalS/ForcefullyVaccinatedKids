package dysenteryKid;

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
        generalAttack();
        trySpawnUnit(UnitType.TRAPPER);
    }


    boolean tryResearch(Technology tech){
        if (uc.canResearchTechnology(tech)){
            uc.researchTechnology(tech);
            return true;
        }
        return false;
    }
}
