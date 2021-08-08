package wolfGang;
import aic2021.user.*;


public class Communications {

    final int XOR_NUMBER = -1251833774;
    final int ADD_NUMBER = -19568032;

    final int ROUND_VALIDATION_BITS = 11; //Last round digits
    final int OFFSET_VALIDATION_BITS = 4;
    final int TYPE_BITS = 3;
    final int INFO_BITS = 14;
    final int RANDOM_BITS = 4;
    UnitController uc;

    final int MSG_TYPE_ENEMY_BASE = 0;
    final int MSG_TYPE_DEER = 1;
    final int MSG_TYPE_ALLIED_SETTLEMENT = 2;
    final int MSG_TYPE_MISC = 3;

    // unit existance [0-99]
    final int MSG_FARM_START = 0;
    final int MSG_SAWMILL_START = 1;
    final int MSG_QUARRY_START = 2;
    final int MSG_BARRACKS_START = 3;
    final int MSG_FARM_END = 10;
    final int MSG_SAWMILL_END = 11;
    final int MSG_QUARRY_END = 12;
    final int MSG_WORKER_END = 13;

    // econonomy control [100-199]
    final int MSG_STOP_BUILDING_SETTLEMENT_TO_COLLECT_FOOD = 100;
    final int MSG_STOP_BUILDING_SETTLEMENT_TO_COLLECT_WOOD = 101;
    final int MSG_STOP_BUILDING_SETTLEMENT_TO_COLLECT_STONE = 102;
    final int MSG_STOP_BUILDING_FARMS = 110;
    final int MSG_STOP_BUILDING_SAWMILLS = 111;
    final int MSG_STOP_BUILDING_QUARRYS = 112;
    final int MSG_STOP_BUILDING_RESOURCE_BUILDINGS = 113;
    final int MSG_START_BUILDING_FARMS = 120;
    final int MSG_START_BUILDING_SAWMILLS = 121;
    final int MSG_START_BUILDING_QUARRYS = 122;
    final int MSG_START_BUILDING_RESOURCE_BUILDINGS = 123;
    final int MSG_REQUEST_RAFTS = 124;

    //Military
    final int MSG_REINFORCE_BASE = 200;



    public Communications(UnitController uc) {
        this.uc = uc;
    }

    int encrypt(int msg) {
        /*for (int i = 1; i < 32; i++){
            msg ^= (msg & (1 << (i-1))) << 1;
        }*/
        msg += ADD_NUMBER;
        msg ^= XOR_NUMBER;
        int smoke = msg;
        /*for (int i = 0; i < 32; i++) {
            smoke |= (((msg & (1 << i)) >>> i) << SHUFFLE_NUMBER[i]);
        }*/
        return smoke;
    }
    int decrypt(int smoke) {
        int bytecode = uc.getEnergyLeft();
        int msg = smoke;
        /*for (int i = 0; i < 32; i++) {
            int sy = SHUFFLE_NUMBER[i];
            msg |= (((smoke & (1 << sy)) >>> sy) << i);
        }*/

        msg ^= XOR_NUMBER;
        msg -= ADD_NUMBER;
        /*for (int i = 31; i > 0; i--){
            msg ^= (msg << 1) & (1 << i);
        }*/
        uc.println("decrypt cost: " + (bytecode - uc.getEnergyLeft()));
        return msg;
    }

    boolean validate(int x) {
        uc.println("validating " + x);
        int bytecode = uc.getEnergyLeft();
        int mod = 1 << ROUND_VALIDATION_BITS;
        int currentRound = uc.getRound() % mod;
        int lastRound = (uc.getRound() - 1) % mod;
        int msgRound = x >>> (TYPE_BITS + INFO_BITS + OFFSET_VALIDATION_BITS);
        if (currentRound != msgRound && lastRound != msgRound) {
            uc.println("validate cost: " + (bytecode - uc.getEnergyLeft()));
            return false;
        }
        int offsetMod = 1 << OFFSET_VALIDATION_BITS;
        int offset = (uc.getLocation().x)/50%offsetMod;
        int offsetM1 = (offset + offsetMod - 1)%offsetMod;
        int offsetP1 = (offset + offsetMod + 1)%offsetMod;
        int msgOffset = (x << ROUND_VALIDATION_BITS) >>> (ROUND_VALIDATION_BITS + INFO_BITS + TYPE_BITS);
        boolean ans = (msgOffset == offset || msgOffset == offsetM1 || msgOffset == offsetP1);
        uc.println("validate cost: " + (bytecode - uc.getEnergyLeft()));
        return ans;
    }

    boolean sendMessage(int type, int info) {
        if (uc.canMakeSmokeSignal()){
            int val = uc.getLocation().x/50%(1 << OFFSET_VALIDATION_BITS) + (uc.getRound() << OFFSET_VALIDATION_BITS);
            int msg = info + ((type + (val << TYPE_BITS)) << INFO_BITS);
            uc.println("decrypted: " + msg);
            msg = encrypt(msg);
            uc.println("encrypted: " + msg);
            msg = decrypt(msg);
            uc.println("re-decrypted: " + msg);
            uc.makeSmokeSignal(encrypt(msg));
            return true;
        }
        return false;
    }
    boolean sendMiscMessage(int info) {
        return sendMessage(MSG_TYPE_MISC, info | ((uc.getInfo().getID()%(1<<RANDOM_BITS)) << (INFO_BITS - RANDOM_BITS)));
    }
    boolean sendLocationMessage(int type, Location loc) {
        return sendMessage(type, locationToInt(loc));
    }

    int getType(int x) {
        return ((x << (OFFSET_VALIDATION_BITS + ROUND_VALIDATION_BITS)) >>> (OFFSET_VALIDATION_BITS + ROUND_VALIDATION_BITS + INFO_BITS));
    }
    int getInfo(int x) {
        if (getType(x) == MSG_TYPE_MISC)
            return (x << (OFFSET_VALIDATION_BITS + ROUND_VALIDATION_BITS + TYPE_BITS + RANDOM_BITS)) >>> (OFFSET_VALIDATION_BITS + ROUND_VALIDATION_BITS + TYPE_BITS + RANDOM_BITS);
        else
            return (x << (OFFSET_VALIDATION_BITS + ROUND_VALIDATION_BITS + TYPE_BITS)) >>> (OFFSET_VALIDATION_BITS + ROUND_VALIDATION_BITS + TYPE_BITS);
    }

    int locationToInt(Location loc) {
        return ((loc.x%128) << 7) + (loc.y%128);
    }
    Location intToLocation(int msg) {
        int x = ((msg << 18) >>> 25);
        int y = ((msg << 25) >>> 25);
        Location act = uc.getLocation();
        x = act.x/128*128 + x;
        y = act.y/128*128 + y;
        while (x > act.x + 50) {
            x -= 128;
        }
        while (x < act.x - 50) {
            x += 128;
        }
        while (y > act.y + 50) {
            y -= 128;
        }
        while (y < act.y - 50) {
            y += 128;
        }
        return new Location(x, y);
    }
}
