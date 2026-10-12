package com.sumirelabs.quarryplus.gui.mui;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.yogpc.qp.QuarryPlusI;
import com.yogpc.qp.gui.TranslationKeys;
import com.yogpc.qp.tile.TileAdvPump;
import com.yogpc.qp.tile.TileAdvQuarry;
import com.yogpc.qp.tile.TileFiller;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.fluids.FluidStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Native ModularUI controls, with machine mutations performed on the server. */
public final class MachinePanels {
    private MachinePanels() {}

    public static ModularPanel build(MuiContext c) {
        int id = c.data.guiId;
        if (id == QuarryPlusI.guiIdAdvQuarry()) return quarry(c);
        if (id == QuarryPlusI.guiIdAdvPump()) return pump(c);
        if (id == QuarryPlusI.guiIdFiller()) return filler(c);
        if (id >= 120 && id < 126) return fluids(c, EnumFacing.byIndex(id - 120));
        return null;
    }

    private static void consume(PacketBuffer b) {
        b.readInt(); b.readBoolean(); b.readBoolean();
    }

    private static void modules(MuiContext c) {
        QuarryGuiFactory.open(c.player, QuarryPlusI.guiIdQuarryModule(), c.tile.getWorld(), c.tile.getPos());
    }

    private static ModularPanel quarry(MuiContext c) {
        TileAdvQuarry q = (TileAdvQuarry) c.tile;
        ModularPanel p = MuiWidgets.panel(c, TranslationKeys.advquarry, 176, 166);
        int[][] positions = {{98,16},{68,16},{98,62},{68,62},{38,39},{8,39},{158,39},{128,39}};
        for (int i = 0; i < positions.length; i++) {
            final int button = i;
            MuiWidgets.actionButton(c,p,"range"+i,positions[i][0],positions[i][1],10,10,
                IKey.str(i % 2 == 0 ? "+" : "-"), b -> {
                    b.readInt(); boolean shift = b.readBoolean(), ctrl = b.readBoolean();
                    if (!q.mode().is(TileAdvQuarry.NOT_NEED_BREAK()) || !q.digRange().defined()) return;
                    int delta = (shift && ctrl ? 1024 : shift ? 256 : ctrl ? 64 : 16) * (button % 2 == 0 ? 1 : -1);
                    NBTTagCompound tag = q.digRange().toNBT();
                    EnumFacing f = EnumFacing.byIndex(button / 2 + 2);
                    String min = f.getAxis() == EnumFacing.Axis.X ? "minX" : "minZ";
                    String max = f.getAxis() == EnumFacing.Axis.X ? "maxX" : "maxZ";
                    String key = f.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE ? max : min;
                    int value = tag.getInteger(key) + (key.equals(max) ? delta : -delta);
                    if (key.equals(max) ? tag.getInteger(min) < value : tag.getInteger(max) > value) {
                        tag.setInteger(key, value);
                        q.digRange_$eq(TileAdvQuarry.DigRange$.MODULE$.readFromNBT(tag));
                        q.markDirty();
                    }
                });
        }
        int[][] labels = {{79,17},{79,63},{19,40},{139,40}};
        for (int i=0;i<4;i++) {
            final int side=i;
            MuiWidgets.text(c,p,"extent"+i,labels[i][0],labels[i][1],20,()-> {
                if (!q.digRange().defined()) return "";
                ChunkPos chunk = new ChunkPos(q.getPos());
                double blocks = side==0 ? chunk.getZStart()-q.digRange().minZ() : side==1 ? q.digRange().maxZ()-chunk.getZEnd() : side==2 ? chunk.getXStart()-q.digRange().minX() : q.digRange().maxX()-chunk.getXEnd();
                return Double.toString(blocks / 16);
            });
        }
        MuiWidgets.actionButton(c,p,"quick",108,58,60,12,IKey.str("No Frame"),b->{consume(b);if(q.mode().is(TileAdvQuarry.NOT_NEED_BREAK())) q.noFrameStart();});
        MuiWidgets.actionButton(c,p,"modules",8,58,60,12,IKey.str("Modules"),b->{consume(b);modules(c);});
        p.child(IKey.lang(TranslationKeys.CONTAINER_INVENTORY).asWidget().pos(8,72));
        MuiWidgets.slots(c,p);
        return p;
    }

    private static ModularPanel pump(MuiContext c) {
        TileAdvPump q = (TileAdvPump)c.tile;
        ModularPanel p = MuiWidgets.panel(c,TranslationKeys.advpump,176,166);
        MuiWidgets.actionButton(c,p,"frame",8,22,80,20,IKey.EMPTY,b->{consume(b);q.placeFrame_$eq(!q.placeFrame());q.markDirty();});
        MuiWidgets.text(c,p,"frameLabel",10,28,76,()->"Frame "+(q.placeFrame()?"on":"off"));
        MuiWidgets.actionButton(c,p,"delete",88,22,80,20,IKey.EMPTY,b->{consume(b);q.toggleDelete();q.markDirty();});
        MuiWidgets.text(c,p,"deleteLabel",90,28,76,()->"Delete "+(q.delete()?"on":"off"));
        MuiWidgets.actionButton(c,p,"start",28,50,120,20,IKey.EMPTY,b->{consume(b);if(!q.isWorking())q.start();});
        MuiWidgets.text(c,p,"startLabel",30,56,116,()->q.isWorking()?"Working":"Start");
        p.child(IKey.lang(TranslationKeys.CONTAINER_INVENTORY).asWidget().pos(8,72));
        MuiWidgets.slots(c,p);
        return p;
    }

