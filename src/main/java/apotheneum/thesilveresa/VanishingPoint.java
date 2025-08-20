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
@LXComponentName("Vanishing Point")
public class VanishingPoint extends ApotheneumPattern {

  private final CompoundParameter convergence = new CompoundParameter("Converge", 0.0, 0.0, 1.0)
    .setDescription("Convergence to singularity");
  private final CompoundParameter singularityX = new CompoundParameter("CenterX", 0.0, -0.5, 0.5)
    .setDescription("Convergence center X");
  private final CompoundParameter singularityY = new CompoundParameter("CenterY", 0.0, -0.5, 0.5)
    .setDescription("Convergence center Y");
  private final CompoundParameter fadeSpeed = new CompoundParameter("Fade", 0.8, 0.1, 2.0)
    .setDescription("Fade animation speed");
  private final CompoundParameter finalGlow = new CompoundParameter("Glow", 95.0, 0.0, 100.0)
    .setDescription("Final pixel glow intensity");
  private final CompoundParameter pullStrength = new CompoundParameter("Pull", 1.5, 0.5, 3.0)
    .setDescription("Gravitational pull strength");
  private final CompoundParameter chromaShift = new CompoundParameter("Chroma", 0.3, 0.0, 1.0)
    .setDescription("Chromatic aberration");

  private float time = 0f;
  private int[] exteriorCache;
  private int[] cylinderCache;
  private boolean useSamePattern = true; // Interior uses same pattern as exterior for efficiency
  
  // Minimal color palette for final convergence
  private static final float[] CONVERGENCE_HUES = { 240f, 260f, 280f, 300f, 320f }; // Deep blues to purples
  private static final float SINGULARITY_HUE = 45f; // Golden singularity
  
  // Pre-computed distance falloff curve
  private static final int FALLOFF_TABLE_SIZE = 256;
  private static final float[] DISTANCE_FALLOFF = new float[FALLOFF_TABLE_SIZE];
  
  // Angle lookup table to replace atan2
  private static final int ANGLE_TABLE_SIZE = 128;
  private static final float[] ANGLE_LOOKUP = new float[ANGLE_TABLE_SIZE * ANGLE_TABLE_SIZE];
  
  static {
    // Initialize distance falloff table
    for (int i = 0; i < FALLOFF_TABLE_SIZE; i++) {
      float t = (float)i / (FALLOFF_TABLE_SIZE - 1);
      DISTANCE_FALLOFF[i] = 1f - t * t * t;
    }
    
    // Initialize angle lookup table
    float scale = (ANGLE_TABLE_SIZE - 1) / 1.0f; // Map [-0.5, 0.5] to [0, ANGLE_TABLE_SIZE-1]
    for (int yIdx = 0; yIdx < ANGLE_TABLE_SIZE; yIdx++) {
      for (int xIdx = 0; xIdx < ANGLE_TABLE_SIZE; xIdx++) {
        float x = (xIdx / scale) - 0.5f;
        float y = (yIdx / scale) - 0.5f;
        ANGLE_LOOKUP[yIdx * ANGLE_TABLE_SIZE + xIdx] = (float)Math.atan2(y, x);
      }
    }
  }

  public VanishingPoint(LX lx) {
    super(lx);
    addParameter("Converge", this.convergence);
    addParameter("CenterX", this.singularityX);
    addParameter("CenterY", this.singularityY);
    addParameter("Fade", this.fadeSpeed);
    addParameter("Glow", this.finalGlow);
    addParameter("Pull", this.pullStrength);
    addParameter("Chroma", this.chromaShift);
  }

