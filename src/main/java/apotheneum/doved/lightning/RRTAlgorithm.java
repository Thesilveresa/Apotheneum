package apotheneum.doved.lightning;

import heronarts.lx.utils.LXUtils;
import java.util.ArrayList;
import java.util.List;

public class RRTAlgorithm {
  
  public static class Parameters {
    public final double stepSize;
    public final double goalBias;
    public final int maxIterations;
    public final double branchProbability;
    public final double jaggedness;
    public final double goalRadius;
    public final double electricalField;
    public final int rasterWidth;
    public final int rasterHeight;
    
    public Parameters(double stepSize, double goalBias, int maxIterations, 
                     double branchProbability, double jaggedness, double goalRadius,
                     double electricalField, int rasterWidth, int rasterHeight) {
      this.stepSize = stepSize;
      this.goalBias = goalBias;
      this.maxIterations = maxIterations;
      this.branchProbability = branchProbability;
      this.jaggedness = jaggedness;
      this.goalRadius = goalRadius;
      this.electricalField = electricalField;
      this.rasterWidth = rasterWidth;
      this.rasterHeight = rasterHeight;
    }
  }
  
  public static class LightningSegment {
    public final double x1, y1, x2, y2;
    public final boolean isBranch;
    public final double intensity;
    public final int depth;
    
    public LightningSegment(double x1, double y1, double x2, double y2, 
                           boolean isBranch, double intensity, int depth) {
      this.x1 = x1;
      this.y1 = y1;
      this.x2 = x2;
      this.y2 = y2;
      this.isBranch = isBranch;
      this.intensity = intensity;
      this.depth = depth;
    }
  }
  
  private static class RRTNode {
    public final double x, y;
    public final RRTNode parent;
    public final double intensity;
    public final int depth;
    public final boolean isBranch;
    public final List<RRTNode> children;
    
    public RRTNode(double x, double y, RRTNode parent, double intensity, int depth, boolean isBranch) {
      this.x = x;
      this.y = y;
      this.parent = parent;
      this.intensity = intensity;
      this.depth = depth;
      this.isBranch = isBranch;
      this.children = new ArrayList<>();
    }
    
    public double distanceTo(double px, double py) {
      double dx = x - px;
      double dy = y - py;
      return Math.sqrt(dx * dx + dy * dy);
    }
  }
  
  private static class GoalRegion {
    public final double centerX, centerY;
    public final double radius;
    
    public GoalRegion(double centerX, double centerY, double radius) {
      this.centerX = centerX;
      this.centerY = centerY;
      this.radius = radius;
    }
    
    public boolean contains(double x, double y) {
      double dx = x - centerX;
      double dy = y - centerY;
      return Math.sqrt(dx * dx + dy * dy) <= radius;
    }
    
    public double[] samplePoint() {
      double angle = Math.random() * 2 * Math.PI;
      double distance = Math.random() * radius;
      return new double[] {
        centerX + Math.cos(angle) * distance,
        centerY + Math.sin(angle) * distance
      };
    }
  }
  
  public static void generateLightning(List<LightningSegment> segments, Parameters params) {
    // Initialize tree with root at top center
    double startX = params.rasterWidth / 2.0;
    double startY = 0;
    RRTNode root = new RRTNode(startX, startY, null, 1.0, 0, false);
    List<RRTNode> tree = new ArrayList<>();
    tree.add(root);
    
    // Define goal region at bottom
    double goalCenterX = params.rasterWidth / 2.0;
    double goalCenterY = params.rasterHeight - 1;
    GoalRegion goalRegion = new GoalRegion(goalCenterX, goalCenterY, params.goalRadius);
    
    // RRT main loop
    boolean goalReached = false;
    for (int iteration = 0; iteration < params.maxIterations && !goalReached; iteration++) {
      // Sample point
      double[] samplePoint = sampleRandomPoint(params, goalRegion);
      double sampleX = samplePoint[0];
      double sampleY = samplePoint[1];
      
      // Find nearest node
      RRTNode nearestNode = findNearestNode(tree, sampleX, sampleY);
      
      // Extend tree towards sample
      RRTNode newNode = extendTree(nearestNode, sampleX, sampleY, params);
      
      if (newNode != null && isValidNode(newNode, params)) {
        tree.add(newNode);
        nearestNode.children.add(newNode);
        
        // Check if goal reached
        if (goalRegion.contains(newNode.x, newNode.y)) {
          goalReached = true;
        }
        
        // Create additional branches with some probability
        if (Math.random() < params.branchProbability && newNode.depth < 3) {
          createBranches(tree, newNode, params);
        }
      }
    }
    
    // Convert tree to lightning segments
    convertTreeToSegments(tree, segments);
  }
  
