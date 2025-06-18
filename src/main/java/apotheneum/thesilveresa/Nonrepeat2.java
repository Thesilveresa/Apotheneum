package apotheneum.thesilveresa;

import apotheneum.ApotheneumPattern;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponentName;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.BooleanParameter;

@LXCategory("Apotheneum/thesilveresa")
@LXComponentName("Nonrepeat 2")
public class Nonrepeat2 extends ApotheneumPattern {

    final CompoundParameter phase =
      new CompoundParameter("Phase", 0, 0, 5)
      .setDescription("Oscillation phase offset");

    final CompoundParameter decoyStrength =
      new CompoundParameter("Decoy", 0.4, 0, 1)
      .setDescription("Strength of false symmetry flicker");

    final CompoundParameter slip =
      new CompoundParameter("Slip", 0, 0, 1)
      .setDescription("Phase slip distortion");

    final CompoundParameter flicker =
      new CompoundParameter("Flicker", 0.4, 0, 1)
      .setDescription("Flicker modulation blending");

    final CompoundParameter grain =
      new CompoundParameter("Grain", 1.0, 0.5, 5.0)
      .setDescription("Scale of the geometric modulation");

    final BooleanParameter modeToggle =
      new BooleanParameter("Spiral", false)
      .setDescription("Toggle between intricate mode and spiral flicker mode");

    final CompoundParameter rotate =
      new CompoundParameter("Rotate", 0, -3.14, 3.14)
      .setDescription("Rotation of spatial geometry");

    final CompoundParameter contrast =
      new CompoundParameter("Contrast", 0.7, 0.2, 1.5)
      .setDescription("Contrast of final signal blend");

    public Nonrepeat2(LX lx) {
      super(lx);
      addParameter("phase", this.phase);
      addParameter("decoyStrength", this.decoyStrength);
      addParameter("slip", this.slip);
      addParameter("flicker", this.flicker);
      addParameter("grain", this.grain);
      addParameter("modeToggle", this.modeToggle);
      addParameter("rotate", this.rotate);
      addParameter("contrast", this.contrast);
    }

    @Override
    public void render(double deltaMs) {
      float t = (float)(lx.engine.nowMillis / 1000.);
      float pShift = t * phase.getValuef();
      float slipVal = slip.getValuef();
      float slipAmount = 0.5f - Math.abs(0.5f - slipVal);
      float blend = decoyStrength.getValuef();
      float flickBlend = flicker.getValuef();
      float scale = grain.getValuef();
      float rot = rotate.getValuef();
      float contrastAmount = contrast.getValuef();
      boolean spiralMode = modeToggle.getValueb();

      for (LXPoint p : model.points) {
        float x0 = p.xn - 0.5f;
        float y0 = p.yn - 0.5f;
        float x = x0 * (float)Math.cos(rot) - y0 * (float)Math.sin(rot);
        float y = x0 * (float)Math.sin(rot) + y0 * (float)Math.cos(rot);

        x *= scale;
        y *= scale;

        float r = (float)Math.sqrt(x * x + y * y);
        float theta = (float)Math.atan2(y, x);

        float signal;

        if (spiralMode) {
          float phaseComponent = 10 * r + pShift;
          float base = (float)Math.sin(2 * Math.PI * (theta + phaseComponent));
          float alt = (float)Math.sin(2 * Math.PI * (theta + phaseComponent + slipAmount));
          signal = lerp(base, alt, blend);
        } else {
          float radialMod = 5 * (float)Math.sin(theta + pShift);
          float base = (float)Math.sin(2 * Math.PI * (r + radialMod));
          float alt = (float)Math.sin(2 * Math.PI * (r + radialMod + slipAmount));
          signal = lerp(base, alt, blend);
        }

        float flickerWave = (float)Math.sin(2 * Math.PI * t);
        signal = lerp(signal, signal * flickerWave, flickBlend);

        // Apply contrast
        signal = (float)Math.tanh(signal * contrastAmount);

        int c = LXColor.gray((int)(127 + 127 * signal));
        colors[p.index] = c;
      }
    }

    private float lerp(float a, float b, float f) {
      return a + f * (b - a);
    }
  }
