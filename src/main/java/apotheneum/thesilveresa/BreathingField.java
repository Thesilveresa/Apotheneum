package apotheneum.thesilveresa;

import apotheneum.ApotheneumPattern;
import apotheneum.Apotheneum;
import apotheneum.Apotheneum.Cube;
import apotheneum.Apotheneum.Cube.Face;
import apotheneum.Apotheneum.Cube.Row;
import apotheneum.Apotheneum.Cylinder;
import apotheneum.Apotheneum.Cylinder.Ring;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponentName;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;

@LXCategory("Apotheneum/thesilveresa")
@LXComponentName("Breathing Field")
public class BreathingField extends ApotheneumPattern {

  private final CompoundParameter breathRate = new CompoundParameter("Rate", 0.4, 0.1, 1.2)
    .setDescription("Breathing frequency");
  private final CompoundParameter amplitude = new CompoundParameter("Depth", 0.8, 0.2, 1.5)
    .setDescription("Breath amplitude");
  private final CompoundParameter waveSpread = new CompoundParameter("Spread", 3.5, 1.0, 8.0)
    .setDescription("Harmonic wave spread");
  private final CompoundParameter centerX = new CompoundParameter("CenterX", 0.0, -0.5, 0.5)
    .setDescription("Breathing center X");
  private final CompoundParameter centerY = new CompoundParameter("CenterY", 0.0, -0.5, 0.5)
    .setDescription("Breathing center Y");
  private final CompoundParameter brightness = new CompoundParameter("Bright", 75.0, 20.0, 100.0)
    .setDescription("Overall brightness");
  private final CompoundParameter hueShift = new CompoundParameter("Hue", 200.0, 0.0, 360.0)
    .setDescription("Base hue");

  private float time = 0f;
  private int[] exteriorCache;
  private int[] interiorCache;
  
  // Pre-computed lookup tables for performance
  private static final int LOOKUP_SIZE = 1024;
  private static final float[] SIN_LOOKUP = new float[LOOKUP_SIZE];
  private static final float[] COS_LOOKUP = new float[LOOKUP_SIZE];
  
  static {
    for (int i = 0; i < LOOKUP_SIZE; i++) {
      float angle = (float)(2.0 * Math.PI * i / LOOKUP_SIZE);
      SIN_LOOKUP[i] = (float)Math.sin(angle);
      COS_LOOKUP[i] = (float)Math.cos(angle);
    }
  }
  
  // Soft breathing colors - peaceful transition palette
  private static final float[] BREATH_HUES = { 200f, 220f, 240f, 180f, 160f };

  public BreathingField(LX lx) {
    super(lx);
    addParameter("Rate", this.breathRate);
    addParameter("Depth", this.amplitude);
    addParameter("Spread", this.waveSpread);
    addParameter("CenterX", this.centerX);
    addParameter("CenterY", this.centerY);
    addParameter("Bright", this.brightness);
    addParameter("Hue", this.hueShift);
  }

  @Override
  protected void render(double deltaMs) {
    time += (float)(deltaMs / 1000.0) * breathRate.getValuef();
    
    // Render cube with caching optimization
    Cube cube = Apotheneum.cube;
    if (cube != null) {
      // Compute exterior pattern once
      Face referenceFace = cube.exterior.faces[0];
      computeExteriorPattern(referenceFace);
      
      // Copy to all exterior faces
      for (Face face : cube.exterior.faces) {
        copyFromExteriorCache(face);
      }
      
      // Interior uses same pattern with phase shift
      if (cube.interior != null) {
        computeInteriorPattern(cube.interior.faces[0]);
        for (Face face : cube.interior.faces) {
          copyFromInteriorCache(face);
        }
      }
    }
    
    Cylinder cylinder = Apotheneum.cylinder;
    if (cylinder != null) {
      processCylinder(cylinder);
    }
  }

  private void computeExteriorPattern(Face face) {
    int cols = face.columns.length;
    int rows = face.rows.length;
    
    if (exteriorCache == null || exteriorCache.length != cols * rows) {
      exteriorCache = new int[cols * rows];
    }
    
    computeFacePattern(face, exteriorCache, 0f);
  }
  
  private void computeInteriorPattern(Face face) {
    int cols = face.columns.length;
    int rows = face.rows.length;
    
    if (interiorCache == null || interiorCache.length != cols * rows) {
      interiorCache = new int[cols * rows];
    }
    
    // Interior breathes in counter-phase
    computeFacePattern(face, interiorCache, (float)Math.PI);
  }

  private void computeFacePattern(Face face, int[] cache, float phaseOffset) {
    int cols = face.columns.length;
    int rows = face.rows.length;
    float invCols = 1.0f / Math.max(1, cols - 1);
    float invRows = 1.0f / Math.max(1, rows - 1);
    
    float cx = centerX.getValuef();
    float cy = centerY.getValuef();
    float spread = waveSpread.getValuef();
    float amp = amplitude.getValuef();
    
    // Global breath phase
    float globalBreath = fastSin(time + phaseOffset) * amp;
    
    int cacheIndex = 0;
    for (int rowIdx = 0; rowIdx < rows; rowIdx++) {
      for (int colIdx = 0; colIdx < cols; colIdx++) {
        float u = colIdx * invCols - 0.5f;
        float v = rowIdx * invRows - 0.5f;
        
        cache[cacheIndex++] = calculateBreathingColor(u - cx, v - cy, spread, globalBreath);
      }
    }
  }
  
