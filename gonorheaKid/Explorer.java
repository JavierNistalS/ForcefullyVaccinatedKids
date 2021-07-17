package gonorheaKid;

import aic2021.user.Direction;
import aic2021.user.UnitController;
import aic2021.user.UnitInfo;
import aic2021.user.UnitType;

public class Explorer extends MyUnit {

    Explorer(UnitController uc){
        super(uc);
    }

    boolean foundBase = false;

    void playRound()
    {
        if (uc.getInfo().getTorchRounds() < 10)
            tryLightTorch();
        uc.println('a');
        UnitInfo[] units = uc.senseUnits();
        for (UnitInfo u : units){
            if (u.getType() == UnitType.BASE && u.getTeam() == uc.getTeam().getOpponent())
                enemyBaseLocation = u.getLocation();
        }
        if (!foundBase && enemyBaseLocation != null){
            foundBase = SendEnemyBaseSignal(enemyBaseLocation);
        }
        randomExplore();
    }

    Direction randomDir = Direction.ZERO;

    void randomExplore(){
        uc.println(randomDir);
        if (!uc.canMove())
            return;
        uc.println("try");
        int k = 20;
        while (k > 0 && (randomDir == Direction.ZERO || !uc.canMove(randomDir))) {
            randomDir = dirs[(int)(uc.getRandomDouble() * 9)];
            uc.println(randomDir);
            k--;
        }
        uc.println("dir chosen");
        if (uc.canMove(randomDir) && randomDir != Direction.ZERO) {
            uc.println("moved");
            uc.move(randomDir);
        }
    }
}
