package apotheneum.doved.lightning;

public class LightningSegment {
  public final double x1, y1, x2, y2;
  public final boolean isBranch;
  public final double intensity;
  public final int depth;

  public LightningSegment(double x1, double y1, double x2, double y2, boolean isBranch, double intensity) {
    this(x1, y1, x2, y2, isBranch, intensity, 0);
  }

  public LightningSegment(double x1, double y1, double x2, double y2, boolean isBranch, double intensity, int depth) {
    this.x1 = x1;
    this.y1 = y1;
    this.x2 = x2;
    this.y2 = y2;
    this.isBranch = isBranch;
    this.intensity = intensity;
    this.depth = depth;
  }

}