/**
 * Copyright 2025- Mark C. Slee, Heron Arts LLC
 *
 * This file is part of the LX Studio software library. By using
 * LX, you agree to the terms of the LX Studio Software License
 * and Distribution Agreement, available at: http://lx.studio/license
 *
 * Please note that the LX license is not open-source. The license
 * allows for free, non-commercial use.
 *
 * HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR
 * OTHERWISE, AND SPECIFICALLY DISCLAIMS ANY WARRANTY OF
 * MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR
 * PURPOSE, WITH RESPECT TO THE SOFTWARE.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */

package apotheneum.mcslee;

import apotheneum.Apotheneum;
import apotheneum.ApotheneumPattern;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponentName;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.modulator.SawLFO;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.utils.LXUtils;

@LXCategory("Apotheneum/mcslee")
@LXComponentName("DNA Helix")
public class DNAHelix extends ApotheneumPattern {

  public final CompoundParameter twist =
    new CompoundParameter("Twist", 0)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Twist");

  public final CompoundParameter winding =
    new CompoundParameter("Winding", 1, 10)
    .setDescription("Amount of DNA winding");

  public final CompoundParameter width =
    new CompoundParameter("Width", 5, Apotheneum.RING_LENGTH)
    .setDescription("Width of DNA strands");

  public final CompoundParameter noise =
    new CompoundParameter("Noise", 0)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Noise");

  private final SawLFO noiseX = new SawLFO(0, 256, 256*3000);
  private final SawLFO noiseY = new SawLFO(0, 256, 256*5000);

  public DNAHelix(LX lx) {
    super(lx);
    addParameter("twist", this.twist);
    addParameter("winding", this.winding);
    addParameter("width", this.width);
    addParameter("noise", this.noise);
    startModulator(this.noiseX);
    startModulator(this.noiseY);
  }

  private float wraplerpf(float v1, float v2, float lerp, float wrap) {
    if (v1 < v2) {
      if (v2 - v1 < .5f * wrap) {
        return LXUtils.lerpf(v1, v2, lerp);
      } else {
        return LXUtils.lerpf(v1+wrap, v2, lerp) % wrap;
      }
    } else {
      if (v1 - v2 < .5f * wrap) {
        return LXUtils.lerpf(v1, v2, lerp);
      } else {
        return LXUtils.lerpf(v1, v2+wrap, lerp) % wrap;
      }
    }
  }

  @Override
  protected void render(double deltaMs) {
    setColors(LXColor.BLACK);

    final float twist = this.twist.getValuef();
    final float winding = this.winding.getValuef();
    final float coeff = winding / Apotheneum.CYLINDER_HEIGHT;
    final float falloff = 100 / this.width.getValuef();
    final float noise = this.noise.getValuef();
    final float noiseX = this.noiseX.getValuef();
    final float noiseY = this.noiseY.getValuef();

    int ri = -Apotheneum.CYLINDER_HEIGHT / 2;
    for (Apotheneum.Cylinder.Ring ring : Apotheneum.cylinder.exterior.rings) {
      float basis =
        100 +
        (twist + ri * coeff) +
        3 * noise * LXUtils.noise(noiseX, noiseY, ri * .1f);
      float pos = Apotheneum.RING_LENGTH * (basis % 1f);
      int pi = 0;
      for (LXPoint p : ring.points) {
        float dist = LXUtils.wrapdistf((2 * pi) % ring.points.length, pos, ring.points.length);
        ++pi;
        colors[p.index] = LXColor.gray(LXUtils.max(0, 100 - falloff * dist));
      }
      ++ri;
    }

  }

}
