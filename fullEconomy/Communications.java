package fullEconomy;
import aic2021.user.*;


public class Communications {

    final int[] SHUFFLE_NUMBER =
              {20, 23,  8, 15,
               17,  6, 10,  3,
               26, 28,  1,  7,
               13, 19,  2,  4,
               11, 22, 21, 31,
               16, 24, 14, 30,
                0, 12, 27, 29,
                9, 25,  5, 18};
    final int XOR_NUMBER = 0;
    final int VALIDATION_BITS = 11; //Last round digits / 2
    final int TYPE_BITS = 3;
    final int INFO_BITS = 18;
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


    public Communications(UnitController uc) {
        this.uc = uc;
    }

    int encrypt(int msg) {
        msg ^= XOR_NUMBER;
        long smoke = 0;
        for (int i = 0; i < 32; i++) {
            smoke |= ((((long)msg & ((long)1 << (long)i)) >> (long)i) << (long)SHUFFLE_NUMBER[i]);
        }
        return (int)smoke;
    }
    int decrypt(int smoke) {
        int msg = 0;
        for (int i = 0; i < 32; i++) {
            long sy = SHUFFLE_NUMBER[i];
            msg |= ((((long)smoke & ((long)1 << (long)sy)) >> (long)sy) << (long)i);
        }
        return msg ^ XOR_NUMBER;
    }

    boolean validate(int x) {
        int mod = 1 << VALIDATION_BITS;
        int currentRound = uc.getRound() % mod;
        int lastRound = (uc.getRound() - 1) % mod;
        int msgRound = x >> (TYPE_BITS + INFO_BITS);
        return msgRound == currentRound || msgRound == lastRound;
    }

    boolean sendMessage(int type, int info) {
        if (uc.canMakeSmokeSignal()){
            int msg = info + ((type + (uc.getRound() << TYPE_BITS)) << INFO_BITS);
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
        return sendMessage(MSG_TYPE_MISC, info);
    }
    boolean sendLocationMessage(int type, Location loc) {
        return sendMessage(type, locationToInt(loc));
    }

    int getType(int x) {
        return ((x << VALIDATION_BITS) >> (VALIDATION_BITS + INFO_BITS));
    }
    int getInfo(int x) {
        return (x << (VALIDATION_BITS + TYPE_BITS)) >> (VALIDATION_BITS + TYPE_BITS);
    }

    int locationToInt(Location loc) {
        return ((loc.x%128) << 7) + (loc.y%128);
    }
    Location intToLocation(int msg) {
        int x = ((msg << 18) >> 25);
        int y = ((msg << 25) >> 25);
        Location act = uc.getLocation();
        x = act.x/128*128 + x;
        y = act.y/128*128 + y;
        while (x > act.x + 50)
            x -= 128;
        while (x < act.x - 50)
            x += 128;
        while (y > act.y + 50)
            x -= 128;
        while (y < act.y - 50)
            y += 128;
        return new Location(x, y);
    }
}