  private static double[] sampleRandomPoint(Parameters params, GoalRegion goalRegion) {
    // With goal bias probability, sample from goal region
    if (Math.random() < params.goalBias) {
      return goalRegion.samplePoint();
    }
    
    // Otherwise sample uniformly from space with electrical field bias
    double x = Math.random() * params.rasterWidth;
    double y = Math.random() * params.rasterHeight;
    
    // Apply electrical field bias towards center and downward
    if (params.electricalField > 0) {
      double centerX = params.rasterWidth / 2.0;
      double centerBias = (centerX - Math.abs(x - centerX)) / centerX;
      double verticalBias = y / params.rasterHeight;
      
      // Bias towards center-bottom region
      double totalBias = (centerBias + verticalBias) * params.electricalField;
      if (Math.random() > totalBias) {
        // Re-sample with bias
        x = centerX + (Math.random() - 0.5) * params.rasterWidth * 0.6;
        y = y * 0.8 + params.rasterHeight * 0.2;
      }
    }
    
    return new double[] {
      LXUtils.constrain(x, 0, params.rasterWidth - 1),
      LXUtils.constrain(y, 0, params.rasterHeight - 1)
    };
  }
  
  private static RRTNode findNearestNode(List<RRTNode> tree, double sampleX, double sampleY) {
    RRTNode nearest = tree.get(0);
    double minDistance = nearest.distanceTo(sampleX, sampleY);
    
    for (RRTNode node : tree) {
      double distance = node.distanceTo(sampleX, sampleY);
      if (distance < minDistance) {
        minDistance = distance;
        nearest = node;
      }
    }
    
    return nearest;
  }
  
  private static RRTNode extendTree(RRTNode nearestNode, double sampleX, double sampleY, Parameters params) {
    // Calculate direction from nearest to sample
    double dx = sampleX - nearestNode.x;
    double dy = sampleY - nearestNode.y;
    double distance = Math.sqrt(dx * dx + dy * dy);
    
    if (distance < 1e-6) {
      return null; // Too close to existing node
    }
    
    // Normalize direction
    dx /= distance;
    dy /= distance;
    
    // Move step size in that direction
    double newX = nearestNode.x + dx * params.stepSize;
    double newY = nearestNode.y + dy * params.stepSize;
    
    // Add jaggedness - random perpendicular displacement
    if (params.jaggedness > 0) {
      double perpX = -dy;
      double perpY = dx;
      double displacement = (Math.random() - 0.5) * params.jaggedness * params.stepSize;
      newX += perpX * displacement;
      newY += perpY * displacement;
    }
    
    // Calculate intensity based on depth and electrical field
    double intensity = Math.max(0.7, 1.0 - nearestNode.depth * 0.05);
    
    // Apply electrical field influence to intensity
    if (params.electricalField > 0) {
      double centerX = params.rasterWidth / 2.0;
      double centerDistance = Math.abs(newX - centerX) / centerX;
      double fieldInfluence = 1.0 - centerDistance * 0.3;
      intensity *= fieldInfluence;
    }
    
    return new RRTNode(newX, newY, nearestNode, intensity, nearestNode.depth + 1, false);
  }
  
  private static boolean isValidNode(RRTNode node, Parameters params) {
    return node.x >= 0 && node.x < params.rasterWidth && 
           node.y >= 0 && node.y < params.rasterHeight;
  }
  
  private static void createBranches(List<RRTNode> tree, RRTNode parentNode, Parameters params) {
    // Create 1-2 branches from this node
    int numBranches = Math.random() < 0.5 ? 1 : 2;
    
    for (int i = 0; i < numBranches; i++) {
      // Create branch in a random direction, biased away from parent
      double branchAngle = Math.random() * 2 * Math.PI;
      
      // If there's a parent, bias away from it
      if (parentNode.parent != null) {
        double parentAngle = Math.atan2(parentNode.y - parentNode.parent.y, 
                                      parentNode.x - parentNode.parent.x);
        double deviation = (Math.random() - 0.5) * Math.PI * 0.8;
        branchAngle = parentAngle + Math.PI/2 + deviation;
      }
      
      double branchLength = params.stepSize * (0.5 + Math.random() * 0.5);
      double branchX = parentNode.x + Math.cos(branchAngle) * branchLength;
      double branchY = parentNode.y + Math.sin(branchAngle) * branchLength;
      
      // Add some jaggedness to branch
      if (params.jaggedness > 0) {
        double jag = (Math.random() - 0.5) * params.jaggedness * branchLength;
        branchX += Math.cos(branchAngle + Math.PI/2) * jag;
        branchY += Math.sin(branchAngle + Math.PI/2) * jag;
      }
      
      double branchIntensity = parentNode.intensity * (0.8 + Math.random() * 0.2);
      RRTNode branchNode = new RRTNode(branchX, branchY, parentNode, branchIntensity, 
                                      parentNode.depth + 1, true);
      
      if (isValidNode(branchNode, params)) {
        tree.add(branchNode);
        parentNode.children.add(branchNode);
      }
    }
  }
  
  private static void convertTreeToSegments(List<RRTNode> tree, List<LightningSegment> segments) {
    for (RRTNode node : tree) {
      if (node.parent != null) {
        segments.add(new LightningSegment(
          node.parent.x, node.parent.y,
          node.x, node.y,
          node.isBranch,
          node.intensity,
          node.depth
        ));
      }
    }
  }
}