/**
 * Copyright 2025- Dan Oved
 *
 * This file is part of the LX Studio software library. By using
 * LX, you agree to the terms of the LX Studio Software License
 * and Distribution Agreement, available at: http://lx.studio/license
 *
 * Please note that the LX license is not open-source. The license
 * allows for free, non-commercial use.
 *
 * HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR
 * OTHERWISE, AND SPECIFICALLY DISCLAIMS ANY WARRANTY OF
 * MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR
 * PURPOSE, WITH RESPECT TO THE SOFTWARE.
 *
 * @author Dan Oved
 */

package apotheneum.doved.patterns;

import apotheneum.Apotheneum;
import apotheneum.ApotheneumPattern;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponentName;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.CompoundDiscreteParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import heronarts.lx.utils.LXUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@LXCategory("Apotheneum/doved")
@LXComponentName("Boids")
public class Boids extends ApotheneumPattern implements UIDeviceControls<Boids> {

  // Dynamic coordinate system based on shape selection
  private int getRingHeight() {
    return shape.getValuei() == 0 ? Apotheneum.GRID_HEIGHT : Apotheneum.CYLINDER_HEIGHT;
  }

  private int getRingLength() {
    return shape.getValuei() == 0 ? Apotheneum.Cube.Ring.LENGTH : Apotheneum.Cylinder.Ring.LENGTH;
  }

  // Parameters - optimized for tight flocking behavior with higher capacity
  public final CompoundDiscreteParameter boidCount =
    new CompoundDiscreteParameter("Count", 25, 5, 300)
    .setDescription("Number of boids in the flock");

  public final CompoundParameter speed =
    new CompoundParameter("Speed", 3.0, 0.5, 4.0)
    .setDescription("Base movement speed");

  public final CompoundParameter separation =
    new CompoundParameter("Separation", 1.5, 0, 4)
    .setDescription("Strength of separation force (avoid crowding)");

  public final CompoundParameter alignment =
    new CompoundParameter("Alignment", 1.8, 0, 3)
    .setDescription("Strength of alignment force (match heading)");

  public final CompoundParameter cohesion =
    new CompoundParameter("Cohesion", 1.0, 0, 3)
    .setDescription("Strength of cohesion force (move toward center)");

  public final CompoundParameter neighborRadius =
    new CompoundParameter("Radius", 15, 5, 30)
    .setDescription("Distance to consider other boids as neighbors");

  public final CompoundParameter turbulence =
    new CompoundParameter("Turbulence", 0.2, 0, 1)
    .setDescription("Random force for organic movement");


  public final DiscreteParameter shape = 
    new DiscreteParameter("Shape", new String[]{"Cube", "Cylinder"}, 0)
    .setDescription("Which shape to render on");

  public final CompoundParameter boidRadius =
    new CompoundParameter("Radius", 1.0, 0.5, 4.0)
    .setDescription("Size of boid rendering (larger = more pixels)");


  // Simple 2D Boid class
  private class Boid {
    float x, y;              // Position in ring coordinates
    float velocityX, velocityY;  // Velocity
    float maxSpeed = 15.0f;
    float maxForce = 0.8f;
    
    // Individual speed variation for more organic movement
    float currentSpeedMultiplier = 1.0f;  // Current speed multiplier (0.7 to 1.3)
    float targetSpeedMultiplier = 1.0f;   // Target speed to interpolate towards
    double lastSpeedTargetUpdate = 0;      // Time when we last picked a new target speed
    float speedInterpolationRate = 0.5f;  // How quickly we interpolate to target (0-1, higher = faster)
    
    
    Boid() {
      // Random initial position across the entire space
      x = (float)Math.random() * getRingLength();
      y = LXUtils.randomf(8, getRingHeight() - 8); // Stay away from edges
      
      // Random initial velocity for natural movement
      float initAngle = (float)(Math.random() * 2 * Math.PI);
      velocityX = (float)Math.cos(initAngle) * LXUtils.randomf(0.5f, 2.0f);
      velocityY = (float)Math.sin(initAngle) * LXUtils.randomf(0.5f, 2.0f);
      
      // Initialize individual speed variation for organic movement
      currentSpeedMultiplier = LXUtils.randomf(0.85f, 1.15f);
      targetSpeedMultiplier = currentSpeedMultiplier;
      lastSpeedTargetUpdate = 0;
    }
    
