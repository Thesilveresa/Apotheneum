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
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;

@LXCategory("Apotheneum/thesilveresa")
@LXComponentName("Vortex Inversion")
public class VortexInversion extends ApotheneumPattern {

  private final CompoundParameter speed = new CompoundParameter("Speed", 0.6, 0.1, 2.5)
    .setDescription("Vortex rotation speed");
  private final CompoundParameter gridScale = new CompoundParameter("Grid", 8.0, 2.0, 16.0)
    .setDescription("Grid density before inversion");
  private final CompoundParameter singularity = new CompoundParameter("Singular", 0.1, 0.01, 0.5)
    .setDescription("Singularity core size");
  private final CompoundParameter warpStrength = new CompoundParameter("Warp", 1.2, 0.5, 3.0)
    .setDescription("Spatial distortion intensity");
  private final CompoundParameter centerX = new CompoundParameter("CenterX", 0.0, -0.5, 0.5)
    .setDescription("Vortex center X position");
  private final CompoundParameter centerY = new CompoundParameter("CenterY", 0.0, -0.5, 0.5)
    .setDescription("Vortex center Y position");
  private final BooleanParameter invert = new BooleanParameter("Invert", false)
    .setDescription("Reverse vortex direction");
  private final CompoundParameter brightness = new CompoundParameter("Bright", 80.0, 30.0, 100.0)
    .setDescription("Overall brightness");
  private final CompoundParameter sat = new CompoundParameter("Sat", 75.0, 20.0, 100.0)
    .setDescription("Color saturation");

  private float time = 0f;
  private int[] exteriorCache;
  private int[] interiorCache;
  
  // Pre-computed spiral constants for performance
  private static final float GOLDEN_ANGLE = 2.39996322972865332f; // 2π/φ²
  private static final float SPIRAL_FACTOR = 0.61803398875f; // 1/φ
  
  // Event horizon colors - deep space to singularity
  private static final float[] HORIZON_HUES = { 240f, 280f, 320f, 20f, 60f };
  private static final float[] GRID_HUES = { 180f, 200f, 220f, 160f, 140f };

  public VortexInversion(LX lx) {
    super(lx);
    addParameter("Speed", this.speed);
    addParameter("Grid", this.gridScale);
    addParameter("Singular", this.singularity);
    addParameter("Warp", this.warpStrength);
    addParameter("CenterX", this.centerX);
    addParameter("CenterY", this.centerY);
    addParameter("Invert", this.invert);
    addParameter("Bright", this.brightness);
    addParameter("Sat", this.sat);
  }

