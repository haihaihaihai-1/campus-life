package common.cn.kafei.simukraft.path;

import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Map;

public record PathSnapshot(ResourceLocation dimensionId, BlockPos startPos, BlockPos targetPos, Map<Long, PathCell> cells,
                           LongSet bodyPassages, int minY, int maxY, long createdAt, boolean complete) {
    public PathSnapshot(ResourceLocation dimensionId, BlockPos startPos, BlockPos targetPos, Map<Long, PathCell> cells,
                        int minY, int maxY, long createdAt, boolean complete) {
        this(dimensionId, startPos, targetPos, cells, LongSets.EMPTY_SET, minY, maxY, createdAt, complete);
    }

    public PathCell cell(BlockPos pos) {
        return cells.get(PathCell.key(pos));
    }

    public PathCell cell(int x, int y, int z) {
        return cells.get(PathCell.key(x, y, z));
    }

    public Collection<PathCell> allCells() {
        return cells.values();
    }

    public boolean contains(BlockPos pos) {
        return cells.containsKey(PathCell.key(pos));
    }

    public boolean bodyPassage(int x, int y, int z) {
        return bodyPassages.contains(BlockPos.asLong(x, y, z));
    }
}
