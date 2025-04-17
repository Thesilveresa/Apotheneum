package apotheneum.examples;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.pattern.LXPattern;
import heronarts.lx.transform.LXParameterizedMatrix;
import heronarts.lx.utils.LXUtils;

@LXCategory("Apotheneum/Examples")
public class StripePattern extends LXPattern {

  public final CompoundParameter yaw =
    new CompoundParameter("Yaw", 0, 360)
    .setUnits(CompoundParameter.Units.DEGREES)
    .setWrappable(true)
    .setDescription("Yaw of the stripe");

  public final CompoundParameter pitch =
    new CompoundParameter("Pitch", 0, 360)
    .setUnits(CompoundParameter.Units.DEGREES)
    .setWrappable(true)
    .setDescription("Pitch of the stripe");

  public final CompoundParameter roll =
    new CompoundParameter("Roll", 0, 360)
    .setUnits(CompoundParameter.Units.DEGREES)
    .setWrappable(true)
    .setDescription("Roll of the stripe");

  public final CompoundParameter center =
    new CompoundParameter("Center", .5)
    .setPolarity(CompoundParameter.Polarity.BIPOLAR)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Center of the stripe");

  public final CompoundParameter width =
    new CompoundParameter("Width", .5)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Width of the stripe");

  private final LXParameterizedMatrix transform = new LXParameterizedMatrix();

  public StripePattern(LX lx) {
    super(lx);
    addParameter("yaw", this.yaw);
    addParameter("pitch", this.pitch);
    addParameter("roll", this.roll);
    addParameter("center", this.center);
    addParameter("width", this.width);

    this.transform.addParameter(this.yaw);
    this.transform.addParameter(this.pitch);
    this.transform.addParameter(this.roll);
  }

  @Override
  protected void run(double deltaMs) {
    this.transform.update(matrix -> {
      matrix
        .translate(.5f, .5f, .5f)
        .rotateZ((float) Math.toRadians(-this.roll.getValue()))
        .rotateX((float) Math.toRadians(-this.pitch.getValue()))
        .rotateY((float) Math.toRadians(-this.yaw.getValue()))
        .translate(-.5f, -.5f, -.5f);
    });

    final float center = this.center.getValuef();
    final float falloff = 1 / this.width.getValuef();

    for (LXPoint p : model.points) {
      float yn = this.transform.yn(p);
      colors[p.index] = LXColor.grayn(LXUtils.max(0, 1 - Math.abs(yn - center) * falloff));
    }
  }

}
