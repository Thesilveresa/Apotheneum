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
import java.util.List;

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

  // Parameters - optimized for tight flocking behavior
  public final CompoundDiscreteParameter boidCount =
    new CompoundDiscreteParameter("Count", 25, 5, 80)
    .setDescription("Number of boids in the flock");

  public final CompoundParameter speed =
    new CompoundParameter("Speed", 8.0, 1.0, 20.0)
    .setDescription("Base movement speed");

  public final CompoundParameter separation =
    new CompoundParameter("Separation", 2.5, 0, 4)
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

  public final CompoundDiscreteParameter leaderCount =
    new CompoundDiscreteParameter("Leaders", 4, 0, 8)
    .setDescription("Number of leader boids that others follow");

  public final CompoundParameter leaderForce =
    new CompoundParameter("Follow", 2.5, 0, 4)
    .setDescription("Strength of attraction to leader boids");

  public final DiscreteParameter shape = 
    new DiscreteParameter("Shape", new String[]{"Cube", "Cylinder"}, 0)
    .setDescription("Which shape to render on");

  // Simple 2D Boid class
  private class Boid {
    float x, y;              // Position in ring coordinates
    float velocityX, velocityY;  // Velocity
    float maxSpeed = 20.0f;
    float maxForce = 0.8f;
    boolean isLeader = false;
    
    // Leader wandering
    float wanderTargetX, wanderTargetY;
    double lastWanderUpdate = 0;
    
    Boid() {
      // Start boids in smaller, clustered areas for better flocking
      int clusterCount = Math.max(1, leaderCount.getValuei());
      int clusterIndex = (int)(Math.random() * clusterCount);
      
      // Create cluster centers
      float clusterSpacing = getRingLength() / (float)clusterCount;
      float clusterCenterX = (clusterIndex * clusterSpacing + clusterSpacing * 0.5f) % getRingLength();
      float clusterCenterY = getRingHeight() * 0.6f; // Start in middle-upper area
      
      // Spawn within very small radius of cluster center for tight flocking
      float clusterRadius = Math.min(6, getRingLength() / (clusterCount * 6));
      float angle = (float)(Math.random() * 2 * Math.PI);
      float distance = LXUtils.randomf(0.5f, clusterRadius);
      
      x = (clusterCenterX + (float)Math.cos(angle) * distance + getRingLength()) % getRingLength();
      y = Math.max(8, Math.min(getRingHeight() - 8, 
          clusterCenterY + (float)Math.sin(angle) * distance * 0.3f));
      
      // Smaller initial velocity for tighter flocking
      float initAngle = (float)(Math.random() * 2 * Math.PI);
      velocityX = (float)Math.cos(initAngle) * LXUtils.randomf(0.2f, 0.8f);
      velocityY = (float)Math.sin(initAngle) * LXUtils.randomf(0.2f, 0.8f);
      
      // Set initial wander target for leaders
      setNewWanderTarget();
    }
    
    void update(double deltaMs, List<Boid> allBoids) {
      float accelerationX = 0;
      float accelerationY = 0;
      
      if (isLeader) {
        // Leaders wander independently but very gently to keep flock together
        float[] wander = wander(deltaMs);
        accelerationX += wander[0] * 0.1f;
        accelerationY += wander[1] * 0.1f;
        
        // Leaders still separate from others but maintain good spacing
        float[] sep = separate(allBoids);
        accelerationX += sep[0] * separation.getValuef() * 1.0f;
        accelerationY += sep[1] * separation.getValuef() * 1.0f;
      } else {
        // Regular boids flock
        float[] sep = separate(allBoids);
        float[] ali = align(allBoids);
        float[] coh = cohesion(allBoids);
        float[] lead = followLeaders(allBoids);
        
        accelerationX += sep[0] * separation.getValuef() * 1.2f;
        accelerationY += sep[1] * separation.getValuef() * 1.2f;
        accelerationX += ali[0] * alignment.getValuef() * 1.0f;
        accelerationY += ali[1] * alignment.getValuef() * 1.0f;
        accelerationX += coh[0] * cohesion.getValuef() * 0.8f;
        accelerationY += coh[1] * cohesion.getValuef() * 0.8f;
        accelerationX += lead[0] * leaderForce.getValuef() * 1.0f;
        accelerationY += lead[1] * leaderForce.getValuef() * 1.0f;
      }
      
      // Add turbulence
      if (turbulence.getValuef() > 0) {
        accelerationX += (Math.random() - 0.5) * turbulence.getValuef() * 1.5f;
        accelerationY += (Math.random() - 0.5) * turbulence.getValuef() * 1.5f;
      }
      
      // Update velocity
      velocityX += accelerationX;
      velocityY += accelerationY;
      
      // Limit speed
      float speed = (float)Math.sqrt(velocityX * velocityX + velocityY * velocityY);
      float maxSpeedScaled = maxSpeed * Boids.this.speed.getValuef();
      if (speed > maxSpeedScaled) {
        velocityX = (velocityX / speed) * maxSpeedScaled;
        velocityY = (velocityY / speed) * maxSpeedScaled;
      }
      
      // Update position
      float deltaSeconds = (float)(deltaMs * 0.001);
      float speedMultiplier = Boids.this.speed.getValuef() / 8.0f; // Scale relative to default
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
      
      // Avoid door areas
      if (isInDoorArea(x, y)) {
        velocityY = Math.abs(velocityY) * 1.5f; // Move up more gently
      }
    }
    
    void setNewWanderTarget() {
      // Leaders wander in very small areas to keep flocks together
      float wanderRadius = 5; // Very small wander area to keep flock cohesive
      float angle = (float)(Math.random() * 2 * Math.PI);
      wanderTargetX = x + (float)Math.cos(angle) * LXUtils.randomf(1, wanderRadius);
      wanderTargetY = y + (float)Math.sin(angle) * LXUtils.randomf(0.5f, wanderRadius * 0.4f);
      
      // Keep within bounds
      wanderTargetX = (wanderTargetX + getRingLength()) % getRingLength();
      wanderTargetY = Math.max(8, Math.min(getRingHeight() - 8, wanderTargetY));
    }
    
    float[] wander(double currentTime) {
      // Update wander target less frequently to maintain direction longer
      if (currentTime - lastWanderUpdate > 5000) { // Every 5 seconds - slower changes
        setNewWanderTarget();
        lastWanderUpdate = currentTime;
      }
      
      // Move toward wander target
      float dx = wanderTargetX - x;
      float dy = wanderTargetY - y;
      
      // Wrap distance calculation for X
      if (Math.abs(dx) > getRingLength() / 2) {
        dx = dx > 0 ? dx - getRingLength() : dx + getRingLength();
      }
      
      float distance = (float)Math.sqrt(dx * dx + dy * dy);
      if (distance > 0) {
        // Very gentle wander force to keep leaders near followers
        dx = (dx / distance) * maxForce * 0.05f;
        dy = (dy / distance) * maxForce * 0.05f;
      }
      
      return new float[]{dx, dy};
    }
    
    float[] separate(List<Boid> boids) {
      float desiredSeparation = 4.0f; // Increased separation for better spacing
      float steerX = 0, steerY = 0;
      int count = 0;
      
      for (Boid other : boids) {
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
    
    float[] align(List<Boid> boids) {
      float neighborDist = neighborRadius.getValuef();
      float sumX = 0, sumY = 0;
      int count = 0;
      
      for (Boid other : boids) {
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
    
    float[] cohesion(List<Boid> boids) {
      float neighborDist = neighborRadius.getValuef();
      float sumX = 0, sumY = 0;
      int count = 0;
      
      for (Boid other : boids) {
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
    
    float[] followLeaders(List<Boid> boids) {
      float steerX = 0, steerY = 0;
      int count = 0;
      
      for (Boid other : boids) {
        if (other.isLeader && other != this) {
          float dx = x - other.x;
          float dy = y - other.y;
          
          if (Math.abs(dx) > getRingLength() / 2) {
            dx = dx > 0 ? dx - getRingLength() : dx + getRingLength();
          }
          
          float distance = (float)Math.sqrt(dx * dx + dy * dy);
          // Followers follow leaders within reasonable range
          if (distance > 0 && distance < neighborRadius.getValuef() * 2) {
            float[] seekForce = seek(other.x, other.y);
            // Stronger influence for closer leaders
            float influence = Math.max(0.1f, 1.0f / (distance + 1)); // Linear falloff with minimum
            steerX += seekForce[0] * influence;
            steerY += seekForce[1] * influence;
            count++;
          }
        }
      }
      
      if (count > 0) {
        steerX /= count;
        steerY /= count;
      }
      
      return new float[]{steerX, steerY};
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

  // Boid management
  private List<Boid> boids = new ArrayList<>();
  private double currentTime = 0;

  public Boids(LX lx) {
    super(lx);
    addParameter("boidCount", this.boidCount);
    addParameter("speed", this.speed);
    addParameter("separation", this.separation);
    addParameter("alignment", this.alignment);
    addParameter("cohesion", this.cohesion);
    addParameter("neighborRadius", this.neighborRadius);
    addParameter("turbulence", this.turbulence);
    addParameter("leaderCount", this.leaderCount);
    addParameter("leaderForce", this.leaderForce);
    addParameter("shape", this.shape);
    
    updateBoidCount();
  }
  
  private void updateBoidCount() {
    int targetCount = boidCount.getValuei();
    while (boids.size() < targetCount) {
      boids.add(new Boid());
    }
    while (boids.size() > targetCount) {
      boids.remove(boids.size() - 1);
    }
    assignLeaders();
  }
  
  private void assignLeaders() {
    // Reset all to followers
    for (Boid boid : boids) {
      boid.isLeader = false;
    }
    
    // Assign leaders randomly
    int leaders = Math.min(leaderCount.getValuei(), boids.size());
    for (int i = 0; i < leaders; i++) {
      int index = LXUtils.randomi(0, boids.size() - 1);
      boids.get(index).isLeader = true;
    }
  }
  
  @Override
  public void onParameterChanged(heronarts.lx.parameter.LXParameter p) {
    super.onParameterChanged(p);
    if (p == boidCount) {
      updateBoidCount();
    } else if (p == leaderCount) {
      assignLeaders();
    } else if (p == shape) {
      // Regenerate boids when switching shapes for immediate visual feedback
      for (Boid boid : boids) {
        boid.x = boid.x * getRingLength() / (shape.getValuei() == 0 ? Apotheneum.Cylinder.Ring.LENGTH : Apotheneum.Cube.Ring.LENGTH);
        boid.y = boid.y * getRingHeight() / (shape.getValuei() == 0 ? Apotheneum.CYLINDER_HEIGHT : Apotheneum.GRID_HEIGHT);
      }
    }
  }

  @Override
  protected void render(double deltaMs) {
    setApotheneumColor(LXColor.BLACK);
    currentTime += deltaMs;
    
    // Update all boids
    for (Boid boid : boids) {
      boid.update(deltaMs, boids);
    }
    
    // Render all boids
    for (Boid boid : boids) {
      renderBoid(boid);
    }
  }
  
  private void renderBoid(Boid boid) {
    // Color leaders differently for visual feedback
    int color = boid.isLeader ? LXColor.hsb(60, 80, 100) : LXColor.WHITE; // Leaders are yellow
    setPixelOnShape(boid.x, boid.y, color);
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
    
    addColumn(uiDevice, "Leaders",
      newIntegerBox(boids.leaderCount),
      newKnob(boids.leaderForce),
      newKnob(boids.neighborRadius)
    );
    
    addVerticalBreak(ui, uiDevice);
    
    addColumn(uiDevice, "Shape",
      newDropMenu(boids.shape)
    );
  }
}