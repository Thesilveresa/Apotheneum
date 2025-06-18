
package apotheneum.thesilveresa;

import apotheneum.ApotheneumPattern;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponentName;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;

@LXCategory("Apotheneum/thesilveresa")
@LXComponentName("Quasicrystal Forest")
public class QuasicrystalForest extends ApotheneumPattern {

  final CompoundParameter density =
    new CompoundParameter("Density", 2.5, 0.5, 5.0)
    .setDescription("Density of radial interference");

  final CompoundParameter spin =
    new CompoundParameter("Spin", 0.2, 0, 2.0)
    .setDescription("Angular rotation rate");

  final CompoundParameter scale =
    new CompoundParameter("Scale", 1.0, 0.5, 3.0)
    .setDescription("Size of interference pattern");

  final CompoundParameter contrast =
    new CompoundParameter("Contrast", 1.0, 0.2, 2.0)
    .setDescription("Contrast of the pattern");

  public QuasicrystalForest(LX lx) {
    super(lx);
    addParameter("density", this.density);
    addParameter("spin", this.spin);
    addParameter("scale", this.scale);
    addParameter("contrast", this.contrast);
  }

  @Override
  public void render(double deltaMs) {
    float t = (float)(lx.engine.nowMillis / 1000.);
    float d = density.getValuef();
    float s = scale.getValuef();
    float spinPhase = spin.getValuef() * t;
    float c = contrast.getValuef();

    for (LXPoint p : model.points) {
      float x = p.xn - 0.5f;
      float y = p.yn - 0.5f;

      float r = (float)Math.sqrt(x * x + y * y) * s;
      float theta = (float)Math.atan2(y, x);

      float radialComponent = (float)Math.sin(d * r);
      float angularComponent = (float)Math.sin(d * theta + spinPhase);

      float combined = 0.5f * (radialComponent + angularComponent);

      float val = (float)Math.tanh(combined * c) * 0.5f + 0.5f;

      colors[p.index] = LXColor.gray((int)(255 * val));
    }
  }
}
