package fullEconomy;
import aic2021.user.*;


public class Communications {

    final int[] SHUFFLE_NUMBER = {20, 23, 8, 15, 17, 6, 10, 3, 26, 28, 1, 7, 13, 19, 2, 4, 11, 22, 21, 31, 16, 24, 14, 30, 0, 12, 27, 29, 9, 25, 5, 18};
    final int XOR_NUMBER = 1916125831;
    final int VALIDATION_BITS = 8; //Last round digits / 2
    final int TYPE_BITS = 2;
    final int INFO_BITS = 22;
    UnitController uc;

    final int MSG_TYPE_ENEMY_BASE = 0;
    final int MSG_TYPE_DEER = 1;
    final int MSG_TYPE_ALLIED_SETTLEMENT = 2;
    final int MSG_TYPE_MISC = 3;

    final int MSG_MISC_STOP_BUILDING = 0;
    final int MSG_MISC_RESOURCE_BUILDING_START_EXISTING = 1;

    public Communications(UnitController uc) {
        this.uc = uc;
    }

    int encrypt(int x) {
        x ^= XOR_NUMBER;
        int y = 0;
        for (int i = 0; i < 32; i++){
            y += (((x & (1 << i)) >> i) << SHUFFLE_NUMBER[i]);
        }
        return y;
    }
    int decrypt(int y) {
        int x = 0;
        for (int i = 0; i < 32; i++){
            int sy = SHUFFLE_NUMBER[i];
            x += (((y & (1 << sy)) >> sy) << i);
        }
        return x^XOR_NUMBER;
    }

    boolean validate(int x) {
        return (x >> (TYPE_BITS + INFO_BITS)) == (uc.getRound() % (1 << (VALIDATION_BITS + 1))) / 2;
    }

    boolean sendMessage(int type, int info){
        if (uc.canMakeSmokeSignal()){
            int x = info + ((type + (uc.getRound() << TYPE_BITS)) << INFO_BITS);
            x = encrypt(x);
            uc.makeSmokeSignal(x);
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
        return (loc.x << 11) + loc.y;
    }
    Location intToLocation(int x) {
        return new Location((x << 10) >> 21, (x << 21) >> 21);
    }
}
