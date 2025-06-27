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

import org.joml.Matrix2f;

import apotheneum.Apotheneum;
import apotheneum.ApotheneumPattern;
import heronarts.glx.ui.component.UIDropMenu;
import heronarts.glx.ui.component.UIKnob;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponent;
import heronarts.lx.LXComponentName;
import heronarts.lx.color.LXColor;
import heronarts.lx.midi.MidiNoteOn;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundDiscreteParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.TriggerParameter;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import heronarts.lx.utils.LXUtils;

@LXCategory("Apotheneum/mcslee")
@LXComponentName("Cube Bursts")
@LXComponent.Description("MIDI reactive emanataions on the cube faces")
public class CubeBursts extends ApotheneumPattern implements ApotheneumPattern.Midi, UIDeviceControls<CubeBursts> {

  public interface DistanceFunction {
    public float getDistance(float xd, float yd);
  }

  public enum Shape {
    CIRCLE("Circle", (xd, yd) -> { return (float) Math.sqrt(xd*xd + yd*yd); }),
    SQUARE("Square", (xd, yd) -> { return LXUtils.maxf(Math.abs(xd), Math.abs(yd)); }),
    DIAMOND("Diamond", (xd, yd) -> { return .5f * Math.abs(xd) + .5f * Math.abs(yd); }),
    CROSS("Cross", (xd, yd) -> { return LXUtils.minf(Math.abs(xd), Math.abs(yd)); });

    public final String label;
    public final DistanceFunction distance;

    private Shape(String label, DistanceFunction distance) {
      this.label = label;
      this.distance = distance;
    }

    @Override
    public String toString() {
      return this.label;
    }
  }

  public final TriggerParameter burst =
    new TriggerParameter("Burst", this::onBurst)
    .setDescription("Trigger a burst");

  public EnumParameter<Shape> shape1 =
    new EnumParameter<Shape>("Shape 1", Shape.CIRCLE)
    .setDescription("Burst shape 1");

  public EnumParameter<Shape> shape2 =
    new EnumParameter<Shape>("Shape 2", Shape.CIRCLE)
    .setDescription("Burst shape 2");

  public final CompoundParameter shapeLerp =
    new CompoundParameter("Shape", 0)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Blend between two shapes");

  public final BooleanParameter allFaces =
    new BooleanParameter("All Faces", false)
    .setDescription("Burst on all faces at once");

  public final CompoundDiscreteParameter perTrig =
    new CompoundDiscreteParameter("Per Trigger", 1, 1, 16)
    .setDescription("Number of bursts per trigger");

  public final CompoundParameter burstTime =
    new CompoundParameter("Time", 1, .5, 5)
    .setUnits(CompoundParameter.Units.SECONDS)
    .setDescription("Burst Time");

  public final CompoundParameter burstRadius =
    new CompoundParameter("Radius ", .1)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Burst Radius");

  public final CompoundParameter burstThickness =
    new CompoundParameter("Thickness ", .05, .5)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Burst Thickness");

  public final CompoundParameter burstAttack =
    new CompoundParameter("Atk ", 0, .5)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Burst Attack");

  public final CompoundParameter burstExp =
    new CompoundParameter("Exp ", 1, .25, 3)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Burst Exp");

  public final CompoundParameter spin =
    new CompoundParameter("Spin", 0, 360)
    .setUnits(CompoundParameter.Units.DEGREES)
    .setWrappable(true)
    .setDescription("Spin the bursts");

  public CubeBursts(LX lx) {
    super(lx);
    addParameter("burst", this.burst);
    addParameter("shape1", this.shape1);
    addParameter("shape2", this.shape2);
    addParameter("shapeLerp", this.shapeLerp);
    addParameter("spin", this.spin);
    addParameter("allFaces", this.allFaces);
    addParameter("perTrig", this.perTrig);
    addParameter("burstRadius", this.burstRadius);
    addParameter("burstThickness", this.burstThickness);
    addParameter("burstTime", this.burstTime);
    addParameter("burstExp", this.burstExp);
    addParameter("burstAttack", this.burstAttack);
  }

  @Override
  protected void onModelChanged(LXModel model) {
    this.bursts.clear();
  }

  private final List<Burst> bursts = new ArrayList<>();

  private class Burst {

    private final Apotheneum.Cube.Face face;
    private float basis;
    private final float xn;
    private final float yn;

