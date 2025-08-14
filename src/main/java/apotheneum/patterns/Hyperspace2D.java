package apotheneum.patterns;

import apotheneum.Apotheneum;
import apotheneum.ApotheneumPattern;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponentName;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import java.util.ArrayList;
import java.util.List;

@LXCategory("Apotheneum")
@LXComponentName("Hyperspace2D")
public class Hyperspace2D extends ApotheneumPattern implements UIDeviceControls<Hyperspace2D> {

  // Star particle moving in 2D face space
  private static class Star {
    float x, y;           // Position on face (0-1 normalized)
    float vx, vy;         // Velocity (NEVER changes after creation)
    float speed;          // Individual speed multiplier
    int color;            // Star color
    double age;           // Current age in milliseconds
    double lifespan;      // Total lifespan in milliseconds
    float prevX, prevY;   // Previous position for trail
    int currentFace;      // Which face the star is currently on
    int facesVisited;     // How many different faces this star has been on
    float twinklePhase;   // Random phase offset for twinkle oscillation
    float twinkleFreq;    // Individual twinkle frequency multiplier
    
    Star(float sourceX, float sourceY, float spreadRadius, double maxLifespan) {
      // Pick a random direction first
      float angle = (float)(Math.random() * 2 * Math.PI);
      this.vx = (float)Math.cos(angle);
      this.vy = (float)Math.sin(angle);
      
      // Start at random distance from source point IN THE SAME DIRECTION as movement
      float spreadDistance = (float)Math.random() * spreadRadius;
      
      this.x = sourceX + this.vx * spreadDistance;
      this.y = sourceY + this.vy * spreadDistance;
      
      // Initialize previous position
      this.prevX = this.x;
      this.prevY = this.y;
      
      // Initialize face tracking
      this.currentFace = getCurrentFace(this.x, this.y);
      this.facesVisited = 1; // Starting on first face
      
      // Direction is already set and guaranteed to point away from source
      
      this.speed = 0.8f + (float)Math.random() * 0.4f; // 0.8-1.2x speed
      this.age = 0;
      this.lifespan = Math.random() * maxLifespan + maxLifespan * 0.5; // 50%-150% of max
      
      // Pure white stars
      float brightness = 0.8f + (float)Math.random() * 0.2f;
      this.color = LXColor.rgb(
        (int)(brightness * 255),
        (int)(brightness * 255),
        (int)(brightness * 255)
      );
      
      // Initialize twinkle properties
      this.twinklePhase = (float)(Math.random() * Math.PI * 2); // Random phase
      this.twinkleFreq = 0.5f + (float)Math.random() * 1.0f;   // 0.5x to 1.5x base speed
    }
    
    void update(double deltaMs, float baseSpeed) {
      age += deltaMs;
      
      // Store previous position for trail
      prevX = x;
      prevY = y;
      
      // Move in straight line - velocity NEVER changes
      float currentSpeed = baseSpeed * speed;
      x += vx * currentSpeed;
      y += vy * currentSpeed;
      
      // Check if we've moved to a new face
      int newFace = getCurrentFace(x, y);
      if (newFace != currentFace && newFace != -1) {
        currentFace = newFace;
        facesVisited++;
      }
    }
    
    boolean isAlive() {
      // Stars disappear when they reach the 3rd face (after visiting 1st and 2nd)
      // Also disappear if they go too far from the main face area
      boolean withinReasonableBounds = x > -1.5f && x < 2.5f && y > -1.5f && y < 2.5f;
      return age < lifespan && facesVisited <= 2 && withinReasonableBounds;
    }
    
    // Determine which face a star is on based on its coordinates
    int getCurrentFace(float x, float y) {
      // Convert coordinates to face indices
      // For cube: 4 faces around, for cylinder: wraps around
      // This is a simplified face detection - you might need to adjust based on your geometry
      
      if (x < 0 || x > 1 || y < 0 || y > 1) {
        // Outside main face area - could be on adjacent faces
        // Simple mapping: negative X = face -1, > 1 X = face +1, etc.
        if (x < 0) return -1; // Left adjacent face
        if (x > 1) return 1;  // Right adjacent face  
        if (y < 0) return -2; // Top adjacent face
        if (y > 1) return 2;  // Bottom adjacent face
      }
      
      return 0; // Main starting face
    }
    
