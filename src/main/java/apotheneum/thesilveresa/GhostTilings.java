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
@LXComponentName("Ghost Tilings")
public class GhostTilings extends ApotheneumPattern {

  private final CompoundParameter speed = new CompoundParameter("Speed", 0.15, 0.02, 0.8)
    .setDescription("Pattern drift speed");
  private final CompoundParameter tileScale = new CompoundParameter("Scale", 5.0, 2.0, 12.0)
    .setDescription("Tile size");
  private final CompoundParameter transparency = new CompoundParameter("Alpha", 0.25, 0.05, 0.6)
    .setDescription("Pattern transparency");
  private final CompoundParameter layers = new CompoundParameter("Layers", 3.0, 1.0, 6.0)
    .setDescription("Number of overlay layers");
  private final CompoundParameter drift = new CompoundParameter("Drift", 0.3, 0.0, 1.0)
    .setDescription("Layer drift amount");
  private final BooleanParameter hexMode = new BooleanParameter("Hexagonal", false)
    .setDescription("Use hexagonal tiling");
  private final CompoundParameter ghostHue = new CompoundParameter("Hue", 260.0, 0.0, 360.0)
    .setDescription("Ghost tiling hue");

  // Tiling patterns - geometric shapes that can overlay
  private static final int[][] TILE_PATTERNS = {
    // Diamond
    {0,1,0, 1,1,1, 0,1,0},
    // Plus
    {0,1,0, 1,1,1, 0,1,0},
    // Square border
    {1,1,1, 1,0,1, 1,1,1},
    // X pattern
    {1,0,1, 0,1,0, 1,0,1},
    // Dots
    {1,0,1, 0,0,0, 1,0,1},
    // Lines
    {0,0,0, 1,1,1, 0,0,0}
  };
  
  // Optimization: Pre-computed values
  private static final int PATTERN_SIZE = 3;
  private float[] driftOffsets;
  private float[] layerAlphas;
  private float[] layerSpeeds;
  private float time = 0f;

  public GhostTilings(LX lx) {
    super(lx);
    addParameter("Speed", this.speed);
    addParameter("Scale", this.tileScale);
    addParameter("Alpha", this.transparency);
    addParameter("Layers", this.layers);
    addParameter("Drift", this.drift);
    addParameter("Hexagonal", this.hexMode);
    addParameter("Hue", this.ghostHue);
    
    initializeLayers();
  }
  
  private void initializeLayers() {
    int maxLayers = 6;
    driftOffsets = new float[maxLayers];
    layerAlphas = new float[maxLayers];
    layerSpeeds = new float[maxLayers];
    
    for (int i = 0; i < maxLayers; i++) {
      driftOffsets[i] = i * 0.3f; // Stagger the layers
      layerAlphas[i] = 1f / (i + 1f); // Fade deeper layers
      layerSpeeds[i] = 1f + i * 0.2f; // Vary speeds slightly
    }
  }

  @Override
  protected void render(double deltaMs) {
    float dt = (float)(deltaMs / 1000.0);
    time += dt * speed.getValuef();
    
    // Optimization: Same tiling logic for all faces
    Cube cube = Apotheneum.cube;
    if (cube != null) {
      // Render reference face
      if (cube.exterior.faces.length > 0) {
        Face referenceFace = cube.exterior.faces[0];
        processFace(referenceFace);
        
        // Copy to other exterior faces
        for (int i = 1; i < cube.exterior.faces.length; i++) {
          Face currentFace = cube.exterior.faces[i];
          // Ensure both faces have the same structure before copying
          if (currentFace.rows.length == referenceFace.rows.length) {
            for (int rowIndex = 0; rowIndex < currentFace.rows.length; rowIndex++) {
              Row currentRow = currentFace.rows[rowIndex];
              Row referenceRow = referenceFace.rows[rowIndex];
              
              // Ensure both rows have the same number of points
              int pointsToProcess = Math.min(currentRow.points.length, referenceRow.points.length);
              for (int cx = 0; cx < pointsToProcess; cx++) {
                colors[currentRow.points[cx].index] = colors[referenceRow.points[cx].index];
              }
            }
          } else {
            // If faces have different structures, process individually
            processFace(currentFace);
          }
        }
        
        // Copy exterior to interior
        if (cube.interior != null) {
          for (Face interiorFace : cube.interior.faces) {
            // Check if interior face structure matches reference face
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
              // If structures don't match, process individually
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
        
        int color = calculateTilingColor(u, v);
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
        
        int color = calculateTilingColor(u, v);
        colors[p.index] = color;
      }
    }
  }
  
  private int calculateTilingColor(float u, float v) {
    float scale = tileScale.getValuef();
    float alpha = transparency.getValuef();
    float driftAmount = drift.getValuef();
    int numLayers = (int)layers.getValuef();
    boolean isHex = hexMode.getValueb();
    
    float totalBrightness = 0f;
    float totalHue = ghostHue.getValuef();
    
    // Layer multiple transparent tilings
    for (int layer = 0; layer < numLayers; layer++) {
      float layerTime = time * layerSpeeds[layer];
      float layerDrift = driftOffsets[layer] + driftAmount * layerTime * 0.1f;
      
      // Apply drift to coordinates
      float layerU = u + layerDrift * 0.2f;
      float layerV = v + layerDrift * 0.15f;
      
      // Calculate tile coordinates
      float tileU, tileV;
      int patternIdx;
      
      if (isHex) {
        // Hexagonal tiling (approximate with offset rows)
        float hexScale = scale * 0.866f; // Hex adjustment
        int row = (int)(layerV * hexScale);
        float rowOffset = (row % 2) * 0.5f;
        tileU = (layerU + rowOffset) * scale;
        tileV = layerV * hexScale;
        patternIdx = (row * 3 + (int)tileU) % TILE_PATTERNS.length;
      } else {
        // Square tiling
        tileU = layerU * scale;
        tileV = layerV * scale;
        int tileX = (int)tileU;
        int tileY = (int)tileV;
        patternIdx = (tileX * 7 + tileY * 11 + layer) % TILE_PATTERNS.length;
      }
      
      // Get pattern and local position
      int[] pattern = TILE_PATTERNS[patternIdx];
      float localU = tileU - (int)tileU;
      float localV = tileV - (int)tileV;
      
      // Sample pattern at local position
      int patX = (int)(localU * PATTERN_SIZE);
      int patY = (int)(localV * PATTERN_SIZE);
      
      if (patX >= 0 && patX < PATTERN_SIZE && patY >= 0 && patY < PATTERN_SIZE) {
        int patternIndex = patY * PATTERN_SIZE + patX;
        if (patternIndex < pattern.length && pattern[patternIndex] > 0) {
          // Apply layer-specific alpha and fade
          float layerContrib = alpha * layerAlphas[layer];
          
          // Add subtle animation to make patterns "breathe"
          float breathe = 0.8f + 0.2f * (1f + (layerTime + layer) % 2f - 1f); // Linear triangle
          layerContrib *= breathe;
          
          totalBrightness += layerContrib;
          
          // Shift hue slightly per layer for depth effect
          totalHue += layer * 20f;
        }
      }
    }
    
    if (totalBrightness < 0.02f) return 0;
    
    // Ghost-like appearance with low saturation and brightness
    float saturation = 15f + 25f * totalBrightness;
    float brightness = Math.min(60f, totalBrightness * 150f);
    totalHue = totalHue % 360f;
    
    return LXColor.hsb(totalHue, saturation, brightness);
  }
}