  @Override
  protected void render(double deltaMs) {
    time += (float)(deltaMs / 1000.0) * fadeSpeed.getValuef();
    
    // Render cube with maximum efficiency
    Cube cube = Apotheneum.cube;
    if (cube != null) {
      Face referenceFace = cube.exterior.faces[0];
      computePattern(referenceFace);
      
      // Copy to all faces (exterior and interior)
      for (Face face : cube.exterior.faces) {
        copyFromCache(face, exteriorCache);
      }
      if (cube.interior != null) {
        for (Face face : cube.interior.faces) {
          copyFromCache(face, exteriorCache);
        }
      }
    }
    
    Cylinder cylinder = Apotheneum.cylinder;
    if (cylinder != null) {
      // Compute pattern once for exterior and cache
      computeCylinderPattern(cylinder.exterior);
      processCylinderOrientation(cylinder.exterior, cylinderCache);
      if (cylinder.interior != null) {
        processCylinderOrientation(cylinder.interior, cylinderCache);
      }
    }
  }

  private void computePattern(Face face) {
    int cols = face.columns.length;
    int rows = face.rows.length;
    
    if (exteriorCache == null || exteriorCache.length != cols * rows) {
      exteriorCache = new int[cols * rows];
    }
    
    computeFacePattern(face, exteriorCache);
  }

  private void computeFacePattern(Face face, int[] cache) {
    int cols = face.columns.length;
    int rows = face.rows.length;
    float invCols = 1.0f / Math.max(1, cols - 1);
    float invRows = 1.0f / Math.max(1, rows - 1);
    
    float sx = singularityX.getValuef();
    float sy = singularityY.getValuef();
    float converge = convergence.getValuef();
    float pull = pullStrength.getValuef();
    float chroma = chromaShift.getValuef();
    
    int cacheIndex = 0;
    for (int rowIdx = 0; rowIdx < rows; rowIdx++) {
      for (int colIdx = 0; colIdx < cols; colIdx++) {
        float u = colIdx * invCols - 0.5f;
        float v = rowIdx * invRows - 0.5f;
        
        cache[cacheIndex++] = calculateConvergenceColor(u - sx, v - sy, converge, pull, chroma);
      }
    }
  }
  
  private void copyFromCache(Face face, int[] cache) {
    int cols = face.columns.length;
    int cacheIndex = 0;
    
    for (Row row : face.rows) {
      for (int cx = 0; cx < cols; cx++) {
        LXPoint p = row.points[cx];
        colors[p.index] = cache[cacheIndex++];
      }
    }
  }
  
  private void computeCylinderPattern(Cylinder.Orientation orientation) {
    Ring[] rings = orientation.rings;
    int numRings = rings.length;
    int pointsPerRing = rings[0].points.length; // Assume uniform rings
    
    if (cylinderCache == null || cylinderCache.length != numRings * pointsPerRing) {
      cylinderCache = new int[numRings * pointsPerRing];
    }
    
    float sx = singularityX.getValuef();
    float sy = singularityY.getValuef();
    float converge = convergence.getValuef();
    float pull = pullStrength.getValuef();
    float chroma = chromaShift.getValuef();
    
    int cacheIndex = 0;
    for (int ringIndex = 0; ringIndex < numRings; ringIndex++) {
      float v = (float)ringIndex / Math.max(1, numRings - 1) - 0.5f;
      for (int pointIndex = 0; pointIndex < pointsPerRing; pointIndex++) {
        float u = (float)pointIndex / pointsPerRing - 0.5f;
        cylinderCache[cacheIndex++] = calculateConvergenceColor(u - sx, v - sy, converge, pull, chroma);
      }
    }
  }
  
  private void processCylinderOrientation(Cylinder.Orientation orientation, int[] cache) {
    Ring[] rings = orientation.rings;
    int numRings = rings.length;
    int cacheIndex = 0;
    
    for (int ringIndex = 0; ringIndex < numRings; ringIndex++) {
      Ring ring = rings[ringIndex];
      int pointsPerRing = ring.points.length;
      for (int pointIndex = 0; pointIndex < pointsPerRing; pointIndex++) {
        LXPoint p = ring.points[pointIndex];
        colors[p.index] = cache[cacheIndex++];
      }
    }
  }
  
