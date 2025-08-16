package apotheneum.patterns;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponentName;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.pattern.LXPattern;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import java.util.ArrayList;
import java.util.List;

@LXCategory("Apotheneum")
@LXComponentName("Hyperspace")
public class Hyperspace extends LXPattern implements UIDeviceControls<Hyperspace> {
  
  // Star particle in 3D space
  private static class Star {
    float x, y, z;  // Position in model space (0-1)
    float vx, vy, vz;  // Velocity
    float speed;    // Individual star speed multiplier
    int color;      // Star color
    double lifespan; // Total lifespan in milliseconds
    double age;     // Current age in milliseconds
    float prevX, prevY, prevZ; // Previous position for trail
    
    Star(double maxLifespan, LXPoint[] allPoints) {
      this.lifespan = Math.random() * maxLifespan + maxLifespan * 0.5; // 50%-150% of max
      reset();
    }
    
    void reset() {
      // Start at random position throughout space
      x = (float)Math.random();
      y = (float)Math.random(); 
      z = (float)Math.random();
      
      // Initialize previous position to current
      prevX = x;
      prevY = y;
      prevZ = z;
      
      // Stars don't have individual velocities - they're static
      // Only the motion controls move them
      vx = 0;
      vy = 0;
      vz = 0;
      
      speed = 0.5f + (float)Math.random() * 1.0f; // Individual speed variation
      age = 0; // Reset age
      
      // Pure white stars with brightness variation
      float brightness = 0.8f + (float)Math.random() * 0.2f;
      color = LXColor.rgb(
        (int)(brightness * 255),
        (int)(brightness * 255),
        (int)(brightness * 255)
      );
    }
    
    void update(double deltaMs, float baseSpeed, double maxLifespan, int axis, float direction) {
      // Age the star
      age += deltaMs;
      
      // Update lifespan if parameter changed
      if (lifespan > maxLifespan * 1.5 || lifespan < maxLifespan * 0.5) {
        lifespan = Math.random() * maxLifespan + maxLifespan * 0.5;
      }
      
      float currentSpeed = baseSpeed * speed;
      
      // Store previous position for trail
      prevX = x;
      prevY = y;
      prevZ = z;
      
      // Motion control moves the entire star field in one axis
      // Stars themselves are static - only the field moves
      float movement = direction * currentSpeed;
      
      switch (axis) {
        case 0: // X axis
          x += movement;
          break;
        case 1: // Y axis
          y += movement;
          break;
        case 2: // Z axis
          z += movement;
          break;
      }
      
      // Reset if out of bounds or exceeded lifespan
      // Wrap around - when stars exit one side, they enter the opposite side
      if (x < -0.2f) x = 1.2f;
      if (x > 1.2f) x = -0.2f;
      if (y < -0.2f) y = 1.2f;
      if (y > 1.2f) y = -0.2f;
      if (z < -0.2f) z = 1.2f;
      if (z > 1.2f) z = -0.2f;
      
      if (age >= lifespan) {
        reset();
      }
    }
    
    float getBrightness() {
      // Smooth fade in/out
      double lifeFraction = age / lifespan;
      
      if (lifeFraction < 0.1) {
        // Fade in over first 10% of life
        return (float)(lifeFraction / 0.1);
      } else if (lifeFraction > 0.9) {
        // Fade out over last 10% of life
        return (float)((1.0 - lifeFraction) / 0.1);
      } else {
        // Full brightness in middle
        return 1.0f;
      }
    }
  }
  
  private final List<Star> stars = new ArrayList<>();
  private LXPoint[] allPoints; // Cache of all LED points for targeting
  
  public final CompoundParameter speed = new CompoundParameter("Speed", 0.5, 0.1, 10.0)
    .setDescription("Speed of hyperspace travel");
    
  public final CompoundParameter density = new CompoundParameter("Density", 100, 10, 800)
    .setDescription("Number of stars");
    
  public final CompoundParameter starSize = new CompoundParameter("Star Size", 0.1, 0.05, 0.3)
    .setDescription("Size of stars and trails");
    
  public final CompoundParameter duration = new CompoundParameter("Duration", 3000, 1000, 8000)
    .setDescription("How long stars live (milliseconds)");
    
  public final CompoundParameter brightness = new CompoundParameter("Bright", 1.0, 0.1, 2.0)
    .setDescription("Overall brightness");
    
  public final CompoundParameter trailLength = new CompoundParameter("Trail", 0.0, 0.0, 1.0)
    .setDescription("Length of star trails");
    
  public final BooleanParameter pulse = new BooleanParameter("Pulse", false)
    .setDescription("Pulsing speed effect");
    
  public final CompoundParameter motionAxis = new CompoundParameter("Axis", 0, 0, 2)
    .setDescription("Motion axis: 0=X, 1=Y, 2=Z");
    
  public final CompoundParameter motionDirection = new CompoundParameter("Direction", 1, -1, 1)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setPolarity(CompoundParameter.Polarity.BIPOLAR)
    .setDescription("Motion direction: -1=Negative, +1=Positive");
  
  private double pulsePhase = 0;
  
  public Hyperspace(LX lx) {
    super(lx);
    addParameter("speed", this.speed);
    addParameter("density", this.density);
    addParameter("starSize", this.starSize);
    addParameter("duration", this.duration);
    addParameter("brightness", this.brightness);
    addParameter("trailLength", this.trailLength);
    addParameter("pulse", this.pulse);
    addParameter("motionAxis", this.motionAxis);
    addParameter("motionDirection", this.motionDirection);
    
    // Cache all LED points for star targeting
    allPoints = model.points;
    
    updateStarCount();
  }
  
