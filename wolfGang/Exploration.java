package wolfGang;

import aic2021.user.*;

public class Exploration
{
    // TODO: try to make specialized worker exploration

    Exploration(UnitController uc, int CHUNK_SIZE, int MOVEMENT_MULT)
    {
        this.uc = uc;
        this.spawnLocation = uc.getLocation();
        this.CHUNK_SIZE = CHUNK_SIZE;
        this.MOVEMENT_MULT = MOVEMENT_MULT;
        SIZE = (99 + 2 * CHUNK_SIZE) / CHUNK_SIZE; // ceil(100 / CHUNK_SIZE) + 1
        exploredChunks = new boolean[SIZE][SIZE];
    }
    UnitController uc;
    Location spawnLocation;
    int CHUNK_SIZE;
    int SIZE;
    int MOVEMENT_MULT;
    boolean[][] exploredChunks;
    UnitInfo[] enemyUnits;

    int targetChunkX, targetChunkY;
    int targetRound = -100;
    boolean foundBase = false;
    boolean foundBuilding = false;

    boolean outOfBoundsUp = true, outOfBoundsDown = true, outOfBoundsLeft = true, outOfBoundsRight = true;

    void updateChunks() {
        // Discard current chunk
        Location loc = uc.getLocation();
        int cx = (loc.x - spawnLocation.x + 50) / CHUNK_SIZE;
        int cy = (loc.y - spawnLocation.y + 50) / CHUNK_SIZE;
        exploredChunks[cx][cy] = true;
        uc.println("explored: [" + cx + ", " + cy + "]");

        int SIZE = (99 + 2 * CHUNK_SIZE) / CHUNK_SIZE;
        int TEST_LEN = CHUNK_SIZE - 1;

        // discard Out-Of-Bounds Chunks
        if(outOfBoundsUp && uc.isOutOfMap(loc.add(0, TEST_LEN))) { // UP
            outOfBoundsUp = false;
            uc.println("up OOB");
            for(int x = 0; x < SIZE; x++)
                for(int y = cy + 1; y < SIZE; y++)
                    exploredChunks[x][y] = true;
        }
        if(outOfBoundsDown && uc.isOutOfMap(loc.add(0, -TEST_LEN))) { // DOWN
            outOfBoundsDown = false;
            uc.println("down OOB");
            for(int x = 0; x < SIZE; x++)
                for(int y = cy - 1; y >= 0; y--)
                    exploredChunks[x][y] = true;
        }
        if(outOfBoundsRight && uc.isOutOfMap(loc.add(TEST_LEN, 0))) { // RIGHT
            outOfBoundsRight = false;
            uc.println("right OOB");
            for(int x = cx + 1; x < SIZE; x++)
                for(int y = 0; y < SIZE; y++)
                    exploredChunks[x][y] = true;
        }
        if(outOfBoundsLeft && uc.isOutOfMap(loc.add(-TEST_LEN, 0))) { // LEFT
            outOfBoundsLeft = false;
            uc.println("left OOB");
            for(int x = cx - 1; x >= 0; x--)
                for(int y = 0; y < SIZE; y++)
                    exploredChunks[x][y] = true;
        }
    }

    Location getLocation() {
        if (setExploreTarget())
            return chunkToLocation(targetChunkX, targetChunkY);
        else
            return null;
    }

    boolean setExploreTarget() {
        int SIZE = 100 / CHUNK_SIZE;
        int c = 0;
        while((exploredChunks[targetChunkX][targetChunkY] && c < 100) || uc.getRound() >= targetRound) {
            targetChunkX = (int)(SIZE * uc.getRandomDouble());
            targetChunkY = (int)(SIZE * uc.getRandomDouble());
            setTargetRound();
            c++;
        }
        uc.println("target (random): [" + targetChunkX + ", " + targetChunkY + "] = " + exploredChunks[targetChunkX][targetChunkY]);
        return !exploredChunks[targetChunkX][targetChunkY];
    }

    void setTargetRound(){
        targetRound = uc.getRound() + MOVEMENT_MULT * (int)(Math.sqrt(uc.getLocation().distanceSquared(chunkToLocation(targetChunkX, targetChunkY))) + 1f);
    }

    Location chunkToLocation(int cx, int cy) {
        return new Location(
            spawnLocation.x - 50 + cx * CHUNK_SIZE,
            spawnLocation.y - 50 + cy * CHUNK_SIZE);
    }

    boolean willReset() {
        return uc.getRound() + Math.sqrt(getLocation().distanceSquared(uc.getLocation()))*uc.getType().movementCooldown >= targetRound;
    }
}
