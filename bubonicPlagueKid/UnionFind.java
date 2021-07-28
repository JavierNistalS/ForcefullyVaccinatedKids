package bubonicPlagueKid;

import aic2021.user.*;

public class UnionFind {
    final int SIZE = 103;
    final int OFFSET = 51;
    final int ROOT = -1;
    final int UNEXPLORED = 0;
    final int OBSTACLE = -2;
    int[] parent = new int[SIZE*SIZE];
    int[] size = new int[SIZE*SIZE];
    int[] unknownConnections = new int[SIZE*SIZE];
    Location spawn;
    int quotientCardinal = 0;
    Direction[] dirs = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
    int[] dirsIdx;
    UnitController uc;

    public UnionFind(UnitController uc, boolean diagonal){
        this.uc = uc;
        this.spawn = uc.getLocation();
        if (!diagonal)
            dirs = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        dirsIdx = new int[dirs.length];
        for (int i = 0; i < dirs.length; i++){
            dirsIdx[i] = dirs[i].dx*SIZE + dirs[i].dy;
        }
    }

    private int find(Location loc){
        return find(locToIdx(loc));
    }

    private int find(int idx){
        while (parent[idx] > 0){
            parent[idx] = parent[parent[idx]];
        }
        return (parent[idx] <= 0) ? idx : (parent[idx] = find(parent[idx]));
    }

    private void union(int A, int B){
        A = find(A);
        B = find(B);
        if (A != B) {
            uc.drawLineDebug(idxToLoc(A), idxToLoc(B), 0,0,0);
            size[A] += size[B];
            unknownConnections[A] += unknownConnections[B];
            parent[B] = A;
            quotientCardinal--;
        }
    }

    private void union(Location A, Location B){
        union(locToIdx(A), locToIdx(B));
    }

    public void recognize(Location loc){
        int idx = locToIdx(loc);
        if (parent[idx] == UNEXPLORED) {
            uc.drawPointDebug(loc, 0,0,0);
            parent[idx] = OBSTACLE;
            for (int dirIdx : dirsIdx) {
                int nIdx = idx + dirIdx;
                if (parent[nIdx] != OBSTACLE && parent[nIdx] != UNEXPLORED) {
                    unknownConnections[find(nIdx)]--;
                }
            }
        }
    }

    public void add(Location loc){
        int idx = locToIdx(loc);
        if (parent[idx] == UNEXPLORED) {
            uc.drawPointDebug(loc, 255, 0, 255);
            parent[idx] = ROOT;
            size[idx] = 1;
            quotientCardinal++;

            for (int dirIdx : dirsIdx) {
                int energy0 = uc.getEnergyLeft();
                int nIdx = idx + dirIdx;
                if (parent[nIdx] == UNEXPLORED) {
                    uc.println("cheap");
                    unknownConnections[find(idx)]++;
                }
                else if (parent[nIdx] != OBSTACLE) {
                    uc.println("union");
                    union(idx, nIdx);
                    unknownConnections[find(nIdx)]--;
                }
                int energy1 = uc.getEnergyLeft();
                uc.println(energy0 - energy1);
            }
            uc.println("");

        }
    }

    private boolean isIsolated(int idx){
        return getUnknownConnections(idx) == 0;
    }

    public boolean isIsolated(Location loc){
        return isIsolated(locToIdx(loc));
    }

    private int getUnknownConnections(int idx) {
        return unknownConnections[find(idx)];
    }

    private int getUnknownConnections(Location loc){
        return getUnknownConnections(locToIdx(loc));
    }

    private int getSize(int idx){
        return size[find(idx)];
    }

    public int getSize(Location loc){
        return getSize(locToIdx(loc));
    }

    public boolean areConnected(Location A, Location B){
        return find(A) == find(B);
    }

    private int locToIdx(Location loc){
        return (loc.x - spawn.x + OFFSET)*SIZE + (loc.y - spawn.y + OFFSET) + 1;
    }

    private Location idxToLoc(int idx){
        return new Location(spawn.x + (idx-1)/SIZE - OFFSET, spawn.y + (idx-1)%SIZE - OFFSET);
    }
}
