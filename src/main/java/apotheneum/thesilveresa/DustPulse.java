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
@LXComponentName("Dust Pulse")
public class DustPulse extends ApotheneumPattern {

  private final CompoundParameter speed = new CompoundParameter("Speed", 0.7, 0.1, 3.0)
    .setDescription("Particle pulse speed");
  private final CompoundParameter density = new CompoundParameter("Density", 0.5, 0.1, 0.9)
    .setDescription("Particle density");
  private final CompoundParameter scatter = new CompoundParameter("Scatter", 0.8, 0.2, 2.0)
    .setDescription("Particle randomness and spread");
  private final CompoundParameter pulse = new CompoundParameter("Pulse", 0.8, 0.1, 1.5)
    .setDescription("Pulse intensity");
  private final CompoundParameter decay = new CompoundParameter("Decay", 0.85, 0.5, 0.98)
    .setDescription("Brightness decay rate");
  private final BooleanParameter coherent = new BooleanParameter("Coherent", false)
    .setDescription("Synchronized particle behavior");
  private final CompoundParameter hueShift = new CompoundParameter("Hue", 0.0, 0.0, 360.0)
    .setDescription("Base hue offset");
  private final CompoundParameter dispersion = new CompoundParameter("Dispersion", 0.6, 0.1, 1.0)
    .setDescription("How particles break away from geometry");
  private final CompoundParameter intensity = new CompoundParameter("Intensity", 0.9, 0.3, 2.0)
    .setDescription("Overall brightness multiplier");

  // Enhanced optimization: Larger lookup tables for smoother effects
  private static final int NOISE_TABLE_SIZE = 512;
  private static final int PULSE_TABLE_SIZE = 256;
  private static final int WAVE_TABLE_SIZE = 128;
  private float[] noiseTable;
  private float[] pulseTable;
  private float[] waveTable;
  
  // Enhanced particle system with more properties
  private float[] particlePhases;
  private float[] particleBrightness;
  private float[] particleVelocityX;
  private float[] particleVelocityY;
  private float[] particleLifetime;
  private float[] particleSize;
  private boolean[] particleActive;
  private int[] particleType; // Different particle behaviors
  
  private float time = 0f;
  private float geometryPhase = 0f;
  private int maxParticles;
  private int activeParticles = 0;

  public DustPulse(LX lx) {
    super(lx);
    addParameter("Speed", this.speed);
    addParameter("Density", this.density);
    addParameter("Scatter", this.scatter);
    addParameter("Pulse", this.pulse);
    addParameter("Decay", this.decay);
    addParameter("Coherent", this.coherent);
    addParameter("Hue", this.hueShift);
    addParameter("Dispersion", this.dispersion);
    addParameter("Intensity", this.intensity);
    
    initializeLookupTables();
    initializeParticles();
  }
  
  private void initializeLookupTables() {
    // Enhanced noise table with multiple octaves for more interesting patterns
    noiseTable = new float[NOISE_TABLE_SIZE];
    for (int i = 0; i < NOISE_TABLE_SIZE; i++) {
      float t = (float)i / NOISE_TABLE_SIZE;
      float noise1 = (float)(0.5 + 0.5 * Math.sin(t * Math.PI * 2 * 3.7) * Math.cos(t * Math.PI * 2 * 2.3));
      float noise2 = (float)(0.3 * Math.sin(t * Math.PI * 2 * 7.1) * Math.cos(t * Math.PI * 2 * 5.7));
      float noise3 = (float)(0.2 * Math.sin(t * Math.PI * 2 * 13.3) * Math.cos(t * Math.PI * 2 * 11.1));
      noiseTable[i] = noise1 + noise2 + noise3;
    }
    
    // Enhanced pulse envelope with multiple peaks for more dynamic pulses
    pulseTable = new float[PULSE_TABLE_SIZE];
    for (int i = 0; i < PULSE_TABLE_SIZE; i++) {
      float t = (float)i / (PULSE_TABLE_SIZE - 1);
      float primary = (float)(Math.exp(-t * t * 6) * (1 + 0.4 * Math.sin(t * Math.PI * 8)));
      float secondary = (float)(0.3 * Math.exp(-(t-0.7) * (t-0.7) * 12) * Math.sin(t * Math.PI * 16));
      pulseTable[i] = Math.max(0, primary + secondary);
    }
    
    // Wave table for geometry dissolution effect
    waveTable = new float[WAVE_TABLE_SIZE];
    for (int i = 0; i < WAVE_TABLE_SIZE; i++) {
      float t = (float)i / (WAVE_TABLE_SIZE - 1);
      waveTable[i] = (float)(0.5 + 0.5 * Math.sin(t * Math.PI * 2) * Math.exp(-t * 2));
    }
  }
  
