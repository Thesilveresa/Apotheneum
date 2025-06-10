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
import heronarts.glx.ui.component.UIKnob;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponent;
import heronarts.lx.LXComponentName;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.utils.LXUtils;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.CompoundDiscreteParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;

@LXCategory("Apotheneum/mcslee")
@LXComponentName("Crawlers")
@LXComponent.Description("Snake-like objects crawling around the surfaces")
public class Crawlers extends ApotheneumPattern implements UIDeviceControls<Crawlers> {

  private static final int MAX_CRAWLERS = 240;
  private static final int MAX_LENGTH = 240;

  public final DiscreteParameter numCube =
    new DiscreteParameter("Num Cube", 64, 0, MAX_CRAWLERS + 1)
    .setDescription("Number of active cube crawlers");

  public final DiscreteParameter numCylinder =
    new DiscreteParameter("Num Cylinder", 64, 0, MAX_CRAWLERS + 1)
    .setDescription("Number of active cube crawlers");

  public final CompoundDiscreteParameter minLength =
    new CompoundDiscreteParameter("Min Length", 16, 1, 64)
    .setDescription("Minimum crawler length");

  public final CompoundDiscreteParameter maxLength =
    new CompoundDiscreteParameter("Max Length", 32, 1, 129)
    .setDescription("Maximum crawler length");

  public final CompoundParameter biasLength =
    new CompoundParameter("Bias Length", .5)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setPolarity(CompoundParameter.Polarity.BIPOLAR)
    .setDescription("Bias crawlers towards shorter or longer lengths");

  public final CompoundParameter minSpeed =
    new CompoundParameter("Min Speed", 0, 10)
    .setDescription("Minimum crawler speed");

  public final CompoundParameter maxSpeed =
    new CompoundParameter("Max Speed", 10, 100)
    .setDescription("Maximum crawler speed");

  public final CompoundParameter biasSpeed =
    new CompoundParameter("Bias Speed", .5)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setPolarity(CompoundParameter.Polarity.BIPOLAR)
    .setDescription("Bias crawlers towards slower or faster movement ");

  public final CompoundParameter turnProbability =
    new CompoundParameter("Turn Prob", .5)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Probability of turning on any step");

  public final DiscreteParameter turnGate =
    new DiscreteParameter("Turn Gate", 0, 64)
    .setDescription("Require a certain number of steps between turns");

  private final Crawler[] cubeCrawlers = new Crawler[MAX_CRAWLERS];
  private final Crawler[] cylinderCrawlers = new Crawler[MAX_CRAWLERS];

  public Crawlers(LX lx) {
    super(lx);
    addParameter("numCube", this.numCube);
    addParameter("numCylinder", this.numCylinder);
    addParameter("minLength", this.minLength);
    addParameter("maxLength", this.maxLength);
    addParameter("biasLength", this.biasLength);
    addParameter("minSpeed", this.minSpeed);
    addParameter("maxSpeed", this.maxSpeed);
    addParameter("biasSpeed", this.biasSpeed);
    addParameter("turnProbability", this.turnProbability);
    addParameter("turnGate", this.turnGate);

    for (int i = 0; i < MAX_CRAWLERS; ++i) {
      this.cubeCrawlers[i] = new Crawler(Apotheneum.cube.exterior);
      this.cylinderCrawlers[i] = new Crawler(Apotheneum.cylinder.exterior);
    }
  }

  private class Crawler {

    private class Coord {
      private int x;
      private int y;

      private void set(int x, int y) {
        this.x = x;
        this.y = y;
      }
    }

    private enum Direction {
      LEFT(-1, 0),
      RIGHT(1, 0),
      UP(0, 1),
      DOWN(0, -1);

      private final int x, y;

      private Direction(int x, int y) {
        this.x = x;
        this.y = y;
      }
    }

    private final Apotheneum.Orientation orientation;

    private final double rnd = Math.random();

    private double basis = 0;
    private int length = 0;
    private final Coord[] coords = new Coord[MAX_LENGTH];
    private int head = 0;
    private Direction direction;
    private int turnCount = 0;

    private Crawler(Apotheneum.Orientation orientation) {
      this.orientation = orientation;
      for (int i = 0; i < MAX_LENGTH; ++i) {
        this.coords[i] = new Coord();
      }
      this.coords[this.head].set(
        LXUtils.randomi(0, orientation.width() - 1),
        LXUtils.randomi(0, orientation.height() - 1)
      );
      this.direction = Direction.values()[(LXUtils.randomi(0, Direction.values().length - 1))];
      this.length = 1;
    }

