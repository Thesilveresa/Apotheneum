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
@LXComponentName("Stillness Layer")
public class StillnessLayer extends ApotheneumPattern {

  private final CompoundParameter slowdown = new CompoundParameter("Slowdown", 0.8, 0.0, 1.0)
    .setDescription("Rate of pattern deceleration");
  private final CompoundParameter moireScale = new CompoundParameter("Moiré", 8.0, 2.0, 20.0)
    .setDescription("Moiré pattern scale");
  private final CompoundParameter interference = new CompoundParameter("Waves", 0.8, 0.0, 1.0)
    .setDescription("Wave interference strength");
  private final CompoundParameter stillness = new CompoundParameter("Stillness", 0.5, 0.0, 1.0)
    .setDescription("Approach to stillness");
  private final CompoundParameter emergence = new CompoundParameter("Emerge", 0.0, 0.0, 1.0)
    .setDescription("New pattern emergence");
  private final BooleanParameter pauseMode = new BooleanParameter("Pause", false)
    .setDescription("Complete stillness mode");
  private final CompoundParameter ambientHue = new CompoundParameter("Hue", 240.0, 0.0, 360.0)
    .setDescription("Ambient color hue");
  private final CompoundParameter intensity = new CompoundParameter("Intensity", 0.8, 0.3, 1.5)
    .setDescription("Overall brightness multiplier");
  private final CompoundParameter complexity = new CompoundParameter("Complexity", 0.6, 0.2, 1.0)
    .setDescription("Pattern complexity layers");

  // Enhanced lookup tables for more interesting patterns
  private static final int WAVE_TABLE_SIZE = 512;
  private static final int MOIRE_CACHE_SIZE = 256;
  private float[] waveTable;
  private float[] moireCache;
  private float[] complexityTable;
  
  // State tracking for smooth transitions
  private float currentSpeed = 1.0f;
  private float targetSpeed = 1.0f;
  private float time = 0f;
  private float stillnessAccumulator = 0f;
  private float lastFrameTime = 0f;
  private float energyLevel = 1.0f; // New: tracks visual energy
  
  // Pattern states for the journey to stillness
  private enum StillnessState {
    MOVING,     // Normal motion - high energy, complex patterns
    SLOWING,    // Gradually slowing down - patterns consolidating
    PAUSED,     // Completely still - subtle breathing effects
    EMERGING    // New patterns beginning to form - burst of energy
  }
  
  private StillnessState currentState = StillnessState.MOVING;
  private float stateTransitionTime = 0f;

  public StillnessLayer(LX lx) {
    super(lx);
    addParameter("Slowdown", this.slowdown);
    addParameter("Moiré", this.moireScale);
    addParameter("Waves", this.interference);
    addParameter("Stillness", this.stillness);
    addParameter("Emerge", this.emergence);
    addParameter("Pause", this.pauseMode);
    addParameter("Hue", this.ambientHue);
    addParameter("Intensity", this.intensity);
    addParameter("Complexity", this.complexity);
    
    initializeWaveTables();
  }
  
  private void initializeWaveTables() {
    // Enhanced wave patterns with more variation
    waveTable = new float[WAVE_TABLE_SIZE];
    for (int i = 0; i < WAVE_TABLE_SIZE; i++) {
      float t = (float)i / WAVE_TABLE_SIZE;
      // Combine triangle, sine, and square-like waves for rich textures
      float triangle = 1f - 2f * Math.abs((t * 2f) % 2f - 1f);
      float sine = (float)Math.sin(t * Math.PI * 2);
      float square = (t % 1f > 0.5f) ? 1f : -1f;
      
      waveTable[i] = triangle * 0.5f + sine * 0.3f + square * 0.2f;
    }
    
    // Enhanced moiré interference patterns
    moireCache = new float[MOIRE_CACHE_SIZE];
    for (int i = 0; i < MOIRE_CACHE_SIZE; i++) {
      float t = (float)i / MOIRE_CACHE_SIZE;
      // Multiple frequency interference for richer patterns
      float wave1 = waveTable[(int)(t * WAVE_TABLE_SIZE * 1.0f) % WAVE_TABLE_SIZE];
      float wave2 = waveTable[(int)(t * WAVE_TABLE_SIZE * 1.03f) % WAVE_TABLE_SIZE];
      float wave3 = waveTable[(int)(t * WAVE_TABLE_SIZE * 0.97f) % WAVE_TABLE_SIZE];
      
      moireCache[i] = (wave1 + wave2 + wave3) * 0.333f;
    }
    
    // Complexity modulation table
    complexityTable = new float[MOIRE_CACHE_SIZE];
    for (int i = 0; i < MOIRE_CACHE_SIZE; i++) {
      float t = (float)i / MOIRE_CACHE_SIZE;
      complexityTable[i] = (float)(0.5 + 0.5 * Math.sin(t * Math.PI * 7.3) * Math.cos(t * Math.PI * 5.7));
    }
  }