    float getBrightness() {
      // Fade in/out based on age
      double lifeFraction = age / lifespan;
      float baseBrightness;
      if (lifeFraction < 0.1) {
        baseBrightness = (float)(lifeFraction / 0.1);
      } else if (lifeFraction > 0.9) {
        baseBrightness = (float)((1.0 - lifeFraction) / 0.1);
      } else {
        baseBrightness = 1.0f;
      }
      return baseBrightness;
    }
    
    float getTwinkleBrightness(double currentTime, float twinkleIntensity, float twinkleSpeed) {
      if (twinkleIntensity < 0.01f) {
        return getBrightness(); // No twinkle
      }
      
      // Calculate oscillating twinkle multiplier
      float twinkleTime = (float)(currentTime * 0.001 * twinkleSpeed * twinkleFreq);
      float twinkleOscillation = (float)Math.sin(twinkleTime + twinklePhase);
      
      // Convert from -1,1 to a brightness multiplier
      float twinkleMultiplier = 1.0f + (twinkleOscillation * twinkleIntensity * 0.8f);
      twinkleMultiplier = Math.max(0.2f, twinkleMultiplier); // Don't go completely dark
      
      return getBrightness() * twinkleMultiplier;
    }
  }
  
  private final List<Star> stars = new ArrayList<>();
  private double currentTime = 0; // Track time for twinkle animation
  
  // Parameters
  public final CompoundParameter sourceX = new CompoundParameter("Source X", 0.5, 0, 1)
    .setDescription("X position of star source (0=left, 1=right)");
    
  public final CompoundParameter sourceY = new CompoundParameter("Source Y", 0.5, 0, 1)
    .setDescription("Y position of star source (0=top, 1=bottom)");
    
  public final CompoundParameter sourceZ = new CompoundParameter("Source Z", 0.5, 0, 1)
    .setDescription("Z depth of star source (affects which face)");
    
  public final CompoundParameter speed = new CompoundParameter("Speed", 0.5, 0.1, 5.0)
    .setDescription("Speed of star movement");
    
  public final CompoundParameter density = new CompoundParameter("Density", 50, 10, 1000)
    .setDescription("Rate of star spawning");
    
  public final CompoundParameter duration = new CompoundParameter("Duration", 3000, 1000, 8000)
    .setDescription("How long stars live (milliseconds)");
    
  public final CompoundParameter brightness = new CompoundParameter("Bright", 1.0, 0.1, 2.0)
    .setDescription("Overall brightness");
    
  public final CompoundParameter trailLength = new CompoundParameter("Trail", 0.0, 0.0, 10.0)
    .setDescription("Length of star trails");
    
  public final CompoundParameter spreadRadius = new CompoundParameter("Spread", 0.1, 0.02, 0.3)
    .setDescription("Random spread distance from source point");
    
  public final BooleanParameter debugSource = new BooleanParameter("Debug", false)
    .setDescription("Show source point in red");
    
  public final CompoundParameter twinkleIntensity = new CompoundParameter("Twinkle", 0.0, 0.0, 1.0)
    .setDescription("Star twinkle intensity");
    
  public final CompoundParameter twinkleSpeed = new CompoundParameter("T Speed", 1.0, 0.1, 5.0)
    .setDescription("Star twinkle speed");
    
  public final DiscreteParameter shape = new DiscreteParameter("Shape",
    new String[] { "Cube", "Cylinder" }, 0)
    .setDescription("Which shape to render on");
  
  private double lastSpawnTime = 0;
  
  public Hyperspace2D(LX lx) {
    super(lx);
    addParameter("sourceX", this.sourceX);
    addParameter("sourceY", this.sourceY);
    addParameter("sourceZ", this.sourceZ);
    addParameter("speed", this.speed);
    addParameter("density", this.density);
    addParameter("duration", this.duration);
    addParameter("brightness", this.brightness);
    addParameter("trailLength", this.trailLength);
    addParameter("spreadRadius", this.spreadRadius);
    addParameter("debugSource", this.debugSource);
    addParameter("twinkleIntensity", this.twinkleIntensity);
    addParameter("twinkleSpeed", this.twinkleSpeed);
    addParameter("shape", this.shape);
  }
  
  @Override
  protected void render(double deltaMs) {
    // Update time for twinkle animation
    currentTime += deltaMs;
    
    // Apply feedback effect for trails instead of clearing all
    float trailAmount = (float)trailLength.getValue();
    if (trailAmount > 0.01f) {
      // Hard trail with quick falloff - aggressive fade
      float fadeAmount = 0.5f; // Very fast fade for sharp trail cutoff
      
      for (int i = 0; i < colors.length; i++) {
        if (colors[i] != 0) {
          colors[i] = LXColor.scaleBrightness(colors[i], fadeAmount);
        }
      }
    } else {
      setApotheneumColor(0); // Clear all if no trails
    }
    
    updateStars(deltaMs);
    renderStars();
    
    // Show debug source if enabled
    if (debugSource.isOn()) {
      renderDebugSource();
    }
  }
  
  private void updateStars(double deltaMs) {
    float baseSpeed = (float)(speed.getValue() * deltaMs * 0.0001);
    double maxLifespan = duration.getValue();
    
    // Spawn new stars
    double spawnRate = density.getValue() * deltaMs * 0.001;
    if (Math.random() < spawnRate) {
      float srcX = (float)sourceX.getValue();
      float srcY = (float)sourceY.getValue();
      float spread = (float)spreadRadius.getValue();
      stars.add(new Star(srcX, srcY, spread, maxLifespan));
    }
    
    // Update existing stars and remove dead ones
    for (int i = stars.size() - 1; i >= 0; i--) {
      Star star = stars.get(i);
      star.update(deltaMs, baseSpeed);
      
      if (!star.isAlive()) {
        stars.remove(i);
      }
    }
  }
  
  private void renderStars() {
    float brightnessMult = (float)brightness.getValue();
    float trailAmount = (float)trailLength.getValue();
    float twinkleIntensityVal = (float)twinkleIntensity.getValue();
    float twinkleSpeedVal = (float)twinkleSpeed.getValue();
    int ringLength = getRingLength();
    int ringHeight = getRingHeight();
    
    for (Star star : stars) {
      // Render current star position
      int faceX = (int)(star.x * (ringLength - 1));
      int faceY = (int)(star.y * (ringHeight - 1));
      
      // Only render if within the actual visible bounds
      if (faceX >= 0 && faceX < ringLength && faceY >= 0 && faceY < ringHeight) {
        // Use twinkle brightness instead of regular brightness
        float starBrightness = star.getTwinkleBrightness(currentTime, twinkleIntensityVal, twinkleSpeedVal) * brightnessMult;
        int starColor = LXColor.scaleBrightness(star.color, starBrightness);
        
        drawStarAtPosition(faceX, faceY, starColor, 0.5f);
        
        // If trails enabled, also draw a few previous positions for harder trail
        if (trailAmount > 0.01f) {
          int trailSteps = Math.min(5, (int)(trailAmount * 2)); // Max 5 trail points
          
          for (int i = 1; i <= trailSteps; i++) {
            float trailMult = (float)i / trailSteps;
            int trailX = (int)((star.x - star.vx * 0.02f * i) * (ringLength - 1));
            int trailY = (int)((star.y - star.vy * 0.02f * i) * (ringHeight - 1));
            
            if (trailX >= 0 && trailX < ringLength && trailY >= 0 && trailY < ringHeight) {
              // Trail uses base brightness (no twinkle on trails)
              float trailBrightness = star.getBrightness() * brightnessMult * (1.0f - trailMult * 0.8f);
              int trailColor = LXColor.scaleBrightness(star.color, trailBrightness);
              drawStarAtPosition(trailX, trailY, trailColor, 0.5f);
            }
          }
        }
      }
    }
  }
  
  private void drawStarAtPosition(float x, float y, int color, float size) {
    // Force to exact integer positions for sharp rendering
    int pixelX = (int)x;
    int pixelY = (int)y;
    
    // For sharp, non-blurry stars, only draw at exact pixel positions
    setPixelOnShape(pixelX, pixelY, color);
    
    // Only add neighboring pixels if size > 1.0 for larger stars
    if (size > 1.0f) {
      // Use a cross pattern instead of circle for sharper edges
      setPixelOnShape(pixelX + 1, pixelY, color);
      setPixelOnShape(pixelX - 1, pixelY, color);
      setPixelOnShape(pixelX, pixelY + 1, color);
      setPixelOnShape(pixelX, pixelY - 1, color);
    }
  }
  