    void updateWithSpatialGrid(double deltaMs, SpatialGrid spatialGrid) {
      // Update individual speed variation
      updateSpeedVariation(Boids.this.currentTime);
      
      float accelerationX = 0;
      float accelerationY = 0;
      
      // Get nearby boids from spatial grid instead of checking all boids
      float searchRadius = neighborRadius.getValuef() * 2.0f;
      List<Boid> nearbyBoids = spatialGrid.getNearbyBoids(x, y, searchRadius);
      
      // Classic Boids: Apply the three fundamental forces to all boids
      float[] sep = separateFromNearby(nearbyBoids);
      float[] ali = alignWithNearby(nearbyBoids);
      float[] coh = cohesionWithNearby(nearbyBoids);
      
      accelerationX += sep[0] * separation.getValuef();
      accelerationY += sep[1] * separation.getValuef();
      accelerationX += ali[0] * alignment.getValuef();
      accelerationY += ali[1] * alignment.getValuef();
      accelerationX += coh[0] * cohesion.getValuef();
      accelerationY += coh[1] * cohesion.getValuef();
      
      // Add turbulence with extra vertical bias
      if (turbulence.getValuef() > 0) {
        accelerationX += (Math.random() - 0.5) * turbulence.getValuef() * 1.5f;
        accelerationY += (Math.random() - 0.5) * turbulence.getValuef() * 2.0f; // More vertical turbulence
      }
      
      // Door avoidance as acceleration force (before velocity update)
      if (isInDoorArea(x, y)) {
        accelerationY += maxForce * 0.5f; // Apply upward force to avoid doors
      }
      
      // Clamp total acceleration to prevent jittery movement at extreme parameter settings
      float totalAcceleration = (float)Math.sqrt(accelerationX * accelerationX + accelerationY * accelerationY);
      float maxAcceleration = maxForce * 3.0f; // Allow up to 3x maxForce for total acceleration
      if (totalAcceleration > maxAcceleration) {
        accelerationX = (accelerationX / totalAcceleration) * maxAcceleration;
        accelerationY = (accelerationY / totalAcceleration) * maxAcceleration;
      }
      
      // Update velocity
      velocityX += accelerationX;
      velocityY += accelerationY;
      
      // Limit speed (use base maxSpeed for consistent behavior)
      float speed = (float)Math.sqrt(velocityX * velocityX + velocityY * velocityY);
      if (speed > maxSpeed) {
        velocityX = (velocityX / speed) * maxSpeed;
        velocityY = (velocityY / speed) * maxSpeed;
      }
      
      // Update position (apply both global speed parameter and individual speed variation)
      float deltaSeconds = (float)(deltaMs * 0.001);
      float speedMultiplier = Boids.this.speed.getValuef() * currentSpeedMultiplier;
      x += velocityX * deltaSeconds * speedMultiplier;
      y += velocityY * deltaSeconds * speedMultiplier;
      
      // Handle boundaries - wrap X, gently redirect Y
      x = (x + getRingLength()) % getRingLength();
      if (y < 3) {
        y = 3;
        velocityY = Math.abs(velocityY) * 0.3f; // Gentle bounce
      }
      if (y > getRingHeight() - 4) {
        y = getRingHeight() - 4;
        velocityY = -Math.abs(velocityY) * 0.3f; // Gentle bounce
      }
    }
    
    void updateSpeedVariation(double currentTime) {
      // Pick a new target speed every 2-5 seconds
      double timeSinceLastTarget = currentTime - lastSpeedTargetUpdate;
      if (timeSinceLastTarget > LXUtils.randomf(2000, 5000)) {
        // Choose a new target speed within range [0.7, 1.3]
        targetSpeedMultiplier = LXUtils.randomf(0.7f, 1.3f);
        lastSpeedTargetUpdate = currentTime;
        
        // Vary the interpolation rate for different boids (some change speed faster than others)
        speedInterpolationRate = LXUtils.randomf(0.3f, 0.8f);
      }
      
      // Smoothly interpolate current speed towards target
      float speedDiff = targetSpeedMultiplier - currentSpeedMultiplier;
      float maxChange = speedInterpolationRate * 0.01f; // Small increments for smooth transitions
      
      if (Math.abs(speedDiff) > maxChange) {
        // Move towards target by maxChange amount
        currentSpeedMultiplier += Math.signum(speedDiff) * maxChange;
      } else {
        // Close enough to target, snap to it
        currentSpeedMultiplier = targetSpeedMultiplier;
      }
      
      // Ensure we stay within bounds
      currentSpeedMultiplier = Math.max(0.7f, Math.min(1.3f, currentSpeedMultiplier));
    }
    
    
    