  @Override
  protected void render(double deltaMs) {
    float dt = (float)(deltaMs / 1000.0);
    updateStillnessState(dt);
    
    // Only advance time if not paused
    if (!pauseMode.getValueb() && currentState != StillnessState.PAUSED) {
      time += dt * currentSpeed;
      lastFrameTime = time;
    } else {
      // In pause mode, use the last frame time to maintain static patterns
      time = lastFrameTime;
    }
    
    // Update energy level based on state
    updateEnergyLevel(dt);
    
    // Optimization: Render once and copy to all similar faces
    Cube cube = Apotheneum.cube;
    if (cube != null) {
      if (cube.exterior.faces.length > 0) {
        Face referenceFace = cube.exterior.faces[0];
        processFace(referenceFace);
        
        // Copy to other exterior faces
        for (int i = 1; i < cube.exterior.faces.length; i++) {
          Face currentFace = cube.exterior.faces[i];
          if (currentFace.rows.length == referenceFace.rows.length) {
            for (int rowIndex = 0; rowIndex < currentFace.rows.length; rowIndex++) {
              Row currentRow = currentFace.rows[rowIndex];
              Row referenceRow = referenceFace.rows[rowIndex];
              
              int pointsToProcess = Math.min(currentRow.points.length, referenceRow.points.length);
              for (int cx = 0; cx < pointsToProcess; cx++) {
                colors[currentRow.points[cx].index] = colors[referenceRow.points[cx].index];
              }
            }
          } else {
            processFace(currentFace);
          }
        }
        
        // Copy to interior
        if (cube.interior != null) {
          for (Face interiorFace : cube.interior.faces) {
            if (interiorFace.rows.length == referenceFace.rows.length) {
              for (int rowIndex = 0; rowIndex < interiorFace.rows.length; rowIndex++) {
                Row interiorRow = interiorFace.rows[rowIndex];
                Row referenceRow = referenceFace.rows[rowIndex];
                
                int pointsToProcess = Math.min(interiorRow.points.length, referenceRow.points.length);
                for (int cx = 0; cx < pointsToProcess; cx++) {
                  colors[interiorRow.points[cx].index] = colors[referenceRow.points[cx].index];
                }
              }
            } else {
              processFace(interiorFace);
            }
          }
        }
      }
    }
    
    // Cylinder processing
    Cylinder cylinder = Apotheneum.cylinder;
    if (cylinder != null) {
      processCylinder(cylinder.exterior);
      if (cylinder.interior != null) {
        processCylinder(cylinder.interior);
      }
    }
  }
  
  private void updateEnergyLevel(float dt) {
    // Energy level affects visual intensity and complexity
    float targetEnergy = 1.0f;
    
    switch (currentState) {
      case MOVING:
        targetEnergy = 1.0f + emergence.getValuef() * 0.5f;
        break;
      case SLOWING:
        targetEnergy = 0.8f - stillness.getValuef() * 0.3f;
        break;
      case PAUSED:
        targetEnergy = 0.2f + 0.1f * (float)Math.sin(stateTransitionTime * 2.0f); // Gentle breathing
        break;
      case EMERGING:
        targetEnergy = 1.2f + 0.3f * (float)Math.sin(stateTransitionTime * 8.0f); // Bursting energy
        break;
    }
    
    // Smooth energy transitions
    energyLevel += (targetEnergy - energyLevel) * dt * 3.0f;
  }
  