  private void initializeParticles() {
    // Increased particle count for denser effects
    maxParticles = 800;
    particlePhases = new float[maxParticles];
    particleBrightness = new float[maxParticles];
    particleVelocityX = new float[maxParticles];
    particleVelocityY = new float[maxParticles];
    particleLifetime = new float[maxParticles];
    particleSize = new float[maxParticles];
    particleActive = new boolean[maxParticles];
    particleType = new int[maxParticles];
    
    // Initialize with random properties
    for (int i = 0; i < maxParticles; i++) {
      particlePhases[i] = (float)Math.random();
      particleBrightness[i] = 0f;
      particleVelocityX[i] = (float)(Math.random() - 0.5) * 0.02f;
      particleVelocityY[i] = (float)(Math.random() - 0.5) * 0.02f;
      particleLifetime[i] = 0f;
      particleSize[i] = 0.5f + (float)Math.random() * 0.5f;
      particleActive[i] = false;
      particleType[i] = (int)(Math.random() * 3); // Three particle types
    }
  }

  @Override
  protected void render(double deltaMs) {
    float dt = (float)(deltaMs / 1000.0);
    time += dt * speed.getValuef();
    geometryPhase += dt * speed.getValuef() * 0.3f;
    
    updateParticleSystem(dt);
    
    // Enhanced rendering with geometry dissolution effect
    Cube cube = Apotheneum.cube;
    if (cube != null) {
      for (Face face : cube.exterior.faces) {
        processFaceWithDissolution(face);
      }
      
      if (cube.interior != null) {
        for (Face face : cube.interior.faces) {
          processFaceWithDissolution(face);
        }
      }
    }
    
    // Enhanced cylinder rendering
    Cylinder cylinder = Apotheneum.cylinder;
    if (cylinder != null) {
      processCylinderWithDissolution(cylinder.exterior);
      if (cylinder.interior != null) {
        processCylinderWithDissolution(cylinder.interior);
      }
    }
  }
  
  private void updateParticleSystem(float dt) {
    float dens = density.getValuef();
    float sctr = scatter.getValuef();
    float decayRate = decay.getValuef();
    boolean isCoherent = coherent.getValueb();
    float disp = dispersion.getValuef();
    
    activeParticles = 0;
    
    for (int i = 0; i < maxParticles; i++) {
      if (particleActive[i]) {
        activeParticles++;
        
        // Update particle physics
        particleLifetime[i] += dt;
        particleBrightness[i] *= decayRate;
        
        // Apply velocity for scattering effect
        float velocityDecay = 0.98f;
        particleVelocityX[i] *= velocityDecay;
        particleVelocityY[i] *= velocityDecay;
        
        // Add some turbulence based on particle type
        if (particleType[i] == 1) {
          int noiseIdx = Math.abs((int)(time * 50 + i)) % NOISE_TABLE_SIZE;
          particleVelocityX[i] += noiseTable[noiseIdx] * dt * 0.01f;
          particleVelocityY[i] += noiseTable[(noiseIdx + 100) % NOISE_TABLE_SIZE] * dt * 0.01f;
        }
        
        // Deactivate particles that are too dim or too old
        if (particleBrightness[i] < 0.01f || particleLifetime[i] > 5.0f) {
          particleActive[i] = false;
        }
      } else if (Math.random() < dens * dt * 3) {
        // Spawn new particle with enhanced properties
        particleActive[i] = true;
        particlePhases[i] = isCoherent ? time : (float)Math.random();
        particleBrightness[i] = pulse.getValuef() * (0.3f + 0.7f * (float)Math.random());
        particleLifetime[i] = 0f;
        
        // Initial velocity based on dispersion and particle type
        float velMagnitude = disp * 0.05f * (0.5f + (float)Math.random());
        float angle = (float)Math.random() * 2f * (float)Math.PI;
        particleVelocityX[i] = (float)Math.cos(angle) * velMagnitude;
        particleVelocityY[i] = (float)Math.sin(angle) * velMagnitude;
        
        particleSize[i] = 0.3f + (float)Math.random() * 1.2f;
        particleType[i] = (int)(Math.random() * 3);
      }
    }
  }