    float[] separateFromNearby(List<Boid> nearbyBoids) {
      float desiredSeparation = 6.0f; // Increased separation to prevent clustering
      float steerX = 0, steerY = 0;
      int count = 0;
      
      for (Boid other : nearbyBoids) {
        if (other == this) continue;
        
        float dx = x - other.x;
        float dy = y - other.y;
        
        // Wrap distance for X
        if (Math.abs(dx) > getRingLength() / 2) {
          dx = dx > 0 ? dx - getRingLength() : dx + getRingLength();
        }
        
        float distance = (float)Math.sqrt(dx * dx + dy * dy);
        if (distance > 0 && distance < desiredSeparation) {
          dx /= distance;
          dy /= distance;
          dx /= distance; // Weight by distance
          dy /= distance;
          steerX += dx;
          steerY += dy;
          count++;
        }
      }
      
      if (count > 0) {
        steerX /= count;
        steerY /= count;
        
        float mag = (float)Math.sqrt(steerX * steerX + steerY * steerY);
        if (mag > 0) {
          steerX = (steerX / mag) * maxSpeed - velocityX;
          steerY = (steerY / mag) * maxSpeed - velocityY;
          
          float steerMag = (float)Math.sqrt(steerX * steerX + steerY * steerY);
          if (steerMag > maxForce) {
            steerX = (steerX / steerMag) * maxForce;
            steerY = (steerY / steerMag) * maxForce;
          }
        }
      }
      
      return new float[]{steerX, steerY};
    }
    
    float[] alignWithNearby(List<Boid> nearbyBoids) {
      float neighborDist = neighborRadius.getValuef();
      float sumX = 0, sumY = 0;
      int count = 0;
      
      for (Boid other : nearbyBoids) {
        if (other == this) continue;
        
        float dx = x - other.x;
        float dy = y - other.y;
        
        if (Math.abs(dx) > getRingLength() / 2) {
          dx = dx > 0 ? dx - getRingLength() : dx + getRingLength();
        }
        
        float distance = (float)Math.sqrt(dx * dx + dy * dy);
        if (distance > 0 && distance < neighborDist) {
          sumX += other.velocityX;
          sumY += other.velocityY;
          count++;
        }
      }
      
      if (count > 0) {
        sumX /= count;
        sumY /= count;
        
        float mag = (float)Math.sqrt(sumX * sumX + sumY * sumY);
        if (mag > 0) {
          sumX = (sumX / mag) * maxSpeed;
          sumY = (sumY / mag) * maxSpeed;
          
          float steerX = sumX - velocityX;
          float steerY = sumY - velocityY;
          
          float steerMag = (float)Math.sqrt(steerX * steerX + steerY * steerY);
          if (steerMag > maxForce) {
            steerX = (steerX / steerMag) * maxForce;
            steerY = (steerY / steerMag) * maxForce;
          }
          
          return new float[]{steerX, steerY};
        }
      }
      
      return new float[]{0, 0};
    }
    
    float[] cohesionWithNearby(List<Boid> nearbyBoids) {
      float neighborDist = neighborRadius.getValuef();
      float sumX = 0, sumY = 0;
      int count = 0;
      
      for (Boid other : nearbyBoids) {
        if (other == this) continue;
        
        float dx = x - other.x;
        float dy = y - other.y;
        
        if (Math.abs(dx) > getRingLength() / 2) {
          dx = dx > 0 ? dx - getRingLength() : dx + getRingLength();
        }
        
        float distance = (float)Math.sqrt(dx * dx + dy * dy);
        if (distance > 0 && distance < neighborDist) {
          sumX += other.x;
          sumY += other.y;
          count++;
        }
      }
      
      if (count > 0) {
        sumX /= count;
        sumY /= count;
        
        return seek(sumX, sumY);
      }
      
      return new float[]{0, 0};
    }
    
    
    float[] seek(float targetX, float targetY) {
      float dx = targetX - x;
      float dy = targetY - y;
      
      // Wrap distance for X
      if (Math.abs(dx) > getRingLength() / 2) {
        dx = dx > 0 ? dx - getRingLength() : dx + getRingLength();
      }
      
      float distance = (float)Math.sqrt(dx * dx + dy * dy);
      if (distance > 0) {
        dx = (dx / distance) * maxSpeed;
        dy = (dy / distance) * maxSpeed;
        
        float steerX = dx - velocityX;
        float steerY = dy - velocityY;
        
        float steerMag = (float)Math.sqrt(steerX * steerX + steerY * steerY);
        if (steerMag > maxForce) {
          steerX = (steerX / steerMag) * maxForce;
          steerY = (steerY / steerMag) * maxForce;
        }
        
        return new float[]{steerX, steerY};
      }
      
      return new float[]{0, 0};
    }
  }