  private void updateStillnessState(float dt) {
    float stillnessLevel = stillness.getValuef();
    float emergenceLevel = emergence.getValuef();
    stateTransitionTime += dt;
    
    // Determine target speed based on stillness level
    targetSpeed = 1f - stillnessLevel * slowdown.getValuef();
    
    // Smooth speed transitions to avoid jarring changes
    float speedDiff = targetSpeed - currentSpeed;
    currentSpeed += speedDiff * dt * 2f;
    
    // State machine for stillness progression
    switch (currentState) {
      case MOVING:
        if (stillnessLevel > 0.7f) {
          currentState = StillnessState.SLOWING;
          stateTransitionTime = 0f;
        }
        break;
        
      case SLOWING:
        if (stillnessLevel > 0.9f || pauseMode.getValueb()) {
          currentState = StillnessState.PAUSED;
          stateTransitionTime = 0f;
          currentSpeed = 0f;
        } else if (stillnessLevel < 0.5f) {
          currentState = StillnessState.MOVING;
        }
        break;
        
      case PAUSED:
        if (emergenceLevel > 0.3f && !pauseMode.getValueb()) {
          currentState = StillnessState.EMERGING;
          stateTransitionTime = 0f;
        } else if (stillnessLevel < 0.7f && !pauseMode.getValueb()) {
          currentState = StillnessState.MOVING;
        }
        break;
        
      case EMERGING:
        if (emergenceLevel > 0.8f) {
          currentState = StillnessState.MOVING;
          currentSpeed = 0.3f;
        } else if (emergenceLevel < 0.1f) {
          currentState = StillnessState.PAUSED;
        }
        break;
    }
    
    // Accumulate stillness for subtle effects
    if (currentSpeed < 0.1f) {
      stillnessAccumulator += dt;
    } else {
      stillnessAccumulator *= 0.95f;
    }
  }

  private void processFace(Face face) {
    int cols = face.columns.length;
    int rows = face.rows.length;
    float invCols = 1.0f / Math.max(1, cols - 1);
    float invRows = 1.0f / Math.max(1, rows - 1);
    
    for (Row row : face.rows) {
      for (int cx = 0; cx < cols; cx++) {
        LXPoint p = row.points[cx];
        float u = cx * invCols;
        float v = row.index * invRows;
        
        int color = calculateStillnessColor(u, v);
        colors[p.index] = color;
      }
    }
  }
  
  private void processCylinder(Cylinder.Orientation orientation) {
    Ring[] rings = orientation.rings;
    int numRings = rings.length;

    for (int ringIndex = 0; ringIndex < numRings; ringIndex++) {
      Ring ring = rings[ringIndex];
      int pointsPerRing = ring.points.length;

      for (int pointIndex = 0; pointIndex < pointsPerRing; pointIndex++) {
        LXPoint p = ring.points[pointIndex];
        float u = (float)pointIndex / pointsPerRing;
        float v = (float)ringIndex / (numRings - 1);
        
        int color = calculateStillnessColor(u, v);
        colors[p.index] = color;
      }
    }
  }
  
