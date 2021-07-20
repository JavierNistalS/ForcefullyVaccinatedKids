package fullEconomy;

import aic2021.user.*;

public class Exploration
{
    Exploration(UnitController uc, Location baseLocation, int CHUNK_SIZE)
    {
        this.uc = uc;
        this.baseLocation = baseLocation;
        this.CHUNK_SIZE = CHUNK_SIZE;
    }

    UnitController uc;
    Location baseLocation;
    int CHUNK_SIZE;
    boolean[][] exploredChunks = new boolean[(99 + CHUNK_SIZE) / CHUNK_SIZE][(99 + CHUNK_SIZE) / CHUNK_SIZE];
    UnitInfo[] enemyUnits;

    int targetChunkX, targetChunkY;
    int targetRound = -100;
    boolean foundBase = false;
    boolean foundBuilding = false;

    boolean outOfBoundsUp = true, outOfBoundsDown = true, outOfBoundsLeft = true, outOfBoundsRight = true;

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
        if(outOfBoundsUp && uc.isOutOfMap(loc.add(0, TEST_LEN))) { // UP
            for(int x = 0; x < SIZE; x++)
                for(int y = cy + 1; y < SIZE; y++)
                    exploredChunks[x][y] = true;
        }
        if(outOfBoundsDown && uc.isOutOfMap(loc.add(0, -TEST_LEN))) { // DOWN
            uc.println("down OOB");
            for(int x = 0; x < SIZE; x++)
                for(int y = cy - 1; y >= 0; y--)
                    exploredChunks[x][y] = true;
        }
        if(outOfBoundsRight && uc.isOutOfMap(loc.add(TEST_LEN, 0))) { // RIGHT
            uc.println("right OOB");
            for(int x = cx + 1; x < SIZE; x++)
                for(int y = 0; y < SIZE; y++)
                    exploredChunks[x][y] = true;
        }
        if(outOfBoundsLeft && uc.isOutOfMap(loc.add(-TEST_LEN, 0))) { // LEFT
            uc.println("left OOB");
            for(int x = cx - 1; x >= 0; x--)
                for(int y = 0; y < SIZE; y++)
                    exploredChunks[x][y] = true;
        }
    }

    boolean setExploreTarget()
    {
        // TODO: adjacent exploration

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
}