  private void processFaceWithDissolution(Face face) {
    int cols = face.columns.length;
    int rows = face.rows.length;
    float invCols = 1.0f / Math.max(1, cols - 1);
    float invRows = 1.0f / Math.max(1, rows - 1);
    
    // Calculate geometry dissolution factor
    float dissolutionWave = (float)(0.5 + 0.5 * Math.sin(geometryPhase * 0.7));
    
    for (Row row : face.rows) {
      for (int cx = 0; cx < cols; cx++) {
        LXPoint p = row.points[cx];
        float u = cx * invCols;
        float v = row.index * invRows;
        
        // Calculate geometry brightness (starts dense, fades out)
        float geometryBrightness = calculateGeometryBrightness(u, v, dissolutionWave);
        
        // Calculate particle contribution
        int particleColor = calculateEnhancedParticleColor(u, v);
        
        // Blend geometry and particles
        int finalColor = blendGeometryAndParticles(geometryBrightness, particleColor, u, v);
        colors[p.index] = finalColor;
      }
    }
  }
  
  private void processCylinderWithDissolution(Cylinder.Orientation orientation) {
    Ring[] rings = orientation.rings;
    int numRings = rings.length;
    
    float dissolutionWave = (float)(0.5 + 0.5 * Math.sin(geometryPhase * 0.7));

    for (int ringIndex = 0; ringIndex < numRings; ringIndex++) {
      Ring ring = rings[ringIndex];
      int pointsPerRing = ring.points.length;

      for (int pointIndex = 0; pointIndex < pointsPerRing; pointIndex++) {
        LXPoint p = ring.points[pointIndex];
        float u = (float)pointIndex / pointsPerRing;
        float v = (float)ringIndex / (numRings - 1);
        
        // Calculate geometry and particle contributions
        float geometryBrightness = calculateGeometryBrightness(u, v, dissolutionWave);
        int particleColor = calculateEnhancedParticleColor(u, v);
        
        // Blend geometry and particles
        int finalColor = blendGeometryAndParticles(geometryBrightness, particleColor, u, v);
        colors[p.index] = finalColor;
      }
    }
  }
  
  private float calculateGeometryBrightness(float u, float v, float dissolutionWave) {
    // Create geometric patterns that fade over time
    float geometricPattern = (float)(
      0.7 * Math.sin(u * Math.PI * 8 + time * 2) * Math.cos(v * Math.PI * 6 + time * 1.5) +
      0.3 * Math.sin(u * Math.PI * 16 + time * 3) * Math.cos(v * Math.PI * 12 + time * 2.5)
    );
    
    // Apply wave-based dissolution
    int waveIdx = Math.abs((int)((u + v + geometryPhase * 0.5f) * WAVE_TABLE_SIZE)) % WAVE_TABLE_SIZE;
    float dissolutionFactor = waveTable[waveIdx];
    
    // Geometry starts bright and fades as particles take over
    float geometryStrength = (1f - dispersion.getValuef() * dissolutionWave * dissolutionFactor);
    
    return Math.max(0, geometricPattern * geometryStrength * 0.6f);
  }
  
