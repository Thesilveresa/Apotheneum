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
import heronarts.lx.parameter.DiscreteParameter;

@LXCategory("Apotheneum/thesilveresa")
@LXComponentName("Superformula 1")
public class Superformula1 extends ApotheneumPattern {

  // Precomputed constants
  private static final float INV_1_6 = 1.0f / 1.6f;
  private static final float TWO_PI = (float)(Math.PI * 2.0);
  private static final float INV_FOUR = 0.25f;
  private static final float HALF = 0.5f;
  private static final int ANGLE_STEPS = 360; // Lookup table resolution
  private static final float INV_ANGLE_STEPS = 1.0f / ANGLE_STEPS;

  // Core superformula parameters
  private final CompoundParameter m = new CompoundParameter("m", 5.0, 1.0, 20.0)
    .setDescription("Rotational symmetry parameter");
  private final CompoundParameter n1 = new CompoundParameter("n1", 1.0, 0.1, 10.0)
    .setDescription("First shape parameter");
  private final CompoundParameter n2 = new CompoundParameter("n2", 1.0, 0.1, 10.0)
    .setDescription("Second shape parameter");
  private final CompoundParameter n3 = new CompoundParameter("n3", 1.0, 0.1, 10.0)
    .setDescription("Third shape parameter");
  private final CompoundParameter a = new CompoundParameter("a", 1.0, 0.1, 3.0)
    .setDescription("X-axis scaling factor");
  private final CompoundParameter b = new CompoundParameter("b", 1.0, 0.1, 3.0)
    .setDescription("Y-axis scaling factor");
    
  // Animation and morphing controls
  private final CompoundParameter morphSpeed = new CompoundParameter("Speed", 0.1, 0.0, 1.0)
    .setDescription("Morph speed between presets");
  private final CompoundParameter morphProgress = new CompoundParameter("Morph", 0.0, 0.0, 1.0)
    .setDescription("Manual morph control");
  private final DiscreteParameter preset = new DiscreteParameter("Preset", 0, 10)
    .setDescription("Shape preset selection");
  private final CompoundParameter sweep = new CompoundParameter("Sweep", 0.0, 0.0, 1.0)
    .setDescription("Sweep through all presets continuously");
    
  // Visual controls
  private final CompoundParameter scale = new CompoundParameter("Scale", 0.8, 0.1, 2.0)
    .setDescription("Shape scale factor");
  private final CompoundParameter rotation = new CompoundParameter("Rotate", 0.0, 0.0, 1.0)
    .setDescription("Shape rotation");
  private final CompoundParameter sat = new CompoundParameter("Sat", 85.0, 0.0, 100.0)
    .setDescription("Color saturation");
  private final CompoundParameter brightness = new CompoundParameter("Bright", 100.0, 10.0, 100.0)
    .setDescription("Base brightness");
  private final BooleanParameter filled = new BooleanParameter("Fill", false)
    .setDescription("Fill shapes vs outline only");
  private final CompoundParameter edgeWidth = new CompoundParameter("Edge", 0.05, 0.01, 0.2)
    .setDescription("Edge detection threshold");
    
  // Animation modes
  private final DiscreteParameter animMode = new DiscreteParameter("Anim", 0, 4)
    .setDescription("Animation mode: 0=Static, 1=Pulse, 2=Rotate, 3=Breathe");
  private final CompoundParameter animRate = new CompoundParameter("Rate", 1.0, 0.1, 5.0)
    .setDescription("Animation rate multiplier");

