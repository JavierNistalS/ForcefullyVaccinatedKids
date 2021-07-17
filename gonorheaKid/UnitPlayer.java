package gonorheaKid;

import aic2021.user.*;

public class UnitPlayer {

	public void run(UnitController uc) {

		try {
			UnitType t = uc.getType();
			MyUnit u;
			uc.println("hello world");

			if (t == UnitType.BASE) u = new Base(uc);
			else if (t == UnitType.WORKER) u = new Worker(uc);
			else if (t == UnitType.EXPLORER) u = new Explorer(uc);
			else if (t == UnitType.TRAPPER) u = new Trapper(uc);
			else if (t == UnitType.AXEMAN) u = new Axeman(uc);
			else if (t == UnitType.SPEARMAN) u = new Spearman(uc);
			else if (t == UnitType.WOLF) u = new Wolf(uc);
			else if (t == UnitType.SETTLEMENT) u = new Settlement(uc);
			else if (t == UnitType.BARRACKS) u = new Barracks(uc);
			else if (t == UnitType.FARM) u = new Farm(uc);
			else if (t == UnitType.QUARRY) u = new Quarry(uc);
			else u = new Sawmill(uc);

			while (true) {
				u.playRound();
				uc.yield();
			}
		}
		catch(Exception e){
			uc.println(e.toString());
			uc.yield();
			uc.println(e.toString());
			uc.yield();
			uc.println(e.toString());
			uc.yield();
			uc.println(e.toString());
			uc.yield();
			uc.println(e.toString());
			uc.yield();
			uc.println(e.toString());
			uc.yield();
			uc.println(e.toString());
			uc.killSelf();
		}
	}

}