    private Burst(Apotheneum.Cube.Face face) {
      this.face = face;
      this.xn = (float) LXUtils.lerp(.25, .75, Math.random());
      this.yn = (float) LXUtils.lerp(.25, .75, Math.random());
    }

    protected void render(double deltaMs) {
      final DistanceFunction distance1 = shape1.getEnum().distance;
      final DistanceFunction distance2 = shape2.getEnum().distance;
      final float sLerp = shapeLerp.getValuef();

      this.basis += deltaMs / (1000f * burstTime.getValuef());
      if (this.basis < 1) {
          final double thickness = LXUtils.lerp(0.01, burstThickness.getValuef(), LXUtils.min(1, this.basis / burstAttack.getValue()));
          final float falloff = 100 / (float) thickness;

        final float radius = (float) (burstRadius.getValue() * Math.pow(this.basis, burstExp.getValuef()));
        final float level = LXUtils.lerpf(100, 0, this.basis);
        int x = 0;
        for (LXModel column : face.columns) {
          int y = 0;
          for (LXPoint p : column.points) {
            float pxn = x / (Apotheneum.GRID_WIDTH - 1f);
            float pyn = y / (Apotheneum.GRID_HEIGHT - 1f);
            float dx = this.xn - pxn;
            float dy = this.yn - pyn;
            float xd = spinMatrix.m00 * dx + spinMatrix.m10 * dy;
            float yd = spinMatrix.m01 * dx + spinMatrix.m11 * dy;

            float dist = LXUtils.lerpf(
              distance1.getDistance(xd, yd),
              distance2.getDistance(xd, yd),
              sLerp
            );
            double b = level - falloff * Math.abs(dist - radius);
            if (b > 0) {
              colors[p.index] = LXColor.lightest(colors[p.index], LXColor.gray(b));
            }
            ++y;
          }
          ++x;
        }
      }
    }
  }

  private void onBurst() {
    if (Apotheneum.exists) {
      final int num = this.perTrig.getValuei();
      final boolean allFaces = this.allFaces.isOn();
      for (int i = 0; i < num; ++i) {
        if (allFaces) {
          for (Apotheneum.Cube.Face face : Apotheneum.cube.exterior.faces) {
            this.bursts.add(new Burst(face));
          }
        } else {
          this.bursts.add(new Burst(Apotheneum.cube.exterior.faces[LXUtils.randomi(0, 3)]));
        }
      }
    }
  }

  private final List<Burst> finished = new ArrayList<>();

  private final Matrix2f spinMatrix = new Matrix2f();

  @Override
  protected void render(double deltaMs) {
    setApotheneumColor(LXColor.BLACK);
    spinMatrix.rotation((float) Math.toRadians(spin.getValuef()));

    this.finished.clear();
    for (Burst burst : this.bursts) {
      burst.render(deltaMs);
      if (burst.basis >= 1) {
        this.finished.add(burst);
      }
    }
    if (!this.finished.isEmpty()) {
      this.bursts.removeAll(this.finished);
      this.finished.clear();
    }
    copyCubeExterior();
  }

  @Override
  public void noteOnReceived(MidiNoteOn note) {
    onBurst();
  }

  @Override
  public void buildDeviceControls(UI ui, UIDevice uiDevice, CubeBursts cubeBursts) {
    uiDevice.setLayout(UIDevice.Layout.HORIZONTAL, 4);

    addColumn(uiDevice,
      newButton(cubeBursts.burst).setTriggerable(true).setBorderRounding(4),
      newIntegerBox(cubeBursts.perTrig),
      newButton(cubeBursts.allFaces),
      newDropMenu(cubeBursts.shape2),
      newVerticalSlider(cubeBursts.shapeLerp, 60).setShowLabel(false),
      newDropMenu(cubeBursts.shape1).setDirection(UIDropMenu.Direction.UP)
    ).setChildSpacing(4);

    addVerticalBreak(ui, uiDevice);

    addColumn(uiDevice, UIKnob.WIDTH,
      "Shape",
      newKnob(cubeBursts.burstRadius, 0),
      newKnob(cubeBursts.burstThickness, 0),
      newKnob(cubeBursts.spin, 0)
    ).setChildSpacing(6);

    addVerticalBreak(ui, uiDevice);

    addColumn(uiDevice, UIKnob.WIDTH,
      "Time",
      newKnob(cubeBursts.burstTime, 0),
      newKnob(cubeBursts.burstAttack, 0),
      newKnob(cubeBursts.burstExp, 0)
    ).setChildSpacing(6);
  }

}