  // Predefined superformula presets (m, n1, n2, n3, a, b)
  private static final float[][] PRESETS = {
    {5f, 1f, 1f, 1f, 1f, 1f},
    {3f, 4.5f, 10f, 10f, 1f, 1f},
    {6f, 1f, 7f, 8f, 1f, 1f},
    {2f, 1f, 1f, 1f, 1f, 1f},
    {4f, 0.5f, 0.5f, 4f, 1f, 1f},
    {8f, 1f, 1f, 8f, 1f, 1f},
    {3f, 0.5f, 0.5f, 0.5f, 1f, 1f},
    {7f, 2f, 6f, 6f, 1f, 1f},
    {4f, 1f, 0.5f, 0.8f, 1f, 1f},
    {6f, 10f, 10f, 10f, 1f, 1f}
  };
  
  private static final float[] HUES = {50f, 340f, 200f, 300f, 120f, 60f, 280f, 180f, 20f, 240f};

  // Current interpolated parameters
  private float currentM, currentN1, currentN2, currentN3, currentA, currentB;
  
  // Cached values
  private float cosRot, sinRot, scaleVal, animTime;
  private float[] superformulaTable = new float[ANGLE_STEPS];
  private float pulseFactor, brightnessFactor;

  public Superformula1(LX lx) {
    super(lx);
    addParameter("m", this.m);
    addParameter("n1", this.n1);
    addParameter("n2", this.n2);
    addParameter("n3", this.n3);
    addParameter("a", this.a);
    addParameter("b", this.b);
    addParameter("Speed", this.morphSpeed);
    addParameter("Morph", this.morphProgress);
    addParameter("Preset", this.preset);
    addParameter("Sweep", this.sweep);
    addParameter("Scale", this.scale);
    addParameter("Rotate", this.rotation);
    addParameter("Sat", this.sat);
    addParameter("Bright", this.brightness);
    addParameter("Fill", this.filled);
    addParameter("Edge", this.edgeWidth);
    addParameter("Anim", this.animMode);
    addParameter("Rate", this.animRate);

    float[] p = PRESETS[0];
    currentM = p[0];
    currentN1 = p[1];
    currentN2 = p[2];
    currentN3 = p[3];
    currentA = p[4];
    currentB = p[5];
  }
  
  private void interpolatePresets(float sweepValue) {
    int numPresets = PRESETS.length;
    float scaledSweep = sweepValue * (numPresets - 1);
    int basePreset = (int)scaledSweep;
    int nextPreset = (basePreset + 1) % numPresets;
    float t = scaledSweep - basePreset;
    t = t * t * (3 - 2 * t);
    
    float[] preset1 = PRESETS[basePreset % numPresets];
    float[] preset2 = PRESETS[nextPreset];
    
    float baseM = preset1[0] + (preset2[0] - preset1[0]) * t;
    float baseN1 = preset1[1] + (preset2[1] - preset1[1]) * t;
    float baseN2 = preset1[2] + (preset2[2] - preset1[2]) * t;
    float baseN3 = preset1[3] + (preset2[3] - preset1[3]) * t;
    float baseA = preset1[4] + (preset2[4] - preset1[4]) * t;
    float baseB = preset1[5] + (preset2[5] - preset1[5]) * t;
    
    currentM = baseM * (m.getValuef() * 0.2f);
    currentN1 = baseN1 * n1.getValuef();
    currentN2 = baseN2 * n2.getValuef();
    currentN3 = baseN3 * n3.getValuef();
    currentA = baseA * a.getValuef();
    currentB = baseB * b.getValuef();
  }
  
  private void updateParameters() {
    float sweepValue = sweep.getValuef();
    
    if (sweepValue > 0.001f) {
      interpolatePresets(sweepValue);
    } else {
      currentM = m.getValuef();
      currentN1 = n1.getValuef();
      currentN2 = n2.getValuef();
      currentN3 = n3.getValuef();
      currentA = a.getValuef();
      currentB = b.getValuef();
    }
    updateSuperformulaTable();
  }
  
  private void updateSuperformulaTable() {
    for (int i = 0; i < ANGLE_STEPS; i++) {
      float angle = i * INV_ANGLE_STEPS * TWO_PI;
      float angleTerm = currentM * angle * INV_FOUR;
      float cosComponent = Math.abs((float)Math.cos(angleTerm) / currentA);
      float sinComponent = Math.abs((float)Math.sin(angleTerm) / currentB);
      float term1 = fastPow(cosComponent, currentN2);
      float term2 = fastPow(sinComponent, currentN3);
      float result = fastPow(term1 + term2, -1.0f / currentN1);
      superformulaTable[i] = Float.isFinite(result) && result > 0 ? result : 0.001f;
    }
  }
  
  // Fast approximation for pow(x, y) for small exponents
  private float fastPow(float x, float y) {
    if (Math.abs(y - 1.0f) < 0.01f) return x;
    if (Math.abs(y - 2.0f) < 0.01f) return x * x;
    if (Math.abs(y - 0.5f) < 0.01f) return (float)Math.sqrt(x);
    return (float)Math.pow(x, y);
  }
  
  private void precomputeFrameValues(float time) {
    scaleVal = scale.getValuef();
    float rotVal = rotation.getValuef() * TWO_PI;
    
    int mode = animMode.getValuei();
    if (mode == 2) {
      rotVal += time * animRate.getValuef() * HALF;
    }
    
    cosRot = (float)Math.cos(rotVal);
    sinRot = (float)Math.sin(rotVal);
    
    if (mode == 1) {
      pulseFactor = 0.7f + 0.6f * (float)Math.sin(time * animRate.getValuef() * 2f);
      scaleVal *= pulseFactor;
      brightnessFactor = 0.6f + 0.4f * (float)Math.sin(time * animRate.getValuef() * 3f);
    } else if (mode == 3) {
      scaleVal *= (0.8f + 0.4f * (float)Math.sin(time * animRate.getValuef()));
      brightnessFactor = 1.0f;
    } else {
      pulseFactor = 1.0f;
      brightnessFactor = 1.0f;
    }
    
    animTime = time;
  }
  
  private float superformula(float angle) {
    int index = (int)((angle / TWO_PI) * ANGLE_STEPS);
    index = Math.max(0, Math.min(ANGLE_STEPS - 1, index));
    return superformulaTable[index];
  }
  
  private boolean isInsideShape(float u, float v) {
    float x = u - HALF;
    float y = v - HALF;
    
    float rotX = x * cosRot - y * sinRot;
    float rotY = x * sinRot + y * cosRot;
    
    float radius = (float)Math.sqrt(rotX * rotX + rotY * rotY);
    float angle = (float)Math.atan2(rotY, rotX);
    if (angle < 0) angle += TWO_PI;
    
    float shapeRadius = superformula(angle) * scaleVal * 0.4f;
    
    return radius <= shapeRadius;
  }
  
  private boolean isOnEdge(float u, float v, float invCols, float invRows) {
    if (!isInsideShape(u, v)) return false;
    
    // Simplified edge detection: check fewer neighbors
    float uPlus = u + invCols;
    float vPlus = v + invRows;
    return !(isInsideShape(uPlus, v) && isInsideShape(u, vPlus));
  }

  @Override
  protected void render(double deltaMs) {
    updateParameters();
    
    float time = (float)(lx.engine.nowMillis * 0.001) * morphSpeed.getValuef();
    precomputeFrameValues(time);
    
    float hue;
    float sweepValue = sweep.getValuef();
    if (sweepValue > 0.001f) {
      float scaledSweep = sweepValue * (HUES.length - 1);
      int baseHue = (int)scaledSweep;
      int nextHue = (baseHue + 1) % HUES.length;
      float t = scaledSweep - baseHue;
      t = t * t * (3 - 2 * t);
      
      float hue1 = HUES[baseHue % HUES.length];
      float hue2 = HUES[nextHue];
      
      if (Math.abs(hue2 - hue1) > 180) {
        if (hue1 > hue2) {
          hue2 += 360;
        } else {
          hue1 += 360;
        }
      }
      
      hue = hue1 + (hue2 - hue1) * t;
      if (hue >= 360) hue -= 360;
    } else {
      hue = HUES[preset.getValuei() % HUES.length];
    }
    
    float saturation = sat.getValuef();
    float baseBright = brightness.getValuef();
    boolean fillMode = filled.getValueb();
    
    Cube cube = Apotheneum.cube;
    if (cube != null) {
      renderCubeGeometry(cube.exterior, hue, saturation, baseBright, fillMode);
      copyCubeExterior();
    }
    
    Cylinder cylinder = Apotheneum.cylinder;
    if (cylinder != null) {
      renderCylinderGeometry(cylinder.exterior, hue, saturation, baseBright, fillMode);
      copyCylinderExterior();
    }
  }
  
  private void renderCubeGeometry(Cube.Orientation orientation, float hue, float saturation, float baseBright, boolean fillMode) {
    int mode = animMode.getValuei();
    float brightness = baseBright * (mode == 1 ? brightnessFactor : 1.0f);
    int color = fillMode ? LXColor.hsb(hue, saturation, brightness) : 0;
    
    for (Face face : orientation.faces) {
      int cols = face.columns.length;
      int rows = face.rows.length;
      float invCols = 1.0f / Math.max(1, cols - 1);
      float invRows = 1.0f / Math.max(1, rows - 1);
      
      for (Row row : face.rows) {
        float v = row.index * invRows;
        for (int cx = 0; cx < cols; cx++) {
          LXPoint p = row.points[cx];
          float u = cx * invCols;
          
          boolean inside = isInsideShape(u, v);
          boolean onEdge = !fillMode && isOnEdge(u, v, invCols, invRows);
          
          if (fillMode && inside) {
            colors[p.index] = color;
          } else if (!fillMode && onEdge) {
            colors[p.index] = LXColor.hsb(hue, saturation, brightness);
          } else {
            colors[p.index] = 0;
          }
        }
      }
    }
  }
  
  private void renderCylinderGeometry(Cylinder.Orientation orientation, float hue, float saturation, float baseBright, boolean fillMode) {
    int mode = animMode.getValuei();
    float brightness = baseBright * (mode == 1 ? brightnessFactor : 1.0f);
    int color = fillMode ? LXColor.hsb(hue, saturation, brightness) : 0;
    
    Ring[] rings = orientation.rings;
    int numRings = rings.length;
    float invNumRings = 1.0f / Math.max(1, numRings - 1);
    
    for (int ringIndex = 0; ringIndex < numRings; ringIndex++) {
      Ring ring = rings[ringIndex];
      int pointsPerRing = ring.points.length;
      float v = ringIndex * invNumRings;
      
      for (int pointIndex = 0; pointIndex < pointsPerRing; pointIndex++) {
        LXPoint p = ring.points[pointIndex];
        float u = (float)pointIndex / pointsPerRing;
        
        boolean inside = isInsideShape(u, v);
        boolean onEdge = !fillMode && isCylinderEdge(u, v, pointIndex, ringIndex, pointsPerRing, numRings);
        
        if (fillMode && inside) {
          colors[p.index] = color;
        } else if (!fillMode && onEdge) {
          colors[p.index] = LXColor.hsb(hue, saturation, brightness);
        } else {
          colors[p.index] = 0;
        }
      }
    }
  }
  
  private boolean isCylinderEdge(float u, float v, int pointIndex, int ringIndex, int pointsPerRing, int numRings) {
    if (!isInsideShape(u, v)) return false;
    
    int nextPoint = (pointIndex + 1) % pointsPerRing;
    float nextU = (float)nextPoint / pointsPerRing;
    float invNumRings = 1.0f / Math.max(1, numRings - 1);
    float nextV = ringIndex + 1 < numRings ? (ringIndex + 1) * invNumRings : v;
    
    return !(isInsideShape(nextU, v) && isInsideShape(u, nextV));
  }
}