  private int calculateEnhancedParticleColor(float u, float v) {
    float sctr = scatter.getValuef();
    float intensityMult = intensity.getValuef();
    
    float maxBrightness = 0f;
    float bestHue = 0f;
    float bestSat = 0f;
    
    // Sample multiple particles for richer layering
    for (int sampleOffset = 0; sampleOffset < 3; sampleOffset++) {
      int hashX = (int)(u * 25) * 73 + sampleOffset * 17;
      int hashY = (int)(v * 25) * 37 + sampleOffset * 23;
      int hash = (hashX + hashY) % maxParticles;
      
      if (!particleActive[hash]) continue;
      
      // Enhanced scatter with multiple noise octaves
      int noiseIdx1 = Math.abs((int)((u + v + time * 0.3f + sampleOffset * 0.1f) * NOISE_TABLE_SIZE)) % NOISE_TABLE_SIZE;
      int noiseIdx2 = Math.abs((int)((u * 2 + v * 3 + time * 0.7f) * NOISE_TABLE_SIZE)) % NOISE_TABLE_SIZE;
      float scatter1 = noiseTable[noiseIdx1] * sctr;
      float scatter2 = noiseTable[noiseIdx2] * sctr * 0.5f;
      
      // Enhanced particle positioning with velocity
      float particleU = (hash % 29) / 29.0f + scatter1 * 0.15f + particleVelocityX[hash] * particleLifetime[hash];
      float particleV = (hash / 29 % 23) / 23.0f + scatter2 * 0.15f + particleVelocityY[hash] * particleLifetime[hash];
      
      // Wrap coordinates
      particleU = particleU - (float)Math.floor(particleU);
      particleV = particleV - (float)Math.floor(particleV);
      
      // Distance with size consideration
      float dx = u - particleU;
      float dy = v - particleV;
      
      // Handle wrapping for smoother effects
      if (dx > 0.5f) dx -= 1.0f;
      if (dx < -0.5f) dx += 1.0f;
      if (dy > 0.5f) dy -= 1.0f;
      if (dy < -0.5f) dy += 1.0f;
      
      float dist = dx * dx + dy * dy;
      float maxDist = 0.08f * particleSize[hash];
      
      if (dist > maxDist) continue;
      
      // Enhanced pulse calculation with particle type variations
      float phase = particlePhases[hash];
      if (particleType[hash] == 2) {
        phase += (float)Math.sin(time * 3 + hash * 0.1) * 0.2f; // Wobbly particles
      }
      
      int pulseIdx = Math.abs((int)(phase * (PULSE_TABLE_SIZE - 1))) % PULSE_TABLE_SIZE;
      float pulseBright = pulseTable[pulseIdx];
      
      // Distance falloff with softer edges
      float falloff = (float)(1f - Math.pow(dist / maxDist, 0.8));
      float brightness = particleBrightness[hash] * pulseBright * falloff * intensityMult;
      
      if (brightness > maxBrightness) {
        maxBrightness = brightness;
        
        // Enhanced color calculation
        float baseHue = (hueShift.getValuef() + hash * 11.7f) % 360f;
        
        // Color variations by particle type
        switch (particleType[hash]) {
          case 0: // Standard particles
            bestHue = baseHue;
            bestSat = 75f + 20f * noiseTable[Math.abs(hash) % NOISE_TABLE_SIZE];
            break;
          case 1: // Turbulent particles - cooler colors
            bestHue = (baseHue + 180f) % 360f;
            bestSat = 85f + 15f * noiseTable[Math.abs(hash) % NOISE_TABLE_SIZE];
            break;
          case 2: // Wobbly particles - warmer colors
            bestHue = (baseHue + 60f) % 360f;
            bestSat = 90f + 10f * noiseTable[Math.abs(hash) % NOISE_TABLE_SIZE];
            break;
        }
      }
    }
    
    if (maxBrightness < 0.05f) return 0;
    
    return LXColor.hsb(bestHue, bestSat, Math.min(100f, maxBrightness * 120f));
  }
  
  private int blendGeometryAndParticles(float geometryBrightness, int particleColor, float u, float v) {
    // Extract particle color components
    float particleBrightness = LXColor.b(particleColor) / 100.0f;
    float particleHue = LXColor.h(particleColor);
    float particleSat = LXColor.s(particleColor);
    
    // Geometry color - starts with structured patterns
    float geometryHue = (hueShift.getValuef() + u * 120f + v * 80f + time * 10f) % 360f;
    float geometrySat = 60f + 30f * (float)Math.sin(time * 0.5f + u * Math.PI * 4);
    
    // Blend based on which is brighter and dispersion setting
    float disp = dispersion.getValuef();
    float totalBrightness = Math.max(geometryBrightness, particleBrightness);
    
    if (totalBrightness < 0.02f) return 0;
    
    // As dispersion increases, particles dominate over geometry
    float particleWeight = disp * (particleBrightness / (particleBrightness + geometryBrightness + 0.01f));
    float geometryWeight = 1f - particleWeight;
    
    // Color blending
    float finalHue = particleWeight * particleHue + geometryWeight * geometryHue;
    float finalSat = particleWeight * particleSat + geometryWeight * geometrySat;
    float finalBrightness = totalBrightness * intensity.getValuef();
    
    // Add some sparkle effect for high-intensity particles
    if (particleBrightness > 0.8f && Math.random() < 0.1) {
      finalBrightness *= 1.5f;
      finalSat = Math.min(100f, finalSat * 1.2f);
    }
    
    return LXColor.hsb(finalHue % 360f, Math.min(100f, finalSat), Math.min(100f, finalBrightness * 100f));
  }
}
