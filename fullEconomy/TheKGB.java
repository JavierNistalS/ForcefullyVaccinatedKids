package fullEconomy;

import aic2021.user.*;

public class TheKGB {
    UnitController uc;
    Communications comms;

    public TheKGB(UnitController uc){
        this.uc = uc;
        comms = new Communications(uc);
    }

    public boolean disruptRosa(Location loc){
        int message = 1050*loc.x + loc.y + 180500000;
        return trySmokeSignal(message);
    }

    public boolean disruptCarbassots(Location loc){
        int message = 2*loc.x + 8192*loc.y - 1073741823;
        return trySmokeSignal(message);
    }

    public boolean disruptWololo(Location loc, boolean explorer){
        int message = 32768*loc.x + 16*loc.y - 1006632960;
        if (explorer)
            message += 4;
        return trySmokeSignal(message);
    }

    public boolean trySmokeSignal(int x){
        if (comms.validate(comms.decrypt(x)))
            return false;
        if (uc.canMakeSmokeSignal()){
            uc.makeSmokeSignal(x);
            return true;
        }
        return false;
    }
}