  private void updateStarCount() {
    int targetCount = (int)density.getValue();
    double maxLifespan = duration.getValue();
    
    while (stars.size() < targetCount) {
      stars.add(new Star(maxLifespan, allPoints));
    }
    while (stars.size() > targetCount) {
      stars.remove(stars.size() - 1);
    }
  }
  
  @Override
  public void onParameterChanged(heronarts.lx.parameter.LXParameter parameter) {
    if (parameter == density) {
      updateStarCount();
    }
    super.onParameterChanged(parameter);
  }
  
  @Override
  protected void run(double deltaMs) {
    // Update pulse phase
    if (pulse.isOn()) {
      pulsePhase += deltaMs * 0.003;
    }
    
    // Calculate current speed with optional pulse
    float currentSpeed = (float)(speed.getValue() * deltaMs * 0.0001);
    if (pulse.isOn()) {
      currentSpeed *= 1.0f + (float)Math.sin(pulsePhase) * 0.5f;
    }
    
    // Update all stars
    double maxLifespan = duration.getValue();
    int axis = (int)motionAxis.getValue();
    float direction = (float)motionDirection.getValue();
    
    for (Star star : stars) {
      star.update(deltaMs, currentSpeed, maxLifespan, axis, direction);
    }
    
    // Clear all points first
    for (LXPoint p : model.points) {
      colors[p.index] = 0;
    }
    
    // Now render each star as a sharp point with optional trails
    float brightnessMult = (float)brightness.getValue();
    float trailAmount = (float)trailLength.getValue();
    
    for (Star star : stars) {
      // Only render stars that are reasonably close to the visible cube
      // This allows stars outside the cube to exist but not waste computation
      if (star.x >= -0.2f && star.x <= 1.2f && 
          star.y >= -0.2f && star.y <= 1.2f && 
          star.z >= -0.2f && star.z <= 1.2f) {
        
        // Render trail if enabled
        if (trailAmount > 0.01f) {
          // Simplified trail - just use 2-3 points for performance
          int trailSteps = Math.min(2, (int)(trailAmount * 3)); // Max 2 trail points
          
          for (int i = 0; i <= trailSteps; i++) {
            float t = (float)i / trailSteps;
            
            // Interpolate position along trail
            float trailX = star.prevX + (star.x - star.prevX) * t;
            float trailY = star.prevY + (star.y - star.prevY) * t;
            float trailZ = star.prevZ + (star.z - star.prevZ) * t;
            
            // Quick bounds check
            if (trailX < 0 || trailX > 1 || trailY < 0 || trailY > 1 || trailZ < 0 || trailZ > 1) {
              continue;
            }
            
            // Find closest LED using a smaller search radius
            float minDistance = 0.01f; // Only look for very close points
            int closestIndex = -1;
            
            for (LXPoint p : model.points) {
              float dx = p.xn - trailX;
              float dy = p.yn - trailY;
              float dz = p.zn - trailZ;
              float distance = dx*dx + dy*dy + dz*dz;
              
              if (distance < minDistance) {
                minDistance = distance;
                closestIndex = p.index;
                break; // Take first close-enough point
              }
            }
            
            // Render trail point with fading brightness
            if (closestIndex >= 0) {
              float trailBrightness = star.getBrightness() * brightnessMult * (0.3f + 0.7f * t); // Fade from 30% to 100%
              int trailColor = LXColor.scaleBrightness(star.color, trailBrightness);
              colors[closestIndex] = LXColor.blend(colors[closestIndex], trailColor, LXColor.Blend.ADD);
            }
          }
        } else {
          // No trail - just render the star point
          float minDistance = Float.MAX_VALUE;
          int closestIndex = -1;
          
          for (LXPoint p : model.points) {
            float dx = p.xn - star.x;
            float dy = p.yn - star.y;
            float dz = p.zn - star.z;
            float distance = dx*dx + dy*dy + dz*dz;
            
            if (distance < minDistance) {
              minDistance = distance;
              closestIndex = p.index;
            }
          }
          
          if (closestIndex >= 0) {
            float starBrightness = star.getBrightness() * brightnessMult;
            int starColorWithBrightness = LXColor.scaleBrightness(star.color, starBrightness);
            colors[closestIndex] = LXColor.blend(colors[closestIndex], starColorWithBrightness, LXColor.Blend.ADD);
          }
        }
      }
    }
  }
  
  @Override
  public void buildDeviceControls(UI ui, UIDevice uiDevice, Hyperspace pattern) {
    uiDevice.setLayout(UIDevice.Layout.HORIZONTAL, 4);
    
    // Movement controls
    addColumn(uiDevice, "Movement",
      newKnob(pattern.speed),
      newKnob(pattern.density),
      newKnob(pattern.duration)).setChildSpacing(6);
    
    addVerticalBreak(ui, uiDevice);
    
    // Motion controls
    addColumn(uiDevice, "Motion",
      newKnob(pattern.motionAxis),
      newKnob(pattern.motionDirection)).setChildSpacing(6);
    
    addVerticalBreak(ui, uiDevice);
    
    // Visual controls
    addColumn(uiDevice, "Visual",
      newKnob(pattern.brightness),
      newKnob(pattern.trailLength),
      newButton(pattern.pulse).setTriggerable(true)).setChildSpacing(6);
  }
}