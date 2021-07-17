package typhusKid;

import aic2021.user.*;

public class Explorer extends MyUnit {

    Explorer(UnitController uc){
        super(uc);
        pathfinding = new Pathfinding(uc);

        for(int i = 0; i < 100 / CHUNK_SIZE; i++)
            for(int j = 0; j < 100 / CHUNK_SIZE; j++)
                exploredChunks[i][j] = false;
    }

    Pathfinding pathfinding;

    int CHUNK_SIZE = 6;
    boolean[][] exploredChunks = new boolean[(99 + CHUNK_SIZE) / CHUNK_SIZE][(99 + CHUNK_SIZE) / CHUNK_SIZE];
    UnitInfo[] enemyUnits;

    int targetChunkX, targetChunkY;
    int targetRound = -100;
    boolean foundBase = false;
    boolean foundBuilding = false;

    void playRound()
    {
        // try find allied base
        if(baseLocation == null)
        {
            UnitInfo[] alliedUnits = uc.senseUnits(uc.getTeam());
            for (UnitInfo u : alliedUnits)
                if (u.getType() == UnitType.BASE)
                    baseLocation = u.getLocation();
        }

        enemyUnits = uc.senseUnits(uc.getTeam().getOpponent());

        updateChunks();

        // find enemy base (TODO in micro)
        boolean anyAggro = false;
        if(enemyBaseLocation == null)
            for (UnitInfo u : enemyUnits) {
                if (u.getType() == UnitType.BASE)
                    enemyBaseLocation = u.getLocation();
                else if(u.getType() == UnitType.FARM || u.getType() == UnitType.QUARRY || u.getType() == UnitType.QUARRY)
                    enemyStructureLocation = u.getLocation();
                else if(u.getType() == UnitType.WORKER || u.getType() == UnitType.AXEMAN || u.getType() == UnitType.SPEARMAN || u.getType() == UnitType.WOLF)
                    anyAggro = true;
            }
        if (!foundBase && enemyBaseLocation != null){
            foundBase = SendEnemyBaseSignal(enemyBaseLocation);
        }
        else if(!foundBuilding && enemyStructureLocation != null && anyAggro)
            foundBuilding = SendEnemyStructureSignal(enemyStructureLocation);

        //if(enemyUnits.length != 0) // micro
        //micro()
        //else
        if (setExploreTarget())
            pathfinding.pathfindTo(new Location(
                baseLocation.x - 50 + targetChunkX * CHUNK_SIZE,
                baseLocation.y - 50 + targetChunkY * CHUNK_SIZE));
        else
            uc.println("target is invalid!");

        if (uc.getInfo().getTorchRounds() < 10)
            tryLightTorch();

    }

    void updateChunks()
    {
        // Discard current chunk
        Location loc = uc.getLocation();
        int cx = (loc.x - baseLocation.x + 50) / CHUNK_SIZE;
        int cy = (loc.y - baseLocation.y + 50) / CHUNK_SIZE;
        exploredChunks[cx][cy] = true;
        uc.println("explored: [" + cx + ", " + cy + "]");

        int SIZE = (99 + CHUNK_SIZE) / CHUNK_SIZE;
        int TEST_LEN = CHUNK_SIZE - 1;

        // discard Out-Of-Bounds Chunks
        if(uc.isOutOfMap(loc.add(0, TEST_LEN))) { // UP
            uc.println("up OOB start");
            for(int x = 0; x < SIZE; x++)
                for(int y = cy + 1; y < SIZE; y++)
                {
                    uc.println("up oob: [" + x + ", " + y + "]");
                    exploredChunks[x][y] = true;
                }
        }
        if(uc.isOutOfMap(loc.add(0, -TEST_LEN))) { // DOWN
            uc.println("down OOB");
            for(int x = 0; x < SIZE; x++)
                for(int y = cy - 1; y >= 0; y--)
                    exploredChunks[x][y] = true;
        }
        if(uc.isOutOfMap(loc.add(TEST_LEN, 0))) { // RIGHT
            uc.println("right OOB");
            for(int x = cx + 1; x < SIZE; x++)
                for(int y = 0; y < SIZE; y++)
                    exploredChunks[x][y] = true;
        }
        if(uc.isOutOfMap(loc.add(-TEST_LEN, 0))) { // LEFT
            uc.println("left OOB");
            for(int x = cx - 1; x >= 0; x--)
                for(int y = 0; y < SIZE; y++)
                    exploredChunks[x][y] = true;
        }
    }

    boolean setExploreTarget()
    {
        int SIZE = 100 / CHUNK_SIZE;
        int c = 0;
        while((exploredChunks[targetChunkX][targetChunkY] && c < 100) || uc.getRound() >= targetRound + 25)
        {
            targetChunkX = (int)(SIZE * uc.getRandomDouble());
            targetChunkY = (int)(SIZE * uc.getRandomDouble());
            uc.println("rand: [" + targetChunkX + ", " + targetChunkY + "]");
            targetRound = uc.getRound();
            c++;
        }
        uc.println("explored[" + targetChunkX + ", " + targetChunkY + "] = " + exploredChunks[targetChunkX][targetChunkY]);
        uc.println("c = " + c);
        uc.println("targetRound = " + targetRound);
        return true;
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
