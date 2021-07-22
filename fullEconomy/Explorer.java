package fullEconomy;

import aic2021.user.*;

public class Explorer extends MyUnit {

    Explorer(UnitController uc){
        super(uc);
        pathfinding = new Pathfinding(uc);
    }

    Exploration exploration;
    Pathfinding pathfinding;

    void playRound(){
        sustainTorch();
        identifyBase();
        if (exploration == null && baseLocation != null){
            exploration = new Exploration(uc, baseLocation, 5, 50);
        }
        ResourceInfo[] resources = uc.senseResources();
        UnitInfo[] enemies = uc.senseUnits(uc.getTeam().getOpponent());
    }
}