  private int calculateConvergenceColor(float x, float y, float converge, float pull, float chroma) {
    // Distance from singularity point
    float r = (float)Math.sqrt(x*x + y*y);
    
    // As convergence approaches 1.0, everything gets pulled toward center
    float convergencePull = converge * pull;
    
    // Gravitational compression - space contracts toward singularity
    float compressedR = r / (1f + convergencePull * (1f - r));
    
    // Radius threshold for visibility - shrinks as convergence increases
    float visibilityRadius = 0.8f * (1f - converge * 0.95f);
    
    if (compressedR > visibilityRadius) {
      return 0; // Beyond event horizon, nothing visible
    }
    
    // Distance-based intensity using lookup table
    int falloffIndex = (int)(compressedR / visibilityRadius * (FALLOFF_TABLE_SIZE - 1));
    falloffIndex = Math.max(0, Math.min(falloffIndex, FALLOFF_TABLE_SIZE - 1));
    float distanceIntensity = DISTANCE_FALLOFF[falloffIndex];
    
    // Time-based shimmer effect using linear approximation
    float shimmer = 1f + 0.3f * fastTriangleWave(time * 8f + compressedR * 20f) * converge;
    
    // Singularity influence for bright center
    float singularityDistance = Math.max(0f, compressedR);
    float singularityInfluence = 1f / (1f + singularityDistance * 50f * (1f + converge * 10f));
    
    // Total intensity
    float totalIntensity = (distanceIntensity * shimmer + singularityInfluence * converge) * (1f - converge * 0.3f);
    
    if (totalIntensity < 0.02f) return 0;
    
    // Color selection: distant points use convergence hues, center approaches singularity hue
    float hueBlend = singularityInfluence * converge;
    float baseHue;
    
    if (hueBlend > 0.7f) {
      // Near singularity - golden glow
      baseHue = SINGULARITY_HUE;
    } else {
      // Distant points - convergence palette using angle lookup
      int xIdx = (int)((x + 0.5f) * (ANGLE_TABLE_SIZE - 1));
      int yIdx = (int)((y + 0.5f) * (ANGLE_TABLE_SIZE - 1));
      xIdx = Math.max(0, Math.min(xIdx, ANGLE_TABLE_SIZE - 1));
      yIdx = Math.max(0, Math.min(yIdx, ANGLE_TABLE_SIZE - 1));
      float angle = ANGLE_LOOKUP[yIdx * ANGLE_TABLE_SIZE + xIdx];
      int hueIndex = (int)((angle / (2f * (float)Math.PI) + 1f + time * 0.1f) * CONVERGENCE_HUES.length) % CONVERGENCE_HUES.length;
      if (hueIndex < 0) hueIndex += CONVERGENCE_HUES.length;
      baseHue = CONVERGENCE_HUES[hueIndex];
      
      // Blend toward singularity hue
      if (hueBlend > 0.3f) {
        float blendFactor = (hueBlend - 0.3f) / 0.4f;
        baseHue = baseHue + (SINGULARITY_HUE - baseHue) * blendFactor;
      }
    }
    
    // Chromatic aberration effect
    if (chroma > 0.01f && r > 0.01f) {
      float aberration = chroma * compressedR * 30f;
      baseHue = (baseHue + aberration) % 360f;
      if (baseHue < 0) baseHue += 360f;
    }
    
    // Saturation: high near singularity, fades with distance
    float saturation = 90f * (0.3f + 0.7f * (1f - compressedR / visibilityRadius)) * (1f - converge * 0.2f);
    
    // Brightness: final glow parameter controls singularity brightness
    float brightness;
    if (singularityInfluence > 0.5f) {
      brightness = finalGlow.getValuef() * singularityInfluence * (0.8f + 0.2f * shimmer);
    } else {
      brightness = totalIntensity * 60f * (1f + converge * 0.5f);
    }
    
    brightness = Math.min(brightness, 100f);
    
    return LXColor.hsb(baseHue, saturation, brightness);
  }
  
  // Fast triangle wave approximation using modulo
  private float fastTriangleWave(float t) {
    t = t % 1f; // Normalize to [0,1]
    if (t < 0) t += 1f;
    return (t < 0.5f) ? (4f * t - 1f) : (3f - 4f * t);
  }
}
