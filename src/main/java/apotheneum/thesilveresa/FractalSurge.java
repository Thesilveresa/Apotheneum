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
@LXComponentName("Fractal Surge")
public class FractalSurge extends ApotheneumPattern {

  private final CompoundParameter speed = new CompoundParameter("Speed", 0.5, 0.1, 3.0)
    .setDescription("Fractal bloom speed");
  private final CompoundParameter complexity = new CompoundParameter("Complex", 4.0, 2.0, 8.0)
    .setDescription("Fractal iteration depth");
  private final CompoundParameter zoom = new CompoundParameter("Zoom", 2.0, 0.5, 6.0)
    .setDescription("Fractal zoom level");
  private final CompoundParameter surgeIntensity = new CompoundParameter("Surge", 0.8, 0.2, 1.5)
    .setDescription("Bloom intensity");
  private final CompoundParameter centerX = new CompoundParameter("CenterX", 0.0, -1.0, 1.0)
    .setDescription("Fractal center X");
  private final CompoundParameter centerY = new CompoundParameter("CenterY", 0.0, -1.0, 1.0)
    .setDescription("Fractal center Y");
  private final BooleanParameter mandelbrot = new BooleanParameter("Mandel", true)
    .setDescription("Mandelbrot vs Julia set");
  private final CompoundParameter sat = new CompoundParameter("Sat", 90.0, 0.0, 100.0)
    .setDescription("Color saturation");

  private float time = 0f;
  private int[] exteriorCache;
  private int[] interiorCache;
  private boolean cacheValid = false;
  
  // Pre-computed palette for performance
  private static final float[] SURGE_HUES = { 220f, 280f, 320f, 20f, 60f, 180f };
  private static final int MAX_ITERATIONS = 32; // Reasonable limit for real-time

  public FractalSurge(LX lx) {
    super(lx);
    addParameter("Speed", this.speed);
    addParameter("Complex", this.complexity);
    addParameter("Zoom", this.zoom);
    addParameter("Surge", this.surgeIntensity);
    addParameter("CenterX", this.centerX);
    addParameter("CenterY", this.centerY);
    addParameter("Mandel", this.mandelbrot);
    addParameter("Sat", this.sat);
  }

  @Override
  protected void render(double deltaMs) {
    time += (float)(deltaMs / 1000.0) * speed.getValuef();
    cacheValid = false; // Invalidate cache each frame for animation
    
    // Render geometries with face copying optimization
    Cube cube = Apotheneum.cube;
    if (cube != null) {
      // Compute exterior pattern
      Face referenceFace = cube.exterior.faces[0];
      computeExteriorPattern(referenceFace);
      
      // Copy to all exterior faces
      for (Face face : cube.exterior.faces) {
        copyFromExteriorCache(face);
      }
      
      // Compute interior if different, otherwise copy
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
    
    float zoomLevel = zoom.getValuef();
    float cx = centerX.getValuef();
    float cy = centerY.getValuef();
    int maxIter = Math.min((int)complexity.getValuef() * 4, MAX_ITERATIONS);
    
    // Interior gets inverted perspective
    float perspective = isInterior ? -1.2f : 1.0f;
    
    int cacheIndex = 0;
    for (int rowIdx = 0; rowIdx < rows; rowIdx++) {
      for (int colIdx = 0; colIdx < cols; colIdx++) {
        // Map to complex plane
        float u = (colIdx * invCols - 0.5f) * perspective / zoomLevel + cx;
        float v = (rowIdx * invRows - 0.5f) * perspective / zoomLevel + cy;
        
        cache[cacheIndex++] = calculateFractalColor(u, v, maxIter);
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
    // Process exterior and interior separately for different perspectives
    processCylinderOrientation(cylinder.exterior, false);
    
    if (cylinder.interior != null) {
      processCylinderOrientation(cylinder.interior, true);
    }
  }
  
  private void processCylinderOrientation(Cylinder.Orientation orientation, boolean isInterior) {
    Ring[] rings = orientation.rings;
    int numRings = rings.length;
    
    float zoomLevel = zoom.getValuef();
    float cx = centerX.getValuef();
    float cy = centerY.getValuef();
    int maxIter = Math.min((int)complexity.getValuef() * 4, MAX_ITERATIONS);
    float perspective = isInterior ? -0.8f : 1.0f;

    for (int ringIndex = 0; ringIndex < numRings; ringIndex++) {
      Ring ring = rings[ringIndex];
      int pointsPerRing = ring.points.length;
      
      float v = ((float)ringIndex / Math.max(1, numRings - 1) - 0.5f) * perspective / zoomLevel + cy;

      for (int pointIndex = 0; pointIndex < pointsPerRing; pointIndex++) {
        LXPoint p = ring.points[pointIndex];
        float u = ((float)pointIndex / pointsPerRing - 0.5f) * perspective / zoomLevel + cx;
        
        int color = calculateFractalColor(u, v, maxIter);
        colors[p.index] = color;
      }
    }
  }
  
  private int calculateFractalColor(float x0, float y0, int maxIter) {
    // Animated Julia set parameters
    float jx = 0.3f * (float)Math.cos(time * 0.7f);
    float jy = 0.4f * (float)Math.sin(time * 0.9f);
    
    float x = x0;
    float y = y0;
    int iter = 0;
    
    // Choose fractal type
    if (mandelbrot.getValueb()) {
      // Mandelbrot set: z = z² + c
      while (x * x + y * y <= 4f && iter < maxIter) {
        float xtemp = x * x - y * y + x0;
        y = 2f * x * y + y0;
        x = xtemp;
        iter++;
      }
    } else {
      // Julia set: z = z² + c (with animated c)
      while (x * x + y * y <= 4f && iter < maxIter) {
        float xtemp = x * x - y * y + jx;
        y = 2f * x * y + jy;
        x = xtemp;
        iter++;
      }
    }
    
    if (iter >= maxIter) return 0; // Inside set
    
    // Smooth iteration count for better coloring
    float smoothIter = iter + 1f - (float)Math.log(Math.log(x*x + y*y) / Math.log(2)) / (float)Math.log(2);
    
    // Apply surge effect
    float surge = surgeIntensity.getValuef();
    float surgeFactor = 1f + surge * (float)Math.sin(time * 2f + smoothIter * 0.5f);
    
    // Map to color
    float hueIndex = (smoothIter * 0.3f + time * 30f) % SURGE_HUES.length;
    int hueIdx = (int)hueIndex;
    float hueBlend = hueIndex - hueIdx;
    
    float hue1 = SURGE_HUES[hueIdx];
    float hue2 = SURGE_HUES[(hueIdx + 1) % SURGE_HUES.length];
    float hue = hue1 * (1f - hueBlend) + hue2 * hueBlend;
    
    float brightness = Math.min((1f - smoothIter / maxIter) * 100f * surgeFactor, 100f);
    
    return LXColor.hsb(hue, sat.getValuef(), brightness);
  }
}