  // Spatial grid for performance optimization
  private class SpatialGrid {
    private final float cellSize;
    private final int gridWidth;
    private final int gridHeight;
    private final Map<Long, List<Boid>> grid;
    
    public SpatialGrid(float cellSize) {
      this.cellSize = cellSize;
      this.gridWidth = (int) Math.ceil(getRingLength() / cellSize);
      this.gridHeight = (int) Math.ceil(getRingHeight() / cellSize);
      this.grid = new HashMap<>();
    }
    
    public void clear() {
      for (List<Boid> cell : grid.values()) {
        cell.clear();
      }
    }
    
    public void addBoid(Boid boid) {
      long key = getCellKey(boid.x, boid.y);
      List<Boid> cell = grid.computeIfAbsent(key, k -> new ArrayList<>());
      cell.add(boid);
    }
    
    public List<Boid> getNearbyBoids(float x, float y, float radius) {
      List<Boid> nearbyBoids = new ArrayList<>();
      
      int minGridX = (int) Math.floor((x - radius) / cellSize);
      int maxGridX = (int) Math.ceil((x + radius) / cellSize);
      int minGridY = Math.max(0, (int) Math.floor((y - radius) / cellSize));
      int maxGridY = Math.min(gridHeight - 1, (int) Math.ceil((y + radius) / cellSize));
      
      for (int gx = minGridX; gx <= maxGridX; gx++) {
        for (int gy = minGridY; gy <= maxGridY; gy++) {
          // Handle X-axis wrapping for ring coordinates
          int wrappedGx = ((gx % gridWidth) + gridWidth) % gridWidth;
          long key = getKey(wrappedGx, gy);
          
          List<Boid> cell = grid.get(key);
          if (cell != null) {
            nearbyBoids.addAll(cell);
          }
        }
      }
      
      return nearbyBoids;
    }
    
    private long getCellKey(float x, float y) {
      int gx = ((int) Math.floor(x / cellSize) % gridWidth + gridWidth) % gridWidth;
      int gy = Math.max(0, Math.min(gridHeight - 1, (int) Math.floor(y / cellSize)));
      return getKey(gx, gy);
    }
    
    private long getKey(int gx, int gy) {
      return ((long) gx << 32) | (gy & 0xffffffffL);
    }
  }

  // Boid management
  private List<Boid> boids = new ArrayList<>();
  private double currentTime = 0;
  private SpatialGrid spatialGrid;

  public Boids(LX lx) {
    super(lx);
    addParameter("boidCount", this.boidCount);
    addParameter("speed", this.speed);
    addParameter("separation", this.separation);
    addParameter("alignment", this.alignment);
    addParameter("cohesion", this.cohesion);
    addParameter("neighborRadius", this.neighborRadius);
    addParameter("turbulence", this.turbulence);
    addParameter("shape", this.shape);
    addParameter("boidRadius", this.boidRadius);
    
    // Initialize spatial grid with cell size based on neighbor radius
    // Use the initial neighborRadius value to set up grid
    initializeSpatialGrid();
    updateBoidCount();
  }
  
  private void initializeSpatialGrid() {
    // Use smaller cell size to ensure better spatial resolution for leader detection
    float cellSize = Math.max(4.0f, neighborRadius.getValuef() * 0.75f);
    spatialGrid = new SpatialGrid(cellSize);
  }
  
  private void updateBoidCount() {
    int targetCount = boidCount.getValuei();
    while (boids.size() < targetCount) {
      boids.add(new Boid());
    }
    while (boids.size() > targetCount) {
      boids.remove(boids.size() - 1);
    }
  }
  
  
  @Override
  public void onParameterChanged(heronarts.lx.parameter.LXParameter p) {
    super.onParameterChanged(p);
    if (p == boidCount) {
      updateBoidCount();
    } else if (p == neighborRadius) {
      // Reinitialize spatial grid when neighbor radius changes
      initializeSpatialGrid();
    } else if (p == shape) {
      // Regenerate boids when switching shapes for immediate visual feedback
      for (Boid boid : boids) {
        boid.x = boid.x * getRingLength() / (shape.getValuei() == 0 ? Apotheneum.Cylinder.Ring.LENGTH : Apotheneum.Cube.Ring.LENGTH);
        boid.y = boid.y * getRingHeight() / (shape.getValuei() == 0 ? Apotheneum.CYLINDER_HEIGHT : Apotheneum.GRID_HEIGHT);
      }
      // Also reinitialize grid for new dimensions
      initializeSpatialGrid();
    }
  }

