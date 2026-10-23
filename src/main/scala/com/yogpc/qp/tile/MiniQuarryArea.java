package com.yogpc.qp.tile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/** Bounded top-down scan; each layer is visited twice to catch falling blocks. */
public record MiniQuarryArea(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public MiniQuarryArea {
        if (minX > maxX || minZ > maxZ || minY < 1 || maxY > 255 || minY > maxY
                || (long) maxX - minX >= 256 || (long) maxZ - minZ >= 256)
            throw new IllegalArgumentException("Invalid Mini Quarry area");
    }

    public long size() { return ((long) maxX - minX + 1) * ((long) maxZ - minZ + 1) * (maxY - minY + 1) * 2; }

    public BlockPos at(long index) {
        if (index < 0 || index >= size()) throw new IndexOutOfBoundsException(Long.toString(index));
        long width = (long) maxX - minX + 1, depth = (long) maxZ - minZ + 1;
        long layer = width * depth;
        long horizontal = index % layer;
        return new BlockPos(minX + (int) (horizontal % width), maxY - (int) (index / (2 * layer)), minZ + (int) (horizontal / width));
    }

    public AxisAlignedBB box() { return new AxisAlignedBB(minX, minY, minZ, (double) maxX + 1, (double) maxY + 1, (double) maxZ + 1); }

    public NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("min", new BlockPos(minX, minY, minZ).toLong());
        tag.setLong("max", new BlockPos(maxX, maxY, maxZ).toLong());
        return tag;
    }

    public static MiniQuarryArea read(NBTTagCompound tag) {
        if (!tag.hasKey("min") || !tag.hasKey("max")) return null;
        BlockPos min = BlockPos.fromLong(tag.getLong("min")), max = BlockPos.fromLong(tag.getLong("max"));
        try { return new MiniQuarryArea(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ()); }
        catch (IllegalArgumentException invalid) { return null; }
    }
}
