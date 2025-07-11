package apotheneum.doved.patterns;

import apotheneum.ApotheneumRasterPattern;
import apotheneum.doved.lightning.LightningSegment;
import apotheneum.doved.lightning.MidpointDisplacementAlgorithm;
import apotheneum.doved.lightning.LSystemAlgorithm;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponent;
import heronarts.lx.LXComponentName;
import heronarts.lx.midi.MidiNoteOn;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.TriggerParameter;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import heronarts.lx.utils.LXUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@LXCategory("Apotheneum/doved")
@LXComponentName("Lightning")
@LXComponent.Description("Lightning strikes using multiple algorithms")
public class Lightning extends ApotheneumRasterPattern implements ApotheneumRasterPattern.Midi, UIDeviceControls<Lightning> {

  public final TriggerParameter trig =
    new TriggerParameter("Trig", this::trig)
    .setDescription("Trigger a lightning strike");

  public final DiscreteParameter algorithm =
    new DiscreteParameter("Algorithm", new String[] {"Midpoint", "L-System"}, 0)
    .setDescription("Lightning generation algorithm");

  public final CompoundParameter intensity =
    new CompoundParameter("Intensity", 1)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Overall brightness of lightning");

  public final CompoundParameter branchProbability =
    new CompoundParameter("Branch", 0.3, 0, 1)
    .setDescription("Likelihood of creating branches during displacement");

  public final CompoundParameter displacement =
    new CompoundParameter("Displace", 0.5, 0, 1)
    .setDescription("Maximum perpendicular displacement for midpoint algorithm");

  public final CompoundParameter recursionDepth =
    new CompoundParameter("Depth", 6, 3, 10)
    .setDescription("Maximum subdivision levels for detail");

  public final CompoundParameter fadeTime =
    new CompoundParameter("Fade", 2, 0.5, 10)
    .setDescription("Duration for lightning trails to fade (seconds)");

  public final CompoundParameter thickness =
    new CompoundParameter("Thickness", 1, 0.5, 3)
    .setDescription("Base thickness of lightning bolts");

  public final CompoundParameter startSpread =
    new CompoundParameter("Start Spread", 0.5, 0, 1)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("How spread out lightning start points are across the top");

  public final CompoundParameter endSpread =
    new CompoundParameter("End Spread", 0.5, 0, 1)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("How spread out lightning end points are across the bottom");

  public final CompoundParameter branchDistance =
    new CompoundParameter("Branch Dist", 0.5, 0.1, 2)
    .setDescription("Maximum distance branches can extend from main bolt");

  public final CompoundParameter branchAngle =
    new CompoundParameter("Branch Angle", 0.5, 0, 1)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("How much branches can deviate from the main bolt direction");

  // L-System specific parameters
  public final CompoundParameter lsIterations =
    new CompoundParameter("LS Iterations", 4, 2, 8)
    .setDescription("Number of L-system iterations");

  public final CompoundParameter lsSegmentLength =
    new CompoundParameter("LS Segment Len", 8, 2, 20)
    .setDescription("Base length of L-system segments");

  public final CompoundParameter lsAngleVariation =
    new CompoundParameter("LS Angle Var", 0.5, 0, 1)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Random variation in L-system angles");

  public final CompoundParameter lsLengthVariation =
    new CompoundParameter("LS Length Var", 0.3, 0, 1)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Random variation in L-system segment lengths");

  public final CompoundParameter lsBranchAngle =
    new CompoundParameter("LS Branch Angle", 45, 15, 90)
    .setDescription("Base angle for L-system branches in degrees");


  private static class LightningBolt {
    public final List<LightningSegment> segments = new ArrayList<>();
    public final double startTime;
    public final double fadeTimeMs;

    public LightningBolt(double startTime, double fadeTimeMs) {
      this.startTime = startTime;
      this.fadeTimeMs = fadeTimeMs;
    }

    public boolean isExpired(double currentTime) {
      return (currentTime - startTime) > fadeTimeMs;
    }

    public double getFadeAmount(double currentTime) {
      double elapsed = currentTime - startTime;
      return Math.max(0, 1.0 - (elapsed / fadeTimeMs));
    }
  }

  private final List<LightningBolt> bolts = new ArrayList<>();