  private void setPixelOnShape(int ringX, int ringY, int color) {
    // Use exact integer positions to eliminate blur
    int ringIndex = ringY;
    int pointIndex = ringX;
    
    if (shape.getValuei() == 0) {
      // Cube rendering
      if (ringIndex >= 0 && ringIndex < Apotheneum.GRID_HEIGHT) {
        setPixelOnRing(Apotheneum.cube.exterior.ring(ringIndex), pointIndex, color);
        setPixelOnRing(Apotheneum.cube.interior.ring(ringIndex), pointIndex, color);
      }
    } else {
      // Cylinder rendering
      if (ringIndex >= 0 && ringIndex < Apotheneum.CYLINDER_HEIGHT) {
        setPixelOnRing(Apotheneum.cylinder.exterior.ring(ringIndex), pointIndex, color);
        setPixelOnRing(Apotheneum.cylinder.interior.ring(ringIndex), pointIndex, color);
      }
    }
  }
  
  private void setPixelOnRing(Apotheneum.Ring ring, int pointIndex, int color) {
    if (ring != null && ring.points.length > 0) {
      // Don't wrap around - only render if within the actual ring bounds
      if (pointIndex >= 0 && pointIndex < ring.points.length) {
        // Replace color instead of blending to avoid accumulated brightness
        colors[ring.points[pointIndex].index] = color;
      }
      // If pointIndex is outside bounds, don't render (star is on different face)
    }
  }
  
  private void renderDebugSource() {
    float srcX = (float)sourceX.getValue();
    float srcY = (float)sourceY.getValue();
    
    // Convert to face coordinates
    int ringLength = getRingLength();
    int ringHeight = getRingHeight();
    
    float faceX = srcX * (ringLength - 1);
    float faceY = srcY * (ringHeight - 1);
    
    int redColor = 0xFFFF0000; // Bright red
    
    // Draw a cross at the source location
    drawStarAtPosition(faceX, faceY, redColor, 1.5f);
    
    // Add cross lines for better visibility
    if (faceX > 2) setPixelOnShape((int)(faceX - 3), (int)faceY, redColor);
    if (faceX < ringLength - 3) setPixelOnShape((int)(faceX + 3), (int)faceY, redColor);
    if (faceY > 2) setPixelOnShape((int)faceX, (int)(faceY - 3), redColor);
    if (faceY < ringHeight - 3) setPixelOnShape((int)faceX, (int)(faceY + 3), redColor);
  }
  
  // Dynamic coordinate system based on shape selection
  private int getRingHeight() {
    return shape.getValuei() == 0 ? Apotheneum.GRID_HEIGHT : Apotheneum.CYLINDER_HEIGHT;
  }
  
  private int getRingLength() {
    return shape.getValuei() == 0 ? Apotheneum.Cube.Ring.LENGTH : Apotheneum.Cylinder.Ring.LENGTH;
  }
  
  @Override
  public void buildDeviceControls(UI ui, UIDevice uiDevice, Hyperspace2D pattern) {
    uiDevice.setLayout(UIDevice.Layout.HORIZONTAL, 4);
    
    // Source controls
    addColumn(uiDevice, "Source",
      newKnob(pattern.sourceX),
      newKnob(pattern.sourceY),
      newKnob(pattern.sourceZ)).setChildSpacing(6);
    
    addVerticalBreak(ui, uiDevice);
    
    // Movement controls
    addColumn(uiDevice, "Movement",
      newKnob(pattern.speed),
      newKnob(pattern.density),
      newButton(pattern.debugSource).setTriggerable(true)).setChildSpacing(6);
    
    addVerticalBreak(ui, uiDevice);
    
    // Visual controls
    addColumn(uiDevice, "Visual",
      newKnob(pattern.duration),
      newKnob(pattern.trailLength),
      newKnob(pattern.brightness)).setChildSpacing(6);
    
    addVerticalBreak(ui, uiDevice);
    
    // Twinkle controls
    addColumn(uiDevice, "Twinkle",
      newKnob(pattern.twinkleIntensity),
      newKnob(pattern.twinkleSpeed),
      newKnob(pattern.spreadRadius)).setChildSpacing(6);
    
    addVerticalBreak(ui, uiDevice);
    
    // Shape controls
    addColumn(uiDevice, "Shape",
      newDropMenu(pattern.shape)).setChildSpacing(6);
  }
}