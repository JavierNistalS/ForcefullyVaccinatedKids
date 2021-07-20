package dysenteryKid;

import aic2021.user.*;

public class Trapper extends MyUnit {

    Trapper(UnitController uc){
        super(uc);
    }

    void playRound(){
        UnitInfo[] units = uc.senseUnits();
        for (UnitInfo ui : units){
            if (ui.getType() == UnitType.BASE && ui.getTeam() == uc.getTeam())
                baseLocation = ui.getLocation();
        }
        /*if (uc.getInfo().getTorchRounds() < 10){
            tryLightTorch();
            if (uc.getInfo().getTorchRounds() < 10 && uc.getInfo().getTorchRounds() > 0){
                if (throwRandomTorch()){
                    tryLightTorch();
                }
            }
        }*/
        if ((uc.getLocation().x + uc.getLocation().y % 2) == 0)
            moveRandom();
        else
            moveRandomDiagonal();
        if (baseLocation != null && baseLocation.distanceSquared(uc.getLocation()) > 25){
            setRandomTrap();
        }
    }

    boolean setRandomTrap(){
        int k = 10;
        while (uc.canAttack() && k-- > 0){
            int random = (int)(uc.getRandomDouble()*8);
            Location loc = uc.getLocation().add(dirs[random]);
            if ((loc.x + loc.y) %2 == 0 && tryAttack(loc))
                return true;
        }
        return false;
    }

}