  @Override
  protected void render(double deltaMs) {
    setApotheneumColor(LXColor.BLACK);
    currentTime += deltaMs;
    
    // Clear and populate spatial grid with current boid positions
    spatialGrid.clear();
    for (Boid boid : boids) {
      spatialGrid.addBoid(boid);
    }
    
    // Update all boids using spatial grid for neighbor finding
    for (Boid boid : boids) {
      boid.updateWithSpatialGrid(deltaMs, spatialGrid);
    }
    
    // Render all boids
    for (Boid boid : boids) {
      renderBoid(boid);
    }
  }
  
  private void renderBoid(Boid boid) {
    // All boids render as white for pure emergent behavior
    renderBoidWithRadius(boid.x, boid.y, LXColor.WHITE);
  }
  
  private void renderBoidWithRadius(float centerX, float centerY, int color) {
    float radius = boidRadius.getValuef();
    
    if (radius <= 0.5f) {
      // Single pixel
      setPixelOnShape(centerX, centerY, color);
    } else {
      // Multiple pixels in circular pattern
      int radiusInt = (int) Math.ceil(radius);
      
      // Render center pixel
      setPixelOnShape(centerX, centerY, color);
      
      // Render surrounding pixels in circular pattern
      for (int dx = -radiusInt; dx <= radiusInt; dx++) {
        for (int dy = -radiusInt; dy <= radiusInt; dy++) {
          if (dx == 0 && dy == 0) continue;
          
          float distance = (float) Math.sqrt(dx * dx + dy * dy);
          if (distance <= radius) {
            setPixelOnShape(centerX + dx, centerY + dy, color);
          }
        }
      }
    }
  }
  
  private void setPixelOnShape(float ringX, float ringY, int color) {
    int ringIndex = (int) Math.round(ringY);
    int pointIndex = (int) Math.round(ringX);
    
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
      int wrappedIndex = ((pointIndex % ring.points.length) + ring.points.length) % ring.points.length;
      colors[ring.points[wrappedIndex].index] = color;
    }
  }
  
  private boolean isInDoorArea(float ringX, float ringY) {
    int ringIndex = (int) Math.round(ringY);
    int ringHeight = getRingHeight();
    int ringLength = getRingLength();
    
    if (ringIndex < 0 || ringIndex >= ringHeight) {
      return false;
    }
    
    // Check if at bottom where doors are
    if (ringIndex >= ringHeight - Apotheneum.DOOR_HEIGHT) {
      int ringPos = (int) Math.round(ringX);
      int wrappedPos = ((ringPos % ringLength) + ringLength) % ringLength;
      
      if (shape.getValuei() == 0) {
        // Cube door detection
        for (int face = 0; face < 4; face++) {
          int faceStart = face * Apotheneum.GRID_WIDTH;
          int doorStart = faceStart + Apotheneum.Cube.DOOR_START_COLUMN;
          int doorEnd = doorStart + Apotheneum.DOOR_WIDTH - 1;
          
          if (wrappedPos >= doorStart && wrappedPos <= doorEnd) {
            return true;
          }
        }
      } else {
        // Cylinder door detection
        int doorStart = Apotheneum.Cylinder.DOOR_START_COLUMN;
        int doorEnd = doorStart + Apotheneum.DOOR_WIDTH - 1;
        int posInCycle = wrappedPos % 30;
        if (posInCycle >= doorStart && posInCycle <= doorEnd) {
          return true;
        }
      }
    }
    
    return false;
  }

  @Override
  public void buildDeviceControls(UI ui, UIDevice uiDevice, Boids boids) {
    uiDevice.setLayout(UIDevice.Layout.HORIZONTAL);
    
    addColumn(uiDevice, "Flock",
      newIntegerBox(boids.boidCount),
      newKnob(boids.speed),
      newKnob(boids.turbulence)
    );
    
    addVerticalBreak(ui, uiDevice);
    
    addColumn(uiDevice, "Forces",
      newKnob(boids.separation),
      newKnob(boids.alignment),
      newKnob(boids.cohesion)
    );
    
    addVerticalBreak(ui, uiDevice);
    
    addColumn(uiDevice, "Behavior",
      newKnob(boids.neighborRadius),
      newKnob(boids.boidRadius),
      newDropMenu(boids.shape)
    );
    
    addVerticalBreak(ui, uiDevice);
    
  }
}