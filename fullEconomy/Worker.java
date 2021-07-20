package fullEconomy;

import aic2021.user.*;

public class Worker extends MyUnit {

    Worker(UnitController uc){
        super(uc);
        pathfinding = new Pathfinding(uc);
    }

    boolean torchLighted = false;
    Location baseLocation;
    Pathfinding pathfinding;

    int farmCount, quarryCount, sawmillCount;
    boolean orbitDirection = false;
    int lastValid = 0;

    Pathfinding pathfinding;

    int CHUNK_SIZE = 5;
    boolean[][] exploredChunks = new boolean[(99 + CHUNK_SIZE) / CHUNK_SIZE][(99 + CHUNK_SIZE) / CHUNK_SIZE];
    UnitInfo[] enemyUnits;

    int targetChunkX, targetChunkY;
    int targetRound = -100;
    boolean foundBase = false;
    boolean foundBuilding = false;

    boolean outOfBoundsUp = true, outOfBoundsDown = true, outOfBoundsLeft = true, outOfBoundsRight = true;

    void playRound()
    {
        UnitInfo[] units = uc.senseUnits(uc.getTeam());
        for(UnitInfo unit : units)
            if(unit.getType() == UnitType.BASE)
                baseLocation = unit.getLocation();


        if(uc.canMove()) {

            UnitInfo[] enemyUnits = uc.senseUnits(uc.getTeam().getOpponent());
            if(enemyUnits.length > 0)
                pathfinding.pathfindTo(baseLocation);

            if(baseLocation == null || uc.getLocation().distanceSquared(baseLocation) < 64) {
                if (isValid(uc.getLocation()))
                    moveRandomDiagonal();
                if (uc.canMove())
                    moveRandom();
            }
            else
                pathfinding.pathfindTo(baseLocation);
        }

        if((sawmillCount < 4 || (uc.getResource(Resource.STONE) > 1100 && uc.getResource(Resource.FOOD) > 1100)) && trySpawnInValid(UnitType.SAWMILL))
            sawmillCount++;
        if((farmCount < 3 || (uc.getResource(Resource.STONE) > 1100 && uc.getResource(Resource.WOOD) > 1100)) && trySpawnInValid(UnitType.FARM))
            farmCount++;
        if((quarryCount < 3 || (uc.getResource(Resource.FOOD) > 1100 && uc.getResource(Resource.WOOD) > 1100)) && trySpawnInValid(UnitType.QUARRY))
            quarryCount++;
    }

    boolean isValid(Location loc)
    {
        return ((loc.x + loc.y) % 2) == 0;
        //return baseLocation != null && loc.distanceSquared (baseLocation) > 1 || (uc.getRound() > 400 && lastValid + 3< uc.getRound());
    }

    boolean trySpawnInValid(UnitType type)
    {
        for (Direction dir : dirs){
            if (isValid(uc.getLocation().add(dir)) && trySpawnUnit(type, dir))
            {
                lastValid = uc.getRound();
                return true;
            }

        }
        return false;
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
