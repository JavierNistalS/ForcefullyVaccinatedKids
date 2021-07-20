package choleraKid;

import aic2021.user.*;

public class Trapper extends MyUnit {

    Trapper(UnitController uc){
        super(uc);
    }

    void playRound(){
        Location[] traps = uc.senseTraps();
        if (traps.length > 0){
            tryMove(uc.getLocation().directionTo(traps[0]));
        }
        UnitInfo[] units = uc.senseUnits();
        for (UnitInfo ui : units){
            if (ui.getType() == UnitType.BASE && ui.getTeam() == uc.getTeam())
                baseLocation = ui.getLocation();
        }
        if (baseLocation != null){
            if (baseLocation.distanceSquared(uc.getLocation()) < 50){
                moveRandom();
            }
            else{
                for(Direction dir : dirs){
                    tryAttack(uc.getLocation().add(dir));
                }
            }
        }
    }

}
