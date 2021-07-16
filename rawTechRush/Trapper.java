package rawTechRush;

import aic2021.user.*;

public class Trapper extends MyUnit {

    Trapper(UnitController uc){
        super(uc);
    }

    void playRound()
    {
        UnitInfo[] units = uc.senseUnits(Team.NEUTRAL);

        if(uc.canLightTorch())
            uc.lightTorch();

        if(uc.canMove())
        {
            if(units.length == 0)
                moveRandom();
            else {
                Direction dir = uc.getLocation().directionTo(units[0].getLocation());
                if(uc.canMove(dir))
                    uc.move(dir);
                else
                    moveRandom();
            }
        }

        if(units.length > 0 && uc.canAttack() && units[0].getLocation().distanceSquared(uc.getLocation()) <= 2)
            uc.attack(units[0].getLocation());

    }

}
