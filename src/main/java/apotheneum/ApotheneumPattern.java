package apotheneum;

import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXModel;
import heronarts.lx.pattern.LXPattern;

public abstract class ApotheneumPattern extends LXPattern {

  protected ApotheneumPattern(LX lx) {
    super(lx);
    Apotheneum.initialize(lx);
  }

  @Override
  protected final void run(double deltaMs) {
    if (Apotheneum.exists) {
      render(deltaMs);
    } else {
      setColors(LXColor.BLACK);
    }
  }

  private void assertExists() {
    if (!Apotheneum.exists) {
      throw new IllegalStateException("Should not call ApothenumPattern utilities when no Apotheneum model loaded");
    }
  }

  protected void copyCubeExterior() {
    copy(Apotheneum.cube.exterior, Apotheneum.cube.interior);
  }

  protected void copyCylinderExterior() {
    copy(Apotheneum.cylinder.exterior, Apotheneum.cylinder.interior);
  }

  protected void copyExterior() {
    copyCubeExterior();
    copyCylinderExterior();
  }

  protected void copyMirror(Apotheneum.Cube.Face from, Apotheneum.Cube.Face to) {
    assertExists();
    if ((from != null) && (to != null)) {
      int colIndex = 0;
      for (LXModel fromCol : from.columns) {
        LXModel toCol = to.columns[to.columns.length - 1 - colIndex];
        System.arraycopy(colors, fromCol.points[0].index, colors, toCol.points[0].index, fromCol.size);
        ++colIndex;
      }
    }
  }

  protected void copy(Apotheneum.Cube.Face from, Apotheneum.Cube.Face to) {
    assertExists();
    if ((from != null) && (to != null)) {
      System.arraycopy(colors, from.model.points[0].index, colors, to.model.points[0].index, from.model.size);
    }
  }

  protected void copy(Apotheneum.Cube.Orientation from, Apotheneum.Cube.Orientation to) {
    assertExists();
    if ((from != null) && (to != null)) {
      System.arraycopy(colors, from.front.model.points[0].index, colors, to.front.model.points[0].index, from.size);
    }
  }

  protected void copy(Apotheneum.Cylinder.Orientation from, Apotheneum.Cylinder.Orientation to) {
    assertExists();
    if ((from != null) && (to != null)) {
      System.arraycopy(colors, from.columns[0].points[0].index, colors, to.columns[0].points[0].index, from.size);
    }
  }

  protected abstract void render(double deltaMs);

}
