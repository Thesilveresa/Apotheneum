package apotheneum.doved.patterns;

import apotheneum.Apotheneum;
import apotheneum.ApotheneumPattern;
import apotheneum.doved.lightning.RRT3DAlgorithm;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponent;
import heronarts.lx.LXComponentName;
import heronarts.lx.color.LXColor;
import heronarts.lx.midi.MidiNoteOn;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.TriggerParameter;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import heronarts.lx.utils.LXUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@LXCategory("Apotheneum/doved")
@LXComponentName("Lightning-RRT")
@LXComponent.Description("3D Lightning using RRT algorithm in 3D space")
public class LightningRRT extends ApotheneumPattern implements ApotheneumPattern.Midi, UIDeviceControls<LightningRRT> {

  public final TriggerParameter trig =
    new TriggerParameter("Trig", this::trig)
    .setDescription("Trigger a lightning strike");

  public final DiscreteParameter target =
    new DiscreteParameter("Target", new String[] {"Cube", "Cylinder", "Both"}, 2)
    .setDescription("Which geometry to target");

  public final CompoundParameter intensity =
    new CompoundParameter("Intensity", 1)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Overall brightness of lightning");

  public final CompoundParameter fadeTime =
    new CompoundParameter("Fade", 3, 0.5, 10)
    .setDescription("Duration for lightning trails to fade (seconds)");

  public final CompoundParameter thickness =
    new CompoundParameter("Thickness", 8, 2, 20)
    .setDescription("3D thickness for proximity calculations");

  // RRT specific parameters
  public final CompoundParameter stepSize =
    new CompoundParameter("Step Size", 15, 5, 30)
    .setDescription("Distance of each RRT tree extension");

  public final CompoundParameter goalBias =
    new CompoundParameter("Goal Bias", 0.15, 0, 0.4)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Probability of sampling from goal region");

  public final CompoundParameter maxIterations =
    new CompoundParameter("Max Iter", 120, 50, 250)
    .setDescription("Maximum number of RRT tree extensions");

  public final CompoundParameter branchProbability =
    new CompoundParameter("Branch Prob", 0.25, 0, 0.5)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Likelihood of creating branches");

  public final CompoundParameter jaggedness =
    new CompoundParameter("Jaggedness", 0.4, 0, 1)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Amount of random displacement for jagged appearance");

  public final CompoundParameter goalRadius =
    new CompoundParameter("Goal Radius", 25, 10, 50)
    .setDescription("Size of target region considered reached");

  public final CompoundParameter electricalField =
    new CompoundParameter("Elec Field", 0.6, 0, 1)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Electrical field strength for biased sampling");

  private static class LightningBolt3D {
    public final List<RRT3DAlgorithm.LightningSegment3D> segments = new ArrayList<>();
    public final double startTime;
    public final double fadeTimeMs;