    private void step() {
      final int width = this.orientation.width();
      final int height = this.orientation.height();

      final Coord current = getCoord(0);
      final Coord next = getCoord(1);
      if ((this.turnCount >= turnGate.getValuei()) && Math.random() < turnProbability.getValue()) {
        this.turnCount = 0;
        if (this.direction.x == 0) {
          this.direction = (Math.random() < .5) ? Direction.LEFT : Direction.RIGHT;
        } else {
          this.direction = (Math.random() < .5) ? Direction.UP : Direction.DOWN;
        }
      } else {
        ++this.turnCount;
      }
      next.x = (current.x + width + this.direction.x) % width;
      next.y = (current.y + height + this.direction.y) % height;
      this.head = (this.head + 1) % this.coords.length;
      this.length = LXUtils.min(MAX_LENGTH, this.length+1);
    }

    private double bias(LXParameter p1, LXParameter p2, LXParameter bias, double rnd) {
      final double b = bias.getValue();
      return LXUtils.lerp(
        p1.getValue(),
        p2.getValue(),
        (b < 0.5) ?
          Math.pow(rnd, LXUtils.lerp(3, 1, 2*b)) :
          1 - Math.pow(1-rnd, LXUtils.lerp(1, 3, 2 * (b-.5f)))
      );
    }

    private void advance(double deltaMs) {
      this.basis += deltaMs * bias(minSpeed, maxSpeed, biasSpeed, this.rnd) / 1000;
      if (this.basis > 1.) {
        step();
        this.basis = this.basis % 1.;
      }
      final int length = (int) bias(minLength, maxLength, biasLength, this.rnd);
      final int limit = LXUtils.min(length, this.length);
      for (int i = 0; i < limit; ++i) {
        final Coord coord = getCoord(-i);
        double b = (i == 0) ? basis : (i == length-1) ? (1-basis) : 1;
        if (b > 0) {
          final int idx = this.orientation.point(coord.x, coord.y).index;
          colors[idx] = LXColor.lightest(colors[idx], LXColor.grayn(b));
        }
      }
    }

    private Coord getCoord(int index) {
      return this.coords[(this.head + this.coords.length + index) % this.coords.length];
    }

    private void render(double deltaMs) {
      advance(deltaMs);

    }
  }


  @Override
  protected void render(double deltaMs) {
    setColors(LXColor.BLACK);

    final int numCube = this.numCube.getValuei();
    if (numCube > 0) {
      for (int i = 0; i < numCube; ++i) {
        this.cubeCrawlers[i].render(deltaMs);
      }
      copyCubeExterior();
    }

    final int numCylinder = this.numCylinder.getValuei();
    if (numCylinder > 0) {
      for (int i = 0; i < numCylinder; ++i) {
        this.cylinderCrawlers[i].render(deltaMs);
      }
      copyCylinderExterior();
    }

  }


  @Override
  public void buildDeviceControls(UI ui, UIDevice uiDevice, Crawlers crawlers) {
    uiDevice.setLayout(UIDevice.Layout.HORIZONTAL);
    uiDevice.setChildSpacing(2);
    addColumn(uiDevice, UIKnob.WIDTH, "Num",
      newKnob(crawlers.numCube, 0),
      newKnob(crawlers.numCylinder, 0)
    ).setChildSpacing(6);
    addVerticalBreak(ui, uiDevice);
    addColumn(uiDevice, UIKnob.WIDTH, "Speed",
      newKnob(crawlers.minSpeed, 0),
      newKnob(crawlers.maxSpeed, 0),
      newKnob(crawlers.biasSpeed, 0)
    ).setChildSpacing(6);
    addVerticalBreak(ui, uiDevice);
    addColumn(uiDevice, UIKnob.WIDTH, "Length",
      newKnob(crawlers.minLength, 0),
      newKnob(crawlers.maxLength, 0),
      newKnob(crawlers.biasLength, 0)
    ).setChildSpacing(6);
    addVerticalBreak(ui, uiDevice);
    addColumn(uiDevice, UIKnob.WIDTH, "Turning",
      newKnob(crawlers.turnGate, 0),
      newKnob(crawlers.turnProbability, 0)
    ).setChildSpacing(6);
  }

}
