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
import heronarts.lx.LXComponent;
import heronarts.lx.LXComponentName;
import heronarts.lx.LXLayer;
import heronarts.lx.color.LXColor;
import heronarts.lx.midi.MidiNoteOn;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.osc.OscMessage;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundDiscreteParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.TriggerParameter;
import heronarts.lx.utils.LXUtils;

@LXCategory("Apotheneum/mcslee")
@LXComponentName("Raindrops")
@LXComponent.Description("Accelerating raindrops that splash onto the cube/cylinder base")
public class Raindrops extends ApotheneumPattern implements ApotheneumPattern.Midi {

  public final TriggerParameter trig =
    new TriggerParameter("Trig", this::trig)
    .setDescription("Trigger a new raindrop");

  public final CompoundDiscreteParameter perTrig =
    new CompoundDiscreteParameter("Per Trig", 1, 1, 21)
    .setDescription("How many drops fall per trigger");

  public final CompoundParameter position =
    new CompoundParameter("Position", 0)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Probability of drops falling in the cylinder vs cube, 100% being all cube");

  public final CompoundParameter gravity =
    new CompoundParameter("Gravity", 1)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Gravity strength");

  public final CompoundParameter initVelocityMin =
    new CompoundParameter("Min Init", 0, 5)
    .setDescription("Minimum initial velocity");

  public final CompoundParameter initVelocityMax =
    new CompoundParameter("Max Init", 5, 0, 20)
    .setDescription("Maximum initial velocity");

  public final CompoundParameter tailLength =
    new CompoundParameter("Tail", 5, 1, 20)
    .setDescription("Tail length");

  public final CompoundParameter floor =
    new CompoundParameter("Floor", 0)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Floor position");

  public final CompoundParameter floorRand =
    new CompoundParameter("Rand", 0, 10)
    .setDescription("Amoutn of randomization applied to splash point");

  public final BooleanParameter sendSplash =
    new BooleanParameter("Send", false)
    .setDescription("Send splashes to OSC out");

  private final OscMessage oscSplash = new OscMessage("/raindrops/splash");

  private class Drop extends LXLayer {

    private final Apotheneum.Orientation orientation;
    private final LXModel column;
    private final int ringIndex;
    private double pos = 0;
    private double velocity = 0;
    private boolean hasSplashed = false;
    private final int splashPoint;
    private final LXPoint[] ring;

    private Drop(LX lx) {
      super(lx);
      this.orientation =
        (Math.random() < position.getValue()) ?
          Apotheneum.cube.exterior :
          Apotheneum.cylinder.exterior;
      this.ringIndex = LXUtils.randomi(0, orientation.columns().length-1);
      this.column = orientation.column(this.ringIndex);
      this.splashPoint = (int) LXUtils.min(
        orientation.available(this.ringIndex) - 1,
        LXUtils.lerp(column.points.length-1, 0, floor.getValue()) + floorRand.getValue() * Math.random()
      );
      this.ring = this.orientation.ring(this.splashPoint).points;
      this.velocity = LXUtils.lerp(initVelocityMin.getValue(), initVelocityMax.getValue(), Math.random());
    }

    @Override
    public void run(double deltaMs) {
      final double acc = gravity.getValue() * 385.826 / 9.375;
      final double t = deltaMs * .001;
      this.pos += .5 * acc * t * t + this.velocity * t;
      this.velocity += acc * t;

      final double fade = 1 / tailLength.getValue();

      boolean done = true;
      int pi = 0;
      for (LXPoint p : column.points) {
        double b = (pi <= this.pos) ?
          1 - fade * (this.pos - pi) :
          1 - (pi - this.pos);
        if (b > 0) {
          colors[p.index] = LXColor.lightest(colors[p.index], LXColor.grayn(b));
          done = false;
        }
        if (++pi > this.splashPoint) {
          break;
        }
      }

      final double splash = this.pos - this.splashPoint;
      if (splash > 0) {
        if (!this.hasSplashed) {
          if (sendSplash.isOn()) {
            Apotheneum.osc2Ableton(oscSplash);
          }
          this.hasSplashed = true;
        }
        final double splashLerp = Math.sqrt(.5 * splash);
        int ri = 0;
        for (LXPoint p : this.ring) {
          double b =
            LXUtils.lerp(1, 0, splashLerp * .15) -
            .25 * Math.abs(LXUtils.wrapdistf(this.ringIndex, ri, this.ring.length) - splashLerp);
          if (b > 0) {
            colors[p.index] = LXColor.lightest(colors[p.index], LXColor.grayn(b));
            done = false;
          }
          ++ri;
        }
      }

      if (done) {
        remove();
      }
    }
  }


  public Raindrops(LX lx) {
    super(lx);
    addParameter("trig", this.trig);
    addParameter("perTrig", this.perTrig);
    addParameter("position", this.position);
    addParameter("floor", this.floor);
    addParameter("floorRand", this.floorRand);
    addParameter("tailLength", this.tailLength);
    addParameter("gravity", this.gravity);
    addParameter("initVelocityMin", this.initVelocityMin);
    addParameter("initVelocityMax", this.initVelocityMax);
    addParameter("sendSplash", this.sendSplash);
  }

  private void trig() {
    final int perTrig = this.perTrig.getValuei();
    for (int i = 0; i < perTrig; ++i) {
      addLayer(new Drop(this.lx));
    }
  }

  @Override
  protected void render(double deltaMs) {
    setColors(LXColor.BLACK);
  }

  @Override
  protected void afterLayers(double deltaMs) {
    copyExterior();
  }

  @Override
  public void noteOnReceived(MidiNoteOn midiNote) {
    trig();
  }

}
