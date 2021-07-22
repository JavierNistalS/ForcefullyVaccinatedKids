package nullplayer;

import aic2021.user.*;

public class UnitPlayer {

	public void run(UnitController uc) {
		// Code to be executed only at the beginning of the unit's lifespan

		while (true) {
			generalAttack(uc);

			// Code to be executed every round
			uc.yield(); // End of turn
		}
	}

	boolean generalAttack(UnitController uc){
		int lessHpAggro = 100000000;
		int lessHpNonAggro = 100000000;
		Location best = null;
		UnitInfo[] units = uc.senseUnits(uc.getTeam().getOpponent());
		for (UnitInfo u : units){
			int hp = u.getHealth();
			if (u.getType().attack > 0){
				if (lessHpAggro > hp){
					lessHpAggro = hp;
					if (uc.canAttack(u.getLocation()))
						best = u.getLocation();
				}
			}
			else{
				if (lessHpAggro == 100000000 && hp < lessHpNonAggro){
					lessHpNonAggro = hp;
					if (uc.canAttack(u.getLocation()))
						best = u.getLocation();
				}
			}
		}
		if (best != null) {
			uc.attack(best);
			return true;
		}
		return false;
	}
}