  private int calculateStillnessColor(float u, float v) {
    float scale = moireScale.getValuef();
    float waveStrength = interference.getValuef();
    float emergenceLevel = emergence.getValuef();
    float hue = ambientHue.getValuef();
    float complexityLevel = complexity.getValuef();
    float intensityMult = intensity.getValuef();
    
    // Multiple scales for richer patterns
    float scale1 = scale;
    float scale2 = scale * 1.618f; // Golden ratio for pleasing interference
    float scale3 = scale * 0.618f;
    
    // Multi-layer moiré with different speeds
    float coord1 = (u * scale1 + time * 0.4f) % 1f;
    float coord2 = (v * scale1 + time * 0.3f) % 1f;
    float coord3 = (u * scale2 + time * 0.2f) % 1f;
    float coord4 = (v * scale3 + time * 0.5f) % 1f;
    
    // Sample from lookup tables
    int idx1 = (int)(coord1 * (MOIRE_CACHE_SIZE - 1));
    int idx2 = (int)(coord2 * (MOIRE_CACHE_SIZE - 1));
    int idx3 = (int)(coord3 * (MOIRE_CACHE_SIZE - 1));
    int idx4 = (int)(coord4 * (MOIRE_CACHE_SIZE - 1));
    
    float moire1 = moireCache[idx1];
    float moire2 = moireCache[idx2];
    float moire3 = moireCache[idx3] * complexityLevel;
    float moire4 = moireCache[idx4] * complexityLevel;
    
    // Combine multiple moiré layers
    float baseInterference = (moire1 + moire2) * 0.5f;
    float complexInterference = (moire3 + moire4) * 0.3f;
    float totalInterference = (baseInterference + complexInterference) * waveStrength;
    
    // Apply stillness effects with more dramatic scaling
    float stillnessFactor = 1f - stillness.getValuef() * 0.7f; // Less dampening
    totalInterference *= stillnessFactor;
    
    // Enhanced breathing effect in stillness
    if (stillnessAccumulator > 1f) {
      float breatheFreq = 0.5f + stillnessAccumulator * 0.05f;
      float breathe = 0.3f * (1f + (float)Math.sin(stillnessAccumulator * breatheFreq));
      totalInterference += breathe * (1f - stillness.getValuef());
    }
    
    // Dramatic emergence effect
    if (emergenceLevel > 0.1f && currentState == StillnessState.EMERGING) {
      float emergenceU = u * 4f + stateTransitionTime * 1.2f;
      float emergenceV = v * 4f + stateTransitionTime * 0.8f;
      
      // Multiple emerging patterns
      float gridU = emergenceU - (int)emergenceU;
      float gridV = emergenceV - (int)emergenceV;
      
      // Sharp grid lines
      if (gridU < 0.15f || gridU > 0.85f || gridV < 0.15f || gridV > 0.85f) {
        totalInterference += emergenceLevel * 0.8f;
        hue += 80f; // Dramatic hue shift
      }
      
      // Radiating circles from emergence
      float centerDist = (float)Math.sqrt((u - 0.5f) * (u - 0.5f) + (v - 0.5f) * (v - 0.5f));
      float ringPhase = (centerDist * 8f - stateTransitionTime * 3f) % 1f;
      if (ringPhase < 0.2f) {
        totalInterference += emergenceLevel * (1f - ringPhase * 5f) * 0.6f;
      }
    }
    
    // Apply energy level scaling
    totalInterference *= energyLevel;
    
    // More generous threshold for visibility
    if (Math.abs(totalInterference) < 0.02f) return 0;
    
    // Enhanced color calculation with much higher brightness
    float brightness = Math.abs(totalInterference) * 100f * intensityMult; // Increased from 40f
    float saturation = 30f + 50f * Math.abs(totalInterference); // Increased saturation
    
    // State-specific enhancements
    switch (currentState) {
      case MOVING:
        brightness *= 1.2f;
        saturation += 20f;
        break;
      case SLOWING:
        brightness *= 0.9f;
        hue += 20f * (1f - currentSpeed);
        break;
      case PAUSED:
        brightness *= 0.6f;
        saturation *= 0.7f;
        hue += 10f * (float)Math.sin(stateTransitionTime);
        break;
      case EMERGING:
        brightness *= 1.8f; // Very bright during emergence
        saturation += 40f;
        hue += 30f * (float)Math.sin(stateTransitionTime * 4f);
        break;
    }
    
    // Ensure reasonable bounds
    brightness = Math.min(100f, Math.max(5f, brightness));
    saturation = Math.min(100f, Math.max(10f, saturation));
    hue = hue % 360f;
    
    return LXColor.hsb(hue, saturation, brightness);
  }
}