  public Lightning(LX lx) {
    super(lx);
    addParameter("trig", this.trig);
    addParameter("algorithm", this.algorithm);
    addParameter("intensity", this.intensity);
    addParameter("branchProbability", this.branchProbability);
    addParameter("displacement", this.displacement);
    addParameter("recursionDepth", this.recursionDepth);
    addParameter("fadeTime", this.fadeTime);
    addParameter("thickness", this.thickness);
    addParameter("startSpread", this.startSpread);
    addParameter("endSpread", this.endSpread);
    addParameter("branchDistance", this.branchDistance);
    addParameter("branchAngle", this.branchAngle);
    addParameter("lsIterations", this.lsIterations);
    addParameter("lsSegmentLength", this.lsSegmentLength);
    addParameter("lsAngleVariation", this.lsAngleVariation);
    addParameter("lsLengthVariation", this.lsLengthVariation);
    addParameter("lsBranchAngle", this.lsBranchAngle);
  }

  private void trig() {
    double currentTime = System.currentTimeMillis();
    double fadeTimeMs = fadeTime.getValue() * 1000;
    LightningBolt bolt = new LightningBolt(currentTime, fadeTimeMs);
    
    // Generate lightning based on selected algorithm
    if (algorithm.getValuei() == 0) {
      generateMidpointLightning(bolt);
    } else {
      generateLSystemLightning(bolt);
    }
    
    synchronized (bolts) {
      bolts.add(bolt);
    }
  }
  
  private void generateMidpointLightning(LightningBolt bolt) {
    List<MidpointDisplacementAlgorithm.LightningSegment> midpointSegments = new ArrayList<>();
    
    MidpointDisplacementAlgorithm.Parameters params = new MidpointDisplacementAlgorithm.Parameters(
      displacement.getValue(),
      (int) recursionDepth.getValue(),
      startSpread.getValue(),
      endSpread.getValue(),
      branchProbability.getValue(),
      branchDistance.getValue(),
      branchAngle.getValue(),
      RASTER_WIDTH,
      RASTER_HEIGHT
    );
    
    MidpointDisplacementAlgorithm.generateLightning(midpointSegments, params);
    
    // Convert to common lightning segments
    for (MidpointDisplacementAlgorithm.LightningSegment segment : midpointSegments) {
      bolt.segments.add(LightningSegment.fromMidpoint(segment));
    }
  }
  
  private void generateLSystemLightning(LightningBolt bolt) {
    List<LSystemAlgorithm.LightningSegment> lSystemSegments = new ArrayList<>();
    
    LSystemAlgorithm.Parameters params = new LSystemAlgorithm.Parameters(
      (int) lsIterations.getValue(),
      lsSegmentLength.getValue(),
      lsAngleVariation.getValue(),
      lsLengthVariation.getValue(),
      lsBranchAngle.getValue(),
      RASTER_WIDTH,
      RASTER_HEIGHT
    );
    
    LSystemAlgorithm.generateLightning(lSystemSegments, params);
    
    // Convert to common lightning segments
    for (LSystemAlgorithm.LightningSegment segment : lSystemSegments) {
      bolt.segments.add(LightningSegment.fromLSystem(segment));
    }
  }


  @Override
  protected void render(double deltaMs, Graphics2D graphics) {
    clear();
    
    double currentTime = System.currentTimeMillis();
    
    synchronized (bolts) {
      Iterator<LightningBolt> iterator = bolts.iterator();
      while (iterator.hasNext()) {
        LightningBolt bolt = iterator.next();
        
        if (bolt.isExpired(currentTime)) {
          iterator.remove();
          continue;
        }
        
        double fadeAmount = bolt.getFadeAmount(currentTime);
        renderBolt(graphics, bolt, fadeAmount);
      }
    }
  }

  private void renderBolt(Graphics2D graphics, LightningBolt bolt, double fadeAmount) {
    if (algorithm.getValuei() == 0) {
      renderMidpointBolt(graphics, bolt, fadeAmount);
    } else {
      renderLSystemBolt(graphics, bolt, fadeAmount);
    }
  }
  
