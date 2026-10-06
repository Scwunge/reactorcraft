package net.scwunge.reactorcraft.content.multi;

/** A block entity that sits in a multiblock structure and works only while the structure stands (the original's MultiBlockTile). */
public interface MultiController {
    MultiStructure structure();

    boolean isFormed();

    void setFormed(boolean formed);
}
