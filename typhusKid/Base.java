package typhusKid;

import aic2021.user.*;

public class Base extends MyUnit {

    int lastWorker = -100;
    int techIdx = 0;
    int explorerCount = 0;

    Technology[] techObjective = {Technology.COIN, Technology.ROCK_ART, Technology.MILITARY_TRAINING, Technology.JOBS, Technology.VOCABULARY, Technology.OIL, Technology.SCHOOLS, Technology.WHEEL};

    Base(UnitController uc){
        super(uc);
    }

    void playRound() {
        baseLocation = uc.getLocation();
        if(enemyBaseLocation == null || Math.abs(baseLocation.x - enemyBaseLocation.x) < 50 || Math.abs(baseLocation.y - enemyBaseLocation.y) < 50)
            ReadSmokeSignals();
        if (enemyBaseLocation != null){
            SendEnemyBaseSignal(enemyBaseLocation);
        }
        generalAttack();

        if(techIdx == techObjective.length)
            uc.killSelf();

        while(tryResearch(techObjective[techIdx]) && techIdx < techObjective.length)
            techIdx++;



        if(techIdx >= 2 && lastWorker < 0)
        {
            if (explorerCount == 0 && trySpawnUnit(UnitType.EXPLORER)){
                explorerCount++;
            }
            if(trySpawnUnit(UnitType.WORKER))
                lastWorker = uc.getRound();
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