  private void renderMidpointBolt(Graphics2D graphics, LightningBolt bolt, double fadeAmount) {
    double alpha = fadeAmount * intensity.getValue();
    
    for (LightningSegment segment : bolt.segments) {
      double segmentAlpha = alpha * segment.intensity;
      if (segment.isBranch) {
        segmentAlpha *= 0.7;
      }
      
      // Create lightning color with fade
      Color lightningColor = new Color(
        (float) LXUtils.constrain(0.8 + 0.2 * segmentAlpha, 0, 1),  // R
        (float) LXUtils.constrain(0.9 + 0.1 * segmentAlpha, 0, 1),  // G
        (float) LXUtils.constrain(1.0, 0, 1),                       // B
        (float) LXUtils.constrain(segmentAlpha, 0, 1)               // A
      );
      
      graphics.setColor(lightningColor);
      
      // Set stroke thickness - branches are thinner
      float strokeWidth = (float) (thickness.getValue() * (segment.isBranch ? 0.5 : 1.0));
      graphics.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      
      // Draw the lightning segment
      Path2D path = new Path2D.Double();
      path.moveTo(segment.x1, segment.y1);
      path.lineTo(segment.x2, segment.y2);
      graphics.draw(path);
      
      // Add glow effect for bright segments
      if (segmentAlpha > 0.5) {
        Color glowColor = new Color(
          (float) LXUtils.constrain(0.6 + 0.4 * segmentAlpha, 0, 1),
          (float) LXUtils.constrain(0.8 + 0.2 * segmentAlpha, 0, 1),
          (float) LXUtils.constrain(1.0, 0, 1),
          (float) LXUtils.constrain(segmentAlpha * 0.3, 0, 1)
        );
        graphics.setColor(glowColor);
        graphics.setStroke(new BasicStroke(strokeWidth * 2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(path);
      }
    }
  }
  
  private void renderLSystemBolt(Graphics2D graphics, LightningBolt bolt, double fadeAmount) {
    double alpha = fadeAmount * intensity.getValue();
    
    for (LightningSegment segment : bolt.segments) {
      double segmentAlpha = alpha * (0.3 + 0.7 * segment.intensity);
      
      // Create lightning color with fade
      Color lightningColor = new Color(
        (float) LXUtils.constrain(0.8 + 0.2 * segmentAlpha, 0, 1),  // R
        (float) LXUtils.constrain(0.9 + 0.1 * segmentAlpha, 0, 1),  // G
        (float) LXUtils.constrain(1.0, 0, 1),                       // B
        (float) LXUtils.constrain(segmentAlpha, 0, 1)               // A
      );
      
      graphics.setColor(lightningColor);
      
      // Set stroke thickness based on depth - deeper branches are thinner
      float strokeWidth = (float) (thickness.getValue() / (1.0 + segment.depth * 0.3));
      graphics.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      
      // Draw the lightning segment
      Path2D path = new Path2D.Double();
      path.moveTo(segment.x1, segment.y1);
      path.lineTo(segment.x2, segment.y2);
      graphics.draw(path);
      
      // Add glow effect for bright segments
      if (segmentAlpha > 0.4) {
        Color glowColor = new Color(
          (float) LXUtils.constrain(0.6 + 0.4 * segmentAlpha, 0, 1),
          (float) LXUtils.constrain(0.8 + 0.2 * segmentAlpha, 0, 1),
          (float) LXUtils.constrain(1.0, 0, 1),
          (float) LXUtils.constrain(segmentAlpha * 0.3, 0, 1)
        );
        graphics.setColor(glowColor);
        graphics.setStroke(new BasicStroke(strokeWidth * 2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(path);
      }
    }
  }

  @Override
  public void noteOnReceived(MidiNoteOn midiNote) {
    trig();
  }

  @Override
  public void buildDeviceControls(UI ui, UIDevice uiDevice, Lightning lightning) {
    uiDevice.setLayout(UIDevice.Layout.HORIZONTAL, 6);
    
    addColumn(uiDevice, "Trigger",
      newButton(lightning.trig).setTriggerable(true).setBorderRounding(4),
      newDropMenu(lightning.algorithm).setTopMargin(6),
      newKnob(lightning.intensity).setTopMargin(6)
    ).setChildSpacing(6);

    addVerticalBreak(ui, uiDevice);

    addColumn(uiDevice, "Midpoint",
      newKnob(lightning.displacement),
      newKnob(lightning.recursionDepth),
      newKnob(lightning.startSpread),
      newKnob(lightning.endSpread)
    ).setChildSpacing(6);

    addVerticalBreak(ui, uiDevice);

    addColumn(uiDevice, "M-Branching",
      newKnob(lightning.branchProbability),
      newKnob(lightning.branchDistance),
      newKnob(lightning.branchAngle)
    ).setChildSpacing(6);

    addVerticalBreak(ui, uiDevice);

    addColumn(uiDevice, "L-System",
      newKnob(lightning.lsIterations),
      newKnob(lightning.lsSegmentLength),
      newKnob(lightning.lsBranchAngle)
    ).setChildSpacing(6);

    addVerticalBreak(ui, uiDevice);

    addColumn(uiDevice, "LS-Variation",
      newKnob(lightning.lsAngleVariation),
      newKnob(lightning.lsLengthVariation)
    ).setChildSpacing(6);

    addVerticalBreak(ui, uiDevice);

    addColumn(uiDevice, "Visual",
      newKnob(lightning.fadeTime),
      newKnob(lightning.thickness)
    ).setChildSpacing(6);
    
    addVerticalBreak(ui, uiDevice);
    
    addColumn(uiDevice, "Faces",
      buildFaceControls(ui, uiDevice, 80)
    );
  }
}