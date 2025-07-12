package apotheneum.doved.lightning;

import heronarts.lx.utils.LXUtils;
import java.util.ArrayList;
import java.util.List;

public class RRT3DAlgorithm {
  
  public static class Parameters {
    public final double stepSize;
    public final double goalBias;
    public final int maxIterations;
    public final double branchProbability;
    public final double jaggedness;
    public final double goalRadius;
    public final double electricalField;
    public final double minX, maxX, minY, maxY, minZ, maxZ;
    
    public Parameters(double stepSize, double goalBias, int maxIterations, 
                     double branchProbability, double jaggedness, double goalRadius,
                     double electricalField, double minX, double maxX, 
                     double minY, double maxY, double minZ, double maxZ) {
      this.stepSize = stepSize;
      this.goalBias = goalBias;
      this.maxIterations = maxIterations;
      this.branchProbability = branchProbability;
      this.jaggedness = jaggedness;
      this.goalRadius = goalRadius;
      this.electricalField = electricalField;
      this.minX = minX;
      this.maxX = maxX;
      this.minY = minY;
      this.maxY = maxY;
      this.minZ = minZ;
      this.maxZ = maxZ;
    }
  }
  
  public static class LightningSegment3D {
    public final double x1, y1, z1, x2, y2, z2;
    public final boolean isBranch;
    public final double intensity;
    public final int depth;
    
    public LightningSegment3D(double x1, double y1, double z1, double x2, double y2, double z2, 
                             boolean isBranch, double intensity, int depth) {
      this.x1 = x1;
      this.y1 = y1;
      this.z1 = z1;
      this.x2 = x2;
      this.y2 = y2;
      this.z2 = z2;
      this.isBranch = isBranch;
      this.intensity = intensity;
      this.depth = depth;
    }
  }
  
  private static class RRTNode3D {
    public final double x, y, z;
    public final RRTNode3D parent;
    public final double intensity;
    public final int depth;
    public final boolean isBranch;
    public final List<RRTNode3D> children;
    
    public RRTNode3D(double x, double y, double z, RRTNode3D parent, double intensity, int depth, boolean isBranch) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.parent = parent;
      this.intensity = intensity;
      this.depth = depth;
      this.isBranch = isBranch;
      this.children = new ArrayList<>();
    }
    
    public double distanceTo(double px, double py, double pz) {
      double dx = x - px;
      double dy = y - py;
      double dz = z - pz;
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
  }
  
  private static class GoalRegion3D {
    public final double centerX, centerY, centerZ;
    public final double radius;
    
    public GoalRegion3D(double centerX, double centerY, double centerZ, double radius) {
      this.centerX = centerX;
      this.centerY = centerY;
      this.centerZ = centerZ;
      this.radius = radius;
    }
    
    public boolean contains(double x, double y, double z) {
      double dx = x - centerX;
      double dy = y - centerY;
      double dz = z - centerZ;
      return Math.sqrt(dx * dx + dy * dy + dz * dz) <= radius;
    }
    
    public double[] samplePoint() {
      // Sample point in sphere using rejection sampling
      double x, y, z;
      do {
        x = (Math.random() - 0.5) * 2;
        y = (Math.random() - 0.5) * 2;
        z = (Math.random() - 0.5) * 2;
      } while (x * x + y * y + z * z > 1);
      
      return new double[] {
        centerX + x * radius,
        centerY + y * radius,
        centerZ + z * radius
      };
    }
  }
  
  public static void generateLightning3D(List<LightningSegment3D> segments, Parameters params) {
    // Initialize tree with root at top center
    double startX = (params.minX + params.maxX) / 2.0;
    double startY = params.maxY; // Start at top
    double startZ = (params.minZ + params.maxZ) / 2.0;
    RRTNode3D root = new RRTNode3D(startX, startY, startZ, null, 1.0, 0, false);
    List<RRTNode3D> tree = new ArrayList<>();
    tree.add(root);
    
    // Define goal region at bottom
    double goalCenterX = (params.minX + params.maxX) / 2.0;
    double goalCenterY = params.minY; // Goal at bottom
    double goalCenterZ = (params.minZ + params.maxZ) / 2.0;
    GoalRegion3D goalRegion = new GoalRegion3D(goalCenterX, goalCenterY, goalCenterZ, params.goalRadius);
    
    // RRT main loop
    boolean goalReached = false;
    for (int iteration = 0; iteration < params.maxIterations && !goalReached; iteration++) {
      // Sample point
      double[] samplePoint = sampleRandomPoint3D(params, goalRegion);
      double sampleX = samplePoint[0];
      double sampleY = samplePoint[1];
      double sampleZ = samplePoint[2];
      
      // Find nearest node
      RRTNode3D nearestNode = findNearestNode3D(tree, sampleX, sampleY, sampleZ);
      
      // Extend tree towards sample
      RRTNode3D newNode = extendTree3D(nearestNode, sampleX, sampleY, sampleZ, params);
      
      if (newNode != null && isValidNode3D(newNode, params)) {
        tree.add(newNode);
        nearestNode.children.add(newNode);
        
        // Check if goal reached
        if (goalRegion.contains(newNode.x, newNode.y, newNode.z)) {
          goalReached = true;
        }
        
        // Create additional branches with some probability
        if (Math.random() < params.branchProbability && newNode.depth < 3) {
          createBranches3D(tree, newNode, params);
        }
      }
    }
    
    // Convert tree to lightning segments
    convertTreeToSegments3D(tree, segments);
  }
  
  private static double[] sampleRandomPoint3D(Parameters params, GoalRegion3D goalRegion) {
    // With goal bias probability, sample from goal region
    if (Math.random() < params.goalBias) {
      return goalRegion.samplePoint();
    }
    
    // Otherwise sample uniformly from 3D space with electrical field bias
    double x = params.minX + Math.random() * (params.maxX - params.minX);
    double y = params.minY + Math.random() * (params.maxY - params.minY);
    double z = params.minZ + Math.random() * (params.maxZ - params.minZ);
    
    // Apply electrical field bias towards center and downward
    if (params.electricalField > 0) {
      double centerX = (params.minX + params.maxX) / 2.0;
      double centerZ = (params.minZ + params.maxZ) / 2.0;
      double centerBiasX = 1.0 - Math.abs(x - centerX) / ((params.maxX - params.minX) / 2.0);
      double centerBiasZ = 1.0 - Math.abs(z - centerZ) / ((params.maxZ - params.minZ) / 2.0);
      double verticalBias = 1.0 - (y - params.minY) / (params.maxY - params.minY);
      
      // Bias towards center-bottom region
      double totalBias = (centerBiasX + centerBiasZ + verticalBias) / 3.0 * params.electricalField;
      if (Math.random() > totalBias) {
        // Re-sample with bias
        x = centerX + (Math.random() - 0.5) * (params.maxX - params.minX) * 0.6;
        z = centerZ + (Math.random() - 0.5) * (params.maxZ - params.minZ) * 0.6;
        y = y * 0.8 + params.minY * 0.2;
      }
    }
    
    return new double[] {
      LXUtils.constrain(x, params.minX, params.maxX),
      LXUtils.constrain(y, params.minY, params.maxY),
      LXUtils.constrain(z, params.minZ, params.maxZ)
    };
  }
  
  private static RRTNode3D findNearestNode3D(List<RRTNode3D> tree, double sampleX, double sampleY, double sampleZ) {
    RRTNode3D nearest = tree.get(0);
    double minDistance = nearest.distanceTo(sampleX, sampleY, sampleZ);
    
    for (RRTNode3D node : tree) {
      double distance = node.distanceTo(sampleX, sampleY, sampleZ);
      if (distance < minDistance) {
        minDistance = distance;
        nearest = node;
      }
    }
    
    return nearest;
  }
  
  private static RRTNode3D extendTree3D(RRTNode3D nearestNode, double sampleX, double sampleY, double sampleZ, Parameters params) {
    // Calculate direction from nearest to sample
    double dx = sampleX - nearestNode.x;
    double dy = sampleY - nearestNode.y;
    double dz = sampleZ - nearestNode.z;
    double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
    
    if (distance < 1e-6) {
      return null; // Too close to existing node
    }
    
    // Normalize direction
    dx /= distance;
    dy /= distance;
    dz /= distance;
    
    // Move step size in that direction
    double newX = nearestNode.x + dx * params.stepSize;
    double newY = nearestNode.y + dy * params.stepSize;
    double newZ = nearestNode.z + dz * params.stepSize;
    
    // Add jaggedness - random displacement
    if (params.jaggedness > 0) {
      // Create two perpendicular vectors to the direction
      double perpX1, perpY1, perpZ1;
      double perpX2, perpY2, perpZ2;
      
      // Find a vector perpendicular to (dx, dy, dz)
      if (Math.abs(dx) < 0.9) {
        perpX1 = 0;
        perpY1 = -dz;
        perpZ1 = dy;
      } else {
        perpX1 = -dz;
        perpY1 = 0;
        perpZ1 = dx;
      }
      
      // Normalize
      double len1 = Math.sqrt(perpX1 * perpX1 + perpY1 * perpY1 + perpZ1 * perpZ1);
      perpX1 /= len1;
      perpY1 /= len1;
      perpZ1 /= len1;
      
      // Second perpendicular vector (cross product)
      perpX2 = dy * perpZ1 - dz * perpY1;
      perpY2 = dz * perpX1 - dx * perpZ1;
      perpZ2 = dx * perpY1 - dy * perpX1;
      
      // Add random displacement
      double displacement1 = (Math.random() - 0.5) * params.jaggedness * params.stepSize;
      double displacement2 = (Math.random() - 0.5) * params.jaggedness * params.stepSize;
      
      newX += perpX1 * displacement1 + perpX2 * displacement2;
      newY += perpY1 * displacement1 + perpY2 * displacement2;
      newZ += perpZ1 * displacement1 + perpZ2 * displacement2;
    }
    
    // Calculate intensity based on depth and electrical field
    double intensity = Math.max(0.7, 1.0 - nearestNode.depth * 0.05);
    
    // Apply electrical field influence to intensity
    if (params.electricalField > 0) {
      double centerX = (params.minX + params.maxX) / 2.0;
      double centerZ = (params.minZ + params.maxZ) / 2.0;
      double centerDistanceX = Math.abs(newX - centerX) / ((params.maxX - params.minX) / 2.0);
      double centerDistanceZ = Math.abs(newZ - centerZ) / ((params.maxZ - params.minZ) / 2.0);
      double fieldInfluence = 1.0 - (centerDistanceX + centerDistanceZ) / 2.0 * 0.3;
      intensity *= fieldInfluence;
    }
    
    return new RRTNode3D(newX, newY, newZ, nearestNode, intensity, nearestNode.depth + 1, false);
  }
  
  private static boolean isValidNode3D(RRTNode3D node, Parameters params) {
    return node.x >= params.minX && node.x <= params.maxX && 
           node.y >= params.minY && node.y <= params.maxY &&
           node.z >= params.minZ && node.z <= params.maxZ;
  }
  
  private static void createBranches3D(List<RRTNode3D> tree, RRTNode3D parentNode, Parameters params) {
    // Create 1-2 branches from this node
    int numBranches = Math.random() < 0.5 ? 1 : 2;
    
    for (int i = 0; i < numBranches; i++) {
      // Create branch in a random direction, biased away from parent
      double theta = Math.random() * 2 * Math.PI;
      double phi = Math.random() * Math.PI;
      
      double branchDx = Math.sin(phi) * Math.cos(theta);
      double branchDy = Math.sin(phi) * Math.sin(theta);
      double branchDz = Math.cos(phi);
      
      // If there's a parent, bias away from it
      if (parentNode.parent != null) {
        double parentDx = parentNode.x - parentNode.parent.x;
        double parentDy = parentNode.y - parentNode.parent.y;
        double parentDz = parentNode.z - parentNode.parent.z;
        double parentLen = Math.sqrt(parentDx * parentDx + parentDy * parentDy + parentDz * parentDz);
        
        if (parentLen > 0) {
          parentDx /= parentLen;
          parentDy /= parentLen;
          parentDz /= parentLen;
          
          // Bias branch direction away from parent direction
          double dot = branchDx * parentDx + branchDy * parentDy + branchDz * parentDz;
          if (dot > 0) {
            branchDx -= parentDx * dot * 0.5;
            branchDy -= parentDy * dot * 0.5;
            branchDz -= parentDz * dot * 0.5;
            
            // Renormalize
            double len = Math.sqrt(branchDx * branchDx + branchDy * branchDy + branchDz * branchDz);
            if (len > 0) {
              branchDx /= len;
              branchDy /= len;
              branchDz /= len;
            }
          }
        }
      }
      
      double branchLength = params.stepSize * (0.5 + Math.random() * 0.5);
      double branchX = parentNode.x + branchDx * branchLength;
      double branchY = parentNode.y + branchDy * branchLength;
      double branchZ = parentNode.z + branchDz * branchLength;
      
      // Add some jaggedness to branch
      if (params.jaggedness > 0) {
        branchX += (Math.random() - 0.5) * params.jaggedness * branchLength;
        branchY += (Math.random() - 0.5) * params.jaggedness * branchLength;
        branchZ += (Math.random() - 0.5) * params.jaggedness * branchLength;
      }
      
      double branchIntensity = parentNode.intensity * (0.8 + Math.random() * 0.2);
      RRTNode3D branchNode = new RRTNode3D(branchX, branchY, branchZ, parentNode, branchIntensity, 
                                          parentNode.depth + 1, true);
      
      if (isValidNode3D(branchNode, params)) {
        tree.add(branchNode);
        parentNode.children.add(branchNode);
      }
    }
  }
  
  private static void convertTreeToSegments3D(List<RRTNode3D> tree, List<LightningSegment3D> segments) {
    for (RRTNode3D node : tree) {
      if (node.parent != null) {
        segments.add(new LightningSegment3D(
          node.parent.x, node.parent.y, node.parent.z,
          node.x, node.y, node.z,
          node.isBranch,
          node.intensity,
          node.depth
        ));
      }
    }
  }
}