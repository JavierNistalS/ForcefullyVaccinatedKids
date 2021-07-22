package fullEconomy;
import aic2021.user.*;


public class Communications {

    final int[] SHUFFLE_NUMBER = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31};
    final int XOR_NUMBER = 0;
    final int VALIDATION_BITS = 8; //Last round digits / 2
    final int TYPE_BITS = 2;
    final int INFO_BITS = 22;
    UnitController uc;

    final int MSG_TYPE_ENEMY_BASE = 0;
    final int MSG_TYPE_DEER = 1;
    final int MSG_TYPE_ALLIED_SETTLEMENT = 2;

    public Communications(UnitController uc) {
        this.uc = uc;
    }

    int encrypt(int x){
        x ^= XOR_NUMBER;
        int y = 0;
        for (int i = 0; i < 32; i++){
            y += (((x & (1 << i)) >> i) << SHUFFLE_NUMBER[i]);
        }
        return y;
    }

    int decrypt(int y){
        int x = 0;
        for (int i = 0; i < 32; i++){
            int sy = SHUFFLE_NUMBER[i];
            x += (((y & (1 << sy)) >> sy) << i);
        }
        return x^XOR_NUMBER;
    }

    boolean validate(int x){
        return (x >> (TYPE_BITS + INFO_BITS)) == (uc.getRound() % (1 << (VALIDATION_BITS+1))) / 2;
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

    int getType(int x){
        return ((x << VALIDATION_BITS) >> (VALIDATION_BITS + INFO_BITS));
    }

    int getInfo(int x){
        return (x << (VALIDATION_BITS + TYPE_BITS)) >> (VALIDATION_BITS + TYPE_BITS);
    }

    boolean signalLocation(int type, Location loc){
        return sendMessage(type, locationToInt(loc));
    }

    int locationToInt(Location loc){
        return (loc.x << 11) + loc.y;
    }

    Location intToLocation(int x){
        return new Location((x << 10) >> 21, (x << 21) >> 21);
    }
}