  @Override
  protected void render(double deltaMs) {
    time += (float)(deltaMs / 1000.0) * speed.getValuef();
    
    // Render cube with dual caching for exterior/interior
    Cube cube = Apotheneum.cube;
    if (cube != null) {
      // Compute exterior pattern
      Face referenceFace = cube.exterior.faces[0];
      computeExteriorPattern(referenceFace);
      
      // Copy to all exterior faces
      for (Face face : cube.exterior.faces) {
        copyFromExteriorCache(face);
      }
      
      // Interior gets inverted vortex
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
    
    computeFacePattern(face, exteriorCache, false);
  }
  
  private void computeInteriorPattern(Face face) {
    int cols = face.columns.length;
    int rows = face.rows.length;
    
    if (interiorCache == null || interiorCache.length != cols * rows) {
      interiorCache = new int[cols * rows];
    }
    
    computeFacePattern(face, interiorCache, true);
  }

  private void computeFacePattern(Face face, int[] cache, boolean isInterior) {
    int cols = face.columns.length;
    int rows = face.rows.length;
    float invCols = 1.0f / Math.max(1, cols - 1);
    float invRows = 1.0f / Math.max(1, rows - 1);
    
    float cx = centerX.getValuef();
    float cy = centerY.getValuef();
    float grid = gridScale.getValuef();
    float singular = singularity.getValuef();
    float warp = warpStrength.getValuef();
    boolean invertDir = invert.getValueb();
    
    // Interior gets opposite rotation
    float timeMultiplier = isInterior ? -1.5f : 1.0f;
    if (invertDir) timeMultiplier *= -1f;
    
    int cacheIndex = 0;
    for (int rowIdx = 0; rowIdx < rows; rowIdx++) {
      for (int colIdx = 0; colIdx < cols; colIdx++) {
        float u = colIdx * invCols - 0.5f;
        float v = rowIdx * invRows - 0.5f;
        
        cache[cacheIndex++] = calculateVortexColor(u - cx, v - cy, grid, singular, warp, timeMultiplier, isInterior);
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
    // Process exterior and interior with different vortex parameters
    processCylinderOrientation(cylinder.exterior, false);
    
    if (cylinder.interior != null) {
      processCylinderOrientation(cylinder.interior, true);
    }
  }
  
  private void processCylinderOrientation(Cylinder.Orientation orientation, boolean isInterior) {
    Ring[] rings = orientation.rings;
    int numRings = rings.length;
    
    float cx = centerX.getValuef();
    float cy = centerY.getValuef();
    float grid = gridScale.getValuef();
    float singular = singularity.getValuef();
    float warp = warpStrength.getValuef();
    boolean invertDir = invert.getValueb();
    
    float timeMultiplier = isInterior ? -1.5f : 1.0f;
    if (invertDir) timeMultiplier *= -1f;

    for (int ringIndex = 0; ringIndex < numRings; ringIndex++) {
      Ring ring = rings[ringIndex];
      int pointsPerRing = ring.points.length;
      
      float v = (float)ringIndex / Math.max(1, numRings - 1) - 0.5f;

      for (int pointIndex = 0; pointIndex < pointsPerRing; pointIndex++) {
        LXPoint p = ring.points[pointIndex];
        float u = (float)pointIndex / pointsPerRing - 0.5f;
        
        int color = calculateVortexColor(u - cx, v - cy, grid, singular, warp, timeMultiplier, isInterior);
        colors[p.index] = color;
      }
    }
  }
  
  private int calculateVortexColor(float x, float y, float grid, float singular, float warp, float timeMult, boolean isInterior) {
    // Distance from vortex center
    float r = (float)Math.sqrt(x*x + y*y);
    if (r < 0.001f) r = 0.001f; // Avoid division by zero
    
    // Angle around vortex
    float theta = (float)Math.atan2(y, x);
    
    // Vortex transformation: inward spiral with time
    float spiralAngle = theta + time * timeMult;
    
    // Logarithmic spiral inversion - closer to center = more warping
    float inversionFactor = 1f / (r + singular);
    float warpedR = r * (1f + warp * inversionFactor);
    float warpedTheta = spiralAngle + inversionFactor * warp;
    
    // Transform back to Cartesian for grid calculation
    float warpedX = warpedR * (float)Math.cos(warpedTheta);
    float warpedY = warpedR * (float)Math.sin(warpedTheta);
    
    // Create grid pattern in warped space
    float gridU = warpedX * grid;
    float gridV = warpedY * grid;
    
    // Grid lines with antialiasing
    float gridLineU = Math.abs(gridU - (float)Math.floor(gridU + 0.5f));
    float gridLineV = Math.abs(gridV - (float)Math.floor(gridV + 0.5f));
    float gridThickness = 0.1f / (1f + inversionFactor * 2f); // Thinner near singularity
    
    boolean onGrid = (gridLineU < gridThickness) || (gridLineV < gridThickness);
    
    // Event horizon effect - approaching singularity
    float horizonDistance = Math.max(0f, (r - singular) / (0.3f - singular));
    horizonDistance = Math.min(horizonDistance, 1f);
    
    if (!onGrid && horizonDistance > 0.8f) {
      return 0; // Empty space beyond event horizon
    }
    
    // Calculate intensity based on proximity to singularity and grid
    float baseIntensity = onGrid ? 1f : (1f - horizonDistance) * 0.3f;
    
    // Doppler shift effect - colors shift based on spiral velocity
    float velocityFactor = inversionFactor * warp * 0.5f;
    
    // Color selection based on grid position and event horizon
    float[] hueArray = onGrid ? GRID_HUES : HORIZON_HUES;
    int hueIndex = (int)((warpedTheta / (2f * (float)Math.PI) + 1f + time * 0.3f) * hueArray.length) % hueArray.length;
    if (hueIndex < 0) hueIndex += hueArray.length;
    
    float baseHue = hueArray[hueIndex];
    
    // Apply Doppler shift
    float dopplerHue = (baseHue + velocityFactor * 60f + (isInterior ? 180f : 0f)) % 360f;
    if (dopplerHue < 0) dopplerHue += 360f;
    
    // Intensity modulation near singularity
    float singularityPulse = (float)Math.sin(time * 5f + inversionFactor * 10f) * 0.3f + 0.7f;
    float finalIntensity = baseIntensity * singularityPulse;
    
    if (finalIntensity < 0.05f) return 0;
    
    // Brightness falls off with distance from singularity but spikes at grid intersections
    float brightnessFactor = onGrid ? 1.2f : (1f - horizonDistance * 0.7f);
    float finalBrightness = Math.min(finalIntensity * brightness.getValuef() * brightnessFactor, 100f);
    
    // Saturation increases near singularity
    float finalSaturation = sat.getValuef() * (0.5f + 0.5f * (1f - horizonDistance));
    
    return LXColor.hsb(dopplerHue, finalSaturation, finalBrightness);
  }
}
