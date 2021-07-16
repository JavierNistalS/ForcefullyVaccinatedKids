package unmaskedKid;

import aic2021.user.*;

public class Base extends MyUnit {

    int lastWorker = -100;
    int techIdx = 0;

    Technology[] techObjective = {Technology.COIN, Technology.ROCK_ART, Technology.UTENSILS, Technology.JOBS, Technology.EUGENICS, Technology.VOCABULARY, Technology.SCHOOLS, Technology.WHEEL};
    TechGroup[] tech = {
        new TechGroup(Technology.COIN, Technology.ROCK_ART, Technology.MILITARY_TRAINING), // COIN es la prioridad, pero como ROCK_ART y BOXES no necesitan comida, no hay conflicto con las otras techs
        new TechGroup(Technology.JOBS),
        new TechGroup(Technology.SHARPENERS, Technology.COOKING),
        new TechGroup(Technology.TACTICS, Technology.EXPERTISE),
        new TechGroup(Technology.VOCABULARY, Technology.RAFTS, Technology.POISON),
        new TechGroup(Technology.WHEEL)
    };

    TechGroup unitCapTech = new TechGroup(Technology.HUTS, Technology.HOUSES);

    Base(UnitController uc){
        super(uc);
    }

    void playRound() {
        if(uc.getTotalUnits() == uc.getMaxTotalUnits())
            researchGroup(unitCapTech);
        else if(researchGroup(tech[techIdx]))
            techIdx++;

        if (techIdx > 3 && uc.getRound() < 200 && lastWorker < uc.getRound() - 150){
            if (spawnRandom(UnitType.WORKER)) lastWorker = uc.getRound();
        }
    }

    // returns true if ALL the Techs have been researched
    boolean researchGroup(TechGroup tg)  {
        for(Technology tech : tg.techs)
            if(tryResearch(tech))
                tg.researched++;

        return tg.researched == tg.techs.length;
    }

    boolean tryResearch(Technology tech){
        if (uc.canResearchTechnology(tech)){
            uc.researchTechnology(tech);
            return true;
        }
        return false;
    }
}
