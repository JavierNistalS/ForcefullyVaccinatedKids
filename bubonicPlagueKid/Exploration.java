package bubonicPlagueKid;

import aic2021.user.*;

public class Exploration
{
    // TODO: try to make specialized worker exploration

    Exploration(UnitController uc, int CHUNK_SIZE, int RESET_TURNS)
    {
        this.uc = uc;
        this.spawnLocation = uc.getLocation();
        this.CHUNK_SIZE = CHUNK_SIZE;
        this.RESET_TURNS = RESET_TURNS;
        SIZE = (99 + 2 * CHUNK_SIZE) / CHUNK_SIZE; // ceil(100 / CHUNK_SIZE) + 1
        exploredChunks = new boolean[SIZE][SIZE];
    }
    UnitController uc;
    Location spawnLocation;
    int CHUNK_SIZE;
    int SIZE;
    int RESET_TURNS;
    boolean[][] exploredChunks;
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

    Location getLocation()
    {
        if (setExploreTarget())
            return new Location(
                spawnLocation.x - 50 + targetChunkX * CHUNK_SIZE,
                spawnLocation.y - 50 + targetChunkY * CHUNK_SIZE);
        else
            return null;
    }

    boolean setExploreTarget() {
        if(exploredChunks[targetChunkX][targetChunkY]) {
            Location loc = uc.getLocation();
            int cx = (loc.x - spawnLocation.x + 50) / CHUNK_SIZE;
            int cy = (loc.y - spawnLocation.y + 50) / CHUNK_SIZE;

            // vv Cardinal directions are repeated so that they are twice as common
            Direction[] randomDirs = {Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST, Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST, Direction.NORTHEAST, Direction.NORTHWEST, Direction.SOUTHEAST, Direction.SOUTHWEST};
            for(int i = randomDirs.length - 1; i >= 1; i--) { // SHUFFLE DIRS
                int j = (int)(uc.getRandomDouble() * i + 1);
                Direction temp = randomDirs[i];
                randomDirs[i] = randomDirs[j];
                randomDirs[j] = temp;
            }

            for(Direction dir : randomDirs) {
                int tx = cx + dir.dx;
                int ty = cy + dir.dy;

                if(!exploredChunks[tx][ty])
                {
                    targetChunkX = tx;
                    targetChunkY = ty;
                    uc.println("target (close): [" + targetChunkX + ", " + targetChunkY + "] = " + exploredChunks[targetChunkX][targetChunkY] + "{dir = " + dir.toString() + "}");
                    return true;
                }
            }
        }

        int SIZE = 100 / CHUNK_SIZE;
        int c = 0;
        while((exploredChunks[targetChunkX][targetChunkY] && c < 100) || uc.getRound() >= targetRound + RESET_TURNS)
        {
            targetChunkX = (int)(SIZE * uc.getRandomDouble());
            targetChunkY = (int)(SIZE * uc.getRandomDouble());
            targetRound = uc.getRound();
            c++;
        }
        uc.println("target (random): [" + targetChunkX + ", " + targetChunkY + "] = " + exploredChunks[targetChunkX][targetChunkY]);
        return !exploredChunks[targetChunkX][targetChunkY];
    }

    void setRandomExploreTarget() {
        int SIZE = 100 / CHUNK_SIZE;
        int c = 0;
        while((exploredChunks[targetChunkX][targetChunkY] && c < 100) || uc.getRound() >= targetRound + RESET_TURNS)
        {
            targetChunkX = (int)(SIZE * uc.getRandomDouble());
            targetChunkY = (int)(SIZE * uc.getRandomDouble());
            targetRound = uc.getRound();
            c++;
        }
        uc.println("randomTarget: [" + targetChunkX + ", " + targetChunkY + "] = " + exploredChunks[targetChunkX][targetChunkY]);

    }

    boolean willReset(){
        return uc.getRound() >= targetRound + RESET_TURNS;
    }
}