    private static ModularPanel filler(MuiContext c) {
        TileFiller q = (TileFiller)c.tile;
        ModularPanel p = MuiWidgets.panel(c,TranslationKeys.filler,176,222);
        MuiWidgets.actionButton(c,p,"modules",110,125,60,14,IKey.str("Modules"),b->{consume(b);modules(c);});
        MuiWidgets.actionButton(c,p,"all",108,18,60,20,IKey.str("FillAll"),b->{consume(b);q.startFillAll();});
        MuiWidgets.actionButton(c,p,"box",108,38,60,20,IKey.str("FillBox"),b->{consume(b);q.startFillBox();});
        p.child(IKey.lang(TranslationKeys.CONTAINER_INVENTORY).asWidget().pos(8,128));
        MuiWidgets.slots(c,p);
        return p;
    }

    private static List<FluidStack> fluidList(TileAdvQuarry q, EnumFacing facing, boolean adding) {
        List<FluidStack> result = new ArrayList<>();
        scala.collection.Iterator<FluidStack> it = adding ? q.fluidStacks().keysIterator() : q.fluidExtractFacings().apply(facing).iterator();
        while(it.hasNext()) {
            FluidStack fluid = it.next();
            if (!adding || !q.fluidExtractFacings().apply(facing).contains(fluid)) result.add(fluid);
        }
        result.sort(Comparator.comparing(f -> f.writeToNBT(new NBTTagCompound()).toString()));
        return result;
    }

    /** The selector is a second page in the same synced panel; pages support any list length. */
    private static ModularPanel fluids(MuiContext c, EnumFacing facing) {
        TileAdvQuarry q=(TileAdvQuarry)c.tile;
        ModularPanel p=MuiWidgets.panel(c,"FD."+facing.toString().toLowerCase(java.util.Locale.ROOT),320,232);
        AtomicBoolean adding=new AtomicBoolean();
        AtomicInteger page=new AtomicInteger();
        FluidStack[] selected={null};
        MuiWidgets.text(c,p,"page",8,20,304,()->(adding.get()?"Select fluid":"Fluid filter")+"  "+(page.get()+1));
        for(int row=0;row<8;row++) {
            final int index=row;
            MuiWidgets.actionButton(c,p,"fluid"+row,8,36+row*18,304,18,IKey.EMPTY,b->{
                consume(b);List<FluidStack> list=fluidList(q,facing,adding.get());int at=page.get()*8+index;
                if(at<list.size())selected[0]=list.get(at);
            });
            MuiWidgets.text(c,p,"fluidName"+row,12,41+row*18,296,()->{
                List<FluidStack> list=fluidList(q,facing,adding.get());int at=page.get()*8+index;
                if(at>=list.size())return "";
                FluidStack f=list.get(at);return (f.equals(selected[0])?"> ":"")+f.getLocalizedName();
            });
        }
        MuiWidgets.actionButton(c,p,"previous",8,183,30,18,IKey.str("<"),b->{consume(b);page.set(Math.max(0,page.get()-1));});
        MuiWidgets.actionButton(c,p,"next",42,183,30,18,IKey.str(">"),b->{consume(b);if((page.get()+1)*8<fluidList(q,facing,adding.get()).size())page.incrementAndGet();});
        MuiWidgets.button(c,p,"add",78,183,110,18,TranslationKeys.FROM_LIST,b->{consume(b);adding.set(true);page.set(0);selected[0]=null;});
        MuiWidgets.button(c,p,"remove",194,183,118,18,TranslationKeys.DELETE,b->{consume(b);if(!adding.get()&&selected[0]!=null){q.fluidExtractFacings().apply(facing).remove(selected[0]);q.markDirty();selected[0]=null;}});
        MuiWidgets.button(c,p,"done",8,207,148,18,TranslationKeys.DONE,b->{
            consume(b);
            if(adding.get()){
                if(selected[0]!=null&&fluidList(q,facing,true).contains(selected[0])){FluidStack f=selected[0].copy();f.amount=1000;q.fluidExtractFacings().apply(facing).add(f);q.markDirty();}
                adding.set(false);page.set(0);selected[0]=null;
            }else c.player.closeScreen();
        });
        MuiWidgets.button(c,p,"cancel",164,207,148,18,TranslationKeys.CANCEL,b->{consume(b);adding.set(false);page.set(0);selected[0]=null;});
        return p;
    }
}