  private void copyFromExteriorCache(Face face) {
    int cols = face.columns.length;
    int cacheIndex = 0;
    
    for (Row row : face.rows) {
      for (int cx = 0; cx < cols; cx++) {
        LXPoint p = row.points[cx];
        colors[p.index] = exteriorCache[cacheIndex++];
      }
    }
  }
  
  private void copyFromInteriorCache(Face face) {
    int cols = face.columns.length;
    int cacheIndex = 0;
    
    for (Row row : face.rows) {
      for (int cx = 0; cx < cols; cx++) {
        LXPoint p = row.points[cx];
        colors[p.index] = interiorCache[cacheIndex++];
      }
    }
  }

  private void processCylinder(Cylinder cylinder) {
    // Exterior and interior use same logic with different phase
    processCylinderOrientation(cylinder.exterior, 0f);
    
    if (cylinder.interior != null) {
      processCylinderOrientation(cylinder.interior, (float)Math.PI);
    }
  }
  
  private void processCylinderOrientation(Cylinder.Orientation orientation, float phaseOffset) {
    Ring[] rings = orientation.rings;
    int numRings = rings.length;
    
    float cx = centerX.getValuef();
    float cy = centerY.getValuef();
    float spread = waveSpread.getValuef();
    float amp = amplitude.getValuef();
    
    float globalBreath = fastSin(time + phaseOffset) * amp;

    for (int ringIndex = 0; ringIndex < numRings; ringIndex++) {
      Ring ring = rings[ringIndex];
      int pointsPerRing = ring.points.length;
      
      float v = (float)ringIndex / Math.max(1, numRings - 1) - 0.5f;

      for (int pointIndex = 0; pointIndex < pointsPerRing; pointIndex++) {
        LXPoint p = ring.points[pointIndex];
        float u = (float)pointIndex / pointsPerRing - 0.5f;
        
        int color = calculateBreathingColor(u - cx, v - cy, spread, globalBreath);
        colors[p.index] = color;
      }
    }
  }
  
  private int calculateBreathingColor(float x, float y, float spread, float globalBreath) {
    // Distance from breathing center
    float r = (float)Math.sqrt(x*x + y*y);
    
    // Harmonic waves emanating from center with breathing modulation
    float wavePhase = r * spread - time * 2f;
    
    // Multiple harmonic layers for richer breathing field
    float breath1 = fastSin(wavePhase) * (0.6f + 0.4f * globalBreath);
    float breath2 = fastSin(wavePhase * 0.7f + time * 0.5f) * (0.3f + 0.2f * globalBreath);
    float breath3 = fastSin(wavePhase * 1.3f - time * 0.3f) * (0.2f + 0.1f * globalBreath);
    
    // Combine harmonics
    float combinedBreath = breath1 + breath2 * 0.6f + breath3 * 0.4f;
    
    // Soft falloff from center
    float centerFalloff = 1f / (1f + r * r * 2f);
    float intensity = (combinedBreath + 1f) * 0.5f * centerFalloff;
    
    if (intensity < 0.05f) return 0;
    
    // Color selection based on breathing phase and position
    float hueIndex = (globalBreath + 1f) * 0.5f * BREATH_HUES.length + r * 2f;
    // Ensure hueIndex is non-negative
    hueIndex = Math.max(0, hueIndex);
    int idx = ((int)hueIndex) % BREATH_HUES.length;
    // Ensure idx is non-negative
    if (idx < 0) idx += BREATH_HUES.length;
    float baseHue = BREATH_HUES[idx];
    
    // Subtle hue modulation with breathing
    float breathingHue = (baseHue + hueShift.getValuef() + globalBreath * 15f) % 360f;
    if (breathingHue < 0) breathingHue += 360f;
    
    // Soft saturation that breathes
    float saturation = 60f + 30f * (globalBreath + 1f) * 0.5f;
    
    // Brightness pulses gently with breathing
    float finalBrightness = Math.min(intensity * brightness.getValuef() * (0.7f + 0.3f * (globalBreath + 1f) * 0.5f), 100f);
    
    return LXColor.hsb(breathingHue, saturation, finalBrightness);
  }
  
  // Fast lookup table trigonometry
  private float fastSin(float angle) {
    // Normalize angle to [0, 2π) range
    while (angle < 0) angle += (float)(2.0 * Math.PI);
    while (angle >= (float)(2.0 * Math.PI)) angle -= (float)(2.0 * Math.PI);
    
    int index = (int)(angle / (2.0 * Math.PI) * LOOKUP_SIZE);
    return SIN_LOOKUP[index % LOOKUP_SIZE];
  }
  
  private float fastCos(float angle) {
    // Normalize angle to [0, 2π) range
    while (angle < 0) angle += (float)(2.0 * Math.PI);
    while (angle >= (float)(2.0 * Math.PI)) angle -= (float)(2.0 * Math.PI);
    
    int index = (int)(angle / (2.0 * Math.PI) * LOOKUP_SIZE);
    return COS_LOOKUP[index % LOOKUP_SIZE];
  }
}
