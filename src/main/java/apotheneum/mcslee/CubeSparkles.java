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

import java.util.ArrayList;
import java.util.List;

import apotheneum.Apotheneum;
import apotheneum.ApotheneumPattern;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponent;
import heronarts.lx.LXComponentName;
import heronarts.lx.color.LXColor;
import heronarts.lx.midi.MidiNoteOn;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundDiscreteParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.TriggerParameter;
import heronarts.lx.utils.LXUtils;

@LXCategory("Apotheneum/mcslee")
@LXComponentName("Cube Sparkles")
@LXComponent.Description("MIDI reactive sparkles on the cube faces")
public class CubeSparkles extends ApotheneumPattern implements ApotheneumPattern.Midi {

  public final TriggerParameter sparkle =
    new TriggerParameter("Sparkle", this::onSparkle)
    .setDescription("Trigger a sparkle");

  public final CompoundDiscreteParameter perTrig =
    new CompoundDiscreteParameter("Per Trigger", 1, 1, 64)
    .setDescription("Number of sparkles per trigger");

  public final CompoundParameter maxHeight =
    new CompoundParameter("Height", .5)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Maximum height of sparkle placement");

  public final CompoundParameter sparkleTime =
    new CompoundParameter("Time", 1, .5, 5)
    .setUnits(CompoundParameter.Units.SECONDS)
    .setDescription("Sparkle Time");

  public final CompoundParameter sparkleDistance =
    new CompoundParameter("Distance ", .1)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Sparkle Time");

  public final CompoundParameter sparkleExp =
    new CompoundParameter("Exp ", 1, .5, 2)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Sparkle Exp");

  public CubeSparkles(LX lx) {
    super(lx);
    addParameter("sparkle", this.sparkle);
    addParameter("perTrig", this.perTrig);
    addParameter("maxHeight", this.maxHeight);
    addParameter("sparkleTime", this.sparkleTime);
    addParameter("sparkleDistance", this.sparkleDistance);
    addParameter("sparkleExp", this.sparkleExp);
  }

  @Override
  protected void onModelChanged(LXModel model) {
    this.sparkles.clear();
  }

  private final List<Sparkle> sparkles = new ArrayList<>();

  private class Sparkle {

    private final LXModel column;
    private final float basePos;
    private float basis;

    private Sparkle(Apotheneum.Cube.Face face) {
      this.column = face.columns[LXUtils.randomi(face.columns.length-1)];
      this.basePos = LXUtils.randomf(maxHeight.getValuef());
    }

    protected void render(double deltaMs) {
      this.basis += deltaMs / (1000f * sparkleTime.getValuef());
      if (this.basis < 1) {
        float dist = (float) (sparkleDistance.getValue() * Math.pow(this.basis, sparkleExp.getValuef()));
        float level = LXUtils.lerpf(100, 0, this.basis);
        float length = LXUtils.lerpf(1, 10, this.basis);
        float falloff = 4500f / length;
        for (LXPoint p : this.column.points) {
          float b = level - falloff * Math.abs(Math.abs(p.yn - this.basePos) - dist);
          if (b > 0) {
            addColor(p.index, LXColor.gray(b));
          }
        }

      }
    }
  }

  private void onSparkle() {
    if (Apotheneum.exists) {
      int num = this.perTrig.getValuei();
      for (int i = 0; i < num; ++i) {
        for (Apotheneum.Cube.Face face : Apotheneum.cube.exterior.faces) {
          this.sparkles.add(new Sparkle(face));
        }
      }
    }
  }

  private final List<Sparkle> finished = new ArrayList<>();

  @Override
  protected void render(double deltaMs) {
    setColors(LXColor.BLACK);
    this.finished.clear();
    for (Sparkle sparkle : this.sparkles) {
      sparkle.render(deltaMs);
      if (sparkle.basis >= 1) {
        this.finished.add(sparkle);
      }
    }
    if (!this.finished.isEmpty()) {
      this.sparkles.removeAll(this.finished);
      this.finished.clear();
    }
    copyCubeExterior();
  }

  @Override
  public void noteOnReceived(MidiNoteOn note) {
    onSparkle();
  }


}