    public LightningBolt3D(double startTime, double fadeTimeMs) {
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

  private final List<LightningBolt3D> bolts = new ArrayList<>();

  public LightningRRT(LX lx) {
    super(lx);
    addParameter("trig", this.trig);
    addParameter("target", this.target);
    addParameter("intensity", this.intensity);
    addParameter("fadeTime", this.fadeTime);
    addParameter("thickness", this.thickness);
    addParameter("stepSize", this.stepSize);
    addParameter("goalBias", this.goalBias);
    addParameter("maxIterations", this.maxIterations);
    addParameter("branchProbability", this.branchProbability);
    addParameter("jaggedness", this.jaggedness);
    addParameter("goalRadius", this.goalRadius);
    addParameter("electricalField", this.electricalField);
  }

  private void trig() {
    double currentTime = System.currentTimeMillis();
    double fadeTimeMs = fadeTime.getValue() * 1000;
    LightningBolt3D bolt = new LightningBolt3D(currentTime, fadeTimeMs);
    
    generateRRTLightning3D(bolt);
    
    synchronized (bolts) {
      bolts.add(bolt);
    }
  }
  
  private void generateRRTLightning3D(LightningBolt3D bolt) {
    // Determine 3D bounds based on target
    double minX, maxX, minY, maxY, minZ, maxZ;
    
    int targetValue = target.getValuei();
    if (targetValue == 0) { // Cube only
      minX = -25;
      maxX = 25;
      minY = -25;
      maxY = 25;
      minZ = -25;
      maxZ = 25;
    } else if (targetValue == 1) { // Cylinder only
      minX = -15;
      maxX = 15;
      minY = -22;
      maxY = 22;
      minZ = -15;
      maxZ = 15;
    } else { // Both - use larger bounds
      minX = -25;
      maxX = 25;
      minY = -25;
      maxY = 25;
      minZ = -25;
      maxZ = 25;
    }
    
    RRT3DAlgorithm.Parameters params = new RRT3DAlgorithm.Parameters(
      stepSize.getValue(),
      goalBias.getValue(),
      (int) maxIterations.getValue(),
      branchProbability.getValue(),
      jaggedness.getValue(),
      goalRadius.getValue(),
      electricalField.getValue(),
      minX, maxX, minY, maxY, minZ, maxZ
    );
    
    RRT3DAlgorithm.generateLightning3D(bolt.segments, params);
  }

  @Override
  public void render(double deltaMs) {
    setColors(LXColor.BLACK);
    
    double currentTime = System.currentTimeMillis();
    
    synchronized (bolts) {
      Iterator<LightningBolt3D> iterator = bolts.iterator();
      while (iterator.hasNext()) {
        LightningBolt3D bolt = iterator.next();
        
        if (bolt.isExpired(currentTime)) {
          iterator.remove();
          continue;
        }
        
        double fadeAmount = bolt.getFadeAmount(currentTime);
        renderBolt3D(bolt, fadeAmount);
      }
    }
  }

  private void renderBolt3D(LightningBolt3D bolt, double fadeAmount) {
    double alpha = fadeAmount * intensity.getValue();
    double thicknessValue = thickness.getValue();
    
    // Use cylinder-based rendering similar to OrboxPattern
    renderBoltWithCylinders(bolt, alpha, thicknessValue);
  }
  
  private void renderBoltWithCylinders(LightningBolt3D bolt, double alpha, double thicknessValue) {
    // Render each segment as a cylinder
    for (RRT3DAlgorithm.LightningSegment3D segment : bolt.segments) {
      renderLightningCylinder(segment, alpha, thicknessValue);
    }
  }
  
  private void renderLightningCylinder(RRT3DAlgorithm.LightningSegment3D segment, double alpha, double thicknessValue) {
    // Calculate segment properties
    double dx = segment.x2 - segment.x1;
    double dy = segment.y2 - segment.y1;
    double dz = segment.z2 - segment.z1;
    double segmentLength = Math.sqrt(dx * dx + dy * dy + dz * dz);
    
    if (segmentLength < 1e-6) return;
    
    // Normalize direction
    dx /= segmentLength;
    dy /= segmentLength;
    dz /= segmentLength;
    
    // Calculate segment center
    double centerX = (segment.x1 + segment.x2) / 2.0;
    double centerY = (segment.y1 + segment.y2) / 2.0;
    double centerZ = (segment.z1 + segment.z2) / 2.0;
    
    // Render to appropriate geometries
    if (target.getValuei() == 0 || target.getValuei() == 2) {
      renderCylinderToCube(segment, centerX, centerY, centerZ, dx, dy, dz, segmentLength, alpha, thicknessValue);
    }
    
    if (target.getValuei() == 1 || target.getValuei() == 2) {
      renderCylinderToCylinder(segment, centerX, centerY, centerZ, dx, dy, dz, segmentLength, alpha, thicknessValue);
    }
  }
  
  private void renderCylinderToCube(RRT3DAlgorithm.LightningSegment3D segment, double centerX, double centerY, double centerZ, 
                                   double dx, double dy, double dz, double segmentLength, double alpha, double thicknessValue) {
    // Iterate through all cube faces
    for (int face = 0; face < 4; face++) {
      for (int col = 0; col < Apotheneum.GRID_WIDTH; col++) {
        for (int row = 0; row < Apotheneum.GRID_HEIGHT; row++) {
          LXPoint point = Apotheneum.cube.exterior.faces[face].columns[col].points[row];
          double brightness = calculateCylinderBrightness(point.x, point.y, point.z, 
                                                         centerX, centerY, centerZ, 
                                                         dx, dy, dz, segmentLength, 
                                                         segment, alpha, thicknessValue);
          
          if (brightness > 0) {
            int color = LXColor.hsb(
              220 + (float)(Math.random() * 20 - 10), // Blue-white with slight variation
              (float)(20 + Math.random() * 10), // Low saturation
              (float)(brightness * 100)
            );
            setColor(point.index, LXColor.lerp(getColor(point.index), color, (float)brightness));
          }
        }
      }
    }
  }
  
  private void renderCylinderToCylinder(RRT3DAlgorithm.LightningSegment3D segment, double centerX, double centerY, double centerZ, 
                                       double dx, double dy, double dz, double segmentLength, double alpha, double thicknessValue) {
    // Iterate through all cylinder columns
    for (int col = 0; col < Apotheneum.RING_LENGTH; col++) {
      for (int row = 0; row < Apotheneum.CYLINDER_HEIGHT; row++) {
        LXPoint point = Apotheneum.cylinder.exterior.columns[col].points[row];
        double brightness = calculateCylinderBrightness(point.x, point.y, point.z, 
                                                       centerX, centerY, centerZ, 
                                                       dx, dy, dz, segmentLength, 
                                                       segment, alpha, thicknessValue);
        
        if (brightness > 0) {
          int color = LXColor.hsb(
            215 + (float)(Math.random() * 25 - 12), // Blue-white with slight variation
            (float)(15 + Math.random() * 10), // Low saturation
            (float)(brightness * 100)
          );
          setColor(point.index, LXColor.lerp(getColor(point.index), color, (float)brightness));
        }
      }
    }
  }
  
  private double calculateCylinderBrightness(double px, double py, double pz, 
                                           double centerX, double centerY, double centerZ,
                                           double dx, double dy, double dz, double segmentLength,
                                           RRT3DAlgorithm.LightningSegment3D segment, double alpha, double thicknessValue) {
    // Vector from segment center to point
    double vx = px - centerX;
    double vy = py - centerY;
    double vz = pz - centerZ;
    
    // Project point onto segment axis
    double projection = vx * dx + vy * dy + vz * dz;
    
    // Check if point is within segment bounds
    if (Math.abs(projection) > segmentLength / 2.0) {
      return 0; // Point is outside segment
    }
    
    // Calculate perpendicular distance to segment axis
    double projX = centerX + projection * dx;
    double projY = centerY + projection * dy;
    double projZ = centerZ + projection * dz;
    
    double perpDistance = Math.sqrt(
      (px - projX) * (px - projX) +
      (py - projY) * (py - projY) +
      (pz - projZ) * (pz - projZ)
    );
    
    // Calculate brightness based on distance
    if (perpDistance > thicknessValue) {
      return 0;
    }
    
    double normalizedDistance = perpDistance / thicknessValue;
    double falloff = 1.0 - normalizedDistance;
    
    // Apply segment intensity
    double brightness = falloff * segment.intensity * alpha;
    
    // Branches are slightly dimmer
    if (segment.isBranch) {
      brightness *= 0.8;
    }
    
    // Add glow effect for very close points
    if (normalizedDistance < 0.3) {
      brightness *= 1.2;
    }
    
    return LXUtils.constrain(brightness, 0, 1);
  }

  @Override
  public void noteOnReceived(MidiNoteOn midiNote) {
    trig();
  }

  @Override
  public void buildDeviceControls(UI ui, UIDevice uiDevice, LightningRRT lightning) {
    uiDevice.setLayout(UIDevice.Layout.HORIZONTAL, 6);
    
    addColumn(uiDevice, "Trigger",
      newButton(lightning.trig).setTriggerable(true).setBorderRounding(4),
      newDropMenu(lightning.target).setTopMargin(6),
      newKnob(lightning.intensity).setTopMargin(6)
    ).setChildSpacing(6);

    addVerticalBreak(ui, uiDevice);

    addColumn(uiDevice, "RRT-Core",
      newKnob(lightning.stepSize),
      newKnob(lightning.goalBias),
      newKnob(lightning.maxIterations)
    ).setChildSpacing(6);

    addVerticalBreak(ui, uiDevice);

    addColumn(uiDevice, "RRT-Shape",
      newKnob(lightning.branchProbability),
      newKnob(lightning.jaggedness),
      newKnob(lightning.goalRadius)
    ).setChildSpacing(6);

    addVerticalBreak(ui, uiDevice);

    addColumn(uiDevice, "RRT-Field",
      newKnob(lightning.electricalField)
    ).setChildSpacing(6);

    addVerticalBreak(ui, uiDevice);

    addColumn(uiDevice, "Visual",
      newKnob(lightning.fadeTime),
      newKnob(lightning.thickness)
    ).setChildSpacing(6);
  }
}