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
@LXComponentName("Crystalline Life")
public class CrystallineLife extends ApotheneumPattern {

  private final CompoundParameter speed = new CompoundParameter("Speed", 0.3, 0.05, 1.5)
    .setDescription("Crystal growth speed");
  private final CompoundParameter complexity = new CompoundParameter("Complex", 3.0, 1.0, 6.0)
    .setDescription("Pentagonal complexity levels");
  private final CompoundParameter scale = new CompoundParameter("Scale", 4.0, 1.0, 8.0)
    .setDescription("Crystal lattice scale");
  private final CompoundParameter lifeForce = new CompoundParameter("Life", 0.6, 0.2, 1.2)
    .setDescription("Organic growth intensity");
  private final CompoundParameter symmetryBreak = new CompoundParameter("Break", 0.3, 0.0, 0.8)
    .setDescription("Symmetry breaking factor");
  private final BooleanParameter breathe = new BooleanParameter("Breathe", true)
    .setDescription("Enable breathing animation");
  private final CompoundParameter hueShift = new CompoundParameter("Hue", 0.0, 0.0, 360.0)
    .setDescription("Base hue rotation");
  private final CompoundParameter sat = new CompoundParameter("Sat", 85.0, 40.0, 100.0)
    .setDescription("Crystal saturation");

  // 5-fold symmetry constants (pentagonal)
  private static final float PHI = 1.618033988749f; // Golden ratio
  private static final float PI_5 = (float)Math.PI / 5f;
  private static final float TWO_PI_5 = 2f * (float)Math.PI / 5f;
  
  // Optimized lookup tables
  private static final int LOOKUP_SIZE = 720; // 0.5 degree resolution
  private float[] sinLookup;
  private float[] cosLookup;
  private float[] phiPowers; // Powers of phi for efficient computation
  
  private float time = 0f;
  private int[] faceCache;
  
  // Crystal seed points for organic growth
  private static final float[][] SEED_POINTS = {
    {0.0f, 0.0f}, {0.3f, 0.2f}, {-0.2f, 0.4f}, {0.5f, -0.3f}, {-0.4f, -0.1f}
  };

  public CrystallineLife(LX lx) {
    super(lx);
    addParameter("Speed", this.speed);
    addParameter("Complex", this.complexity);
    addParameter("Scale", this.scale);
    addParameter("Life", this.lifeForce);
    addParameter("Break", this.symmetryBreak);
    addParameter("Breathe", this.breathe);
    addParameter("Hue", this.hueShift);
    addParameter("Sat", this.sat);
    
    initializeLookupTables();
  }
  
  private void initializeLookupTables() {
    sinLookup = new float[LOOKUP_SIZE];
    cosLookup = new float[LOOKUP_SIZE];
    phiPowers = new float[10]; // Sufficient for complexity levels
    
    for (int i = 0; i < LOOKUP_SIZE; i++) {
      float angle = (float)i * 2f * (float)Math.PI / LOOKUP_SIZE;
      sinLookup[i] = (float)Math.sin(angle);
      cosLookup[i] = (float)Math.cos(angle);
    }
    
    phiPowers[0] = 1f;
    for (int i = 1; i < phiPowers.length; i++) {
      phiPowers[i] = phiPowers[i-1] * PHI;
    }
  }

  @Override
  protected void render(double deltaMs) {
    float dt = (float)(deltaMs / 1000.0);
    time += dt * speed.getValuef();
    
    // Render cube with face caching
    Cube cube = Apotheneum.cube;
    if (cube != null) {
      Face referenceFace = cube.exterior.faces[0];
      computeFacePattern(referenceFace);
      
      // Copy to all faces with slight variations for organic feel
      for (int i = 0; i < cube.exterior.faces.length; i++) {
        copyToFaceWithVariation(cube.exterior.faces[i], i * 0.1f);
      }
      
      if (cube.interior != null) {
        for (int i = 0; i < cube.interior.faces.length; i++) {
          copyToFaceWithVariation(cube.interior.faces[i], (i + 3) * 0.15f);
        }
      }
    }
    
    Cylinder cylinder = Apotheneum.cylinder;
    if (cylinder != null) {
      processCylinder(cylinder);
    }
  }

  private void computeFacePattern(Face face) {
    int cols = face.columns.length;
    int rows = face.rows.length;
    
    if (faceCache == null || faceCache.length != cols * rows) {
      faceCache = new int[cols * rows];
    }
    
    float invCols = 1.0f / Math.max(1, cols - 1);
    float invRows = 1.0f / Math.max(1, rows - 1);
    float scl = scale.getValuef();
    int complexLevels = (int)complexity.getValuef();
    
    int cacheIndex = 0;
    for (int rowIdx = 0; rowIdx < rows; rowIdx++) {
      for (int colIdx = 0; colIdx < cols; colIdx++) {
        float u = (colIdx * invCols - 0.5f) * scl;
        float v = (rowIdx * invRows - 0.5f) * scl;
        
        faceCache[cacheIndex++] = calculateCrystalColor(u, v, complexLevels, 0f);
      }
    }
  }
  
  private void copyToFaceWithVariation(Face face, float variation) {
    int cols = face.columns.length;
    int cacheIndex = 0;
    
    for (Row row : face.rows) {
      for (int cx = 0; cx < cols; cx++) {
        LXPoint p = row.points[cx];
        int baseColor = faceCache[cacheIndex++];
        
        // Apply slight variation for organic feel
        if (baseColor != 0) {
          float h = LXColor.h(baseColor);
          float s = LXColor.s(baseColor);
          float b = LXColor.b(baseColor);
          
          h += variation * 20f; // Slight hue shift
          b *= (1f + variation * 0.1f); // Slight brightness variation
          
          colors[p.index] = LXColor.hsb(h, s, Math.min(b, 100f));
        } else {
          colors[p.index] = baseColor;
        }
      }
    }
  }
  
  private void processCylinder(Cylinder cylinder) {
    Cylinder.Orientation[] faces = (cylinder.interior != null)
      ? new Cylinder.Orientation[]{ cylinder.exterior, cylinder.interior }
      : new Cylinder.Orientation[]{ cylinder.exterior };

    for (int faceIdx = 0; faceIdx < faces.length; faceIdx++) {
      Cylinder.Orientation face = faces[faceIdx];
      Ring[] rings = face.rings;
      int numRings = rings.length;
      float scl = scale.getValuef();
      int complexLevels = (int)complexity.getValuef();
      float variation = faceIdx * 0.2f; // Interior gets more variation

      for (int ringIndex = 0; ringIndex < numRings; ringIndex++) {
        Ring ring = rings[ringIndex];
        int pointsPerRing = ring.points.length;
        
        float v = ((float)ringIndex / Math.max(1, numRings - 1) - 0.5f) * scl;

        for (int pointIndex = 0; pointIndex < pointsPerRing; pointIndex++) {
          LXPoint p = ring.points[pointIndex];
          float u = ((float)pointIndex / pointsPerRing - 0.5f) * scl;
          
          int color = calculateCrystalColor(u, v, complexLevels, variation);
          colors[p.index] = color;
        }
      }
    }
  }
  
  private int calculateCrystalColor(float x, float y, int levels, float variation) {
    float intensity = 0f;
    float organicFactor = 0f;
    float breakFactor = symmetryBreak.getValuef();
    float life = lifeForce.getValuef();
    
    // Breathing animation
    float breathScale = breathe.getValueb() ?
      (1f + 0.3f * fastSin(time * 1.5f)) : 1f;
    
    // Calculate distance to nearest seed point for organic growth
    float minSeedDist = Float.MAX_VALUE;
    for (float[] seed : SEED_POINTS) {
      float dx = x - seed[0];
      float dy = y - seed[1];
      float dist = (float)Math.sqrt(dx*dx + dy*dy);
      minSeedDist = Math.min(minSeedDist, dist);
    }
    
    // Organic growth wave from seeds
    float seedWave = life * (1f - minSeedDist) * fastSin(time * 2f - minSeedDist * 3f);
    organicFactor = Math.max(0f, seedWave);
    
    // 5-fold crystalline patterns at multiple scales
    for (int level = 1; level <= levels; level++) {
      float levelScale = phiPowers[Math.min(level, phiPowers.length-1)] * breathScale;
      float scaledX = x * levelScale;
      float scaledY = y * levelScale;
      
      // Convert to polar coordinates
      float r = (float)Math.sqrt(scaledX*scaledX + scaledY*scaledY);
      float theta = fastAtan2(scaledY, scaledX) + variation;
      
      // 5-fold symmetry with golden ratio spacing
      float pentagonalPattern = 0f;
      for (int i = 0; i < 5; i++) {
        float angleOffset = i * TWO_PI_5 + time * 0.5f;
        float symAngle = theta + angleOffset;
        
        // Radial component with phi scaling
        float radialComponent = 1f - (r % (1f / phiPowers[Math.min(level-1, phiPowers.length-1)]));
        
        // Angular component
        float angularComponent = fastCos(symAngle * 5f);
        
        // Symmetry breaking for organic feel
        float breakage = breakFactor * fastSin(theta * 7f + time + i);
        angularComponent += breakage;
        
        pentagonalPattern += radialComponent * angularComponent;
      }
      
      // Combine with organic growth
      float levelIntensity = Math.abs(pentagonalPattern) * (1f + organicFactor);
      intensity += levelIntensity / (level * level); // Diminishing contribution
    }
    
    if (intensity < 0.1f) return 0;
    
    // Complex hue calculation based on position and growth
    float baseHue = hueShift.getValuef();
    float positionHue = (fastAtan2(y, x) * 180f / (float)Math.PI + 180f) * 0.5f;
    float organicHue = organicFactor * 60f; // Green-blue for life
    float crystalHue = intensity * 40f; // Crystalline colors
    
    float finalHue = (baseHue + positionHue + organicHue + crystalHue + time * 20f) % 360f;
    
    // Brightness combines crystal structure and organic pulses
    float crystalBrightness = Math.min(intensity * 60f, 80f);
    float organicPulse = organicFactor * 40f * fastSin(time * 3f);
    float finalBrightness = Math.min(crystalBrightness + organicPulse, 100f);
    
    // Saturation varies with complexity
    float complexSat = sat.getValuef() * (0.7f + 0.3f * intensity);
    
    return LXColor.hsb(finalHue, complexSat, finalBrightness);
  }
  
  // Fast lookup-based trig functions
  private float fastSin(float angle) {
    int index = (int)((angle / (2f * (float)Math.PI) + 1f) * LOOKUP_SIZE) % LOOKUP_SIZE;
    if (index < 0) index += LOOKUP_SIZE;
    return sinLookup[index];
  }
  
  private float fastCos(float angle) {
    int index = (int)((angle / (2f * (float)Math.PI) + 1f) * LOOKUP_SIZE) % LOOKUP_SIZE;
    if (index < 0) index += LOOKUP_SIZE;
    return cosLookup[index];
  }
  
  private float fastAtan2(float y, float x) {
    if (x == 0f) return (y > 0f) ? (float)Math.PI / 2f : -(float)Math.PI / 2f;
    
    float atan = y / x;
    float result = atan / (1f + 0.28f * atan * atan);
    
    if (x < 0f) {
      result += (y >= 0f) ? (float)Math.PI : -(float)Math.PI;
    }
    
    return result;
  }
}
