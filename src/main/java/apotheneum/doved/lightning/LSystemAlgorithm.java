package apotheneum.doved.lightning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LSystemAlgorithm {
  
  public static class Parameters {
    public final int iterations;
    public final double segmentLength;
    public final double angleVariation;
    public final double lengthVariation;
    public final double branchAngle;
    public final int rasterWidth;
    public final int rasterHeight;
    
    public Parameters(int iterations, double segmentLength, double angleVariation, 
                     double lengthVariation, double branchAngle, int rasterWidth, int rasterHeight) {
      this.iterations = iterations;
      this.segmentLength = segmentLength;
      this.angleVariation = angleVariation;
      this.lengthVariation = lengthVariation;
      this.branchAngle = branchAngle;
      this.rasterWidth = rasterWidth;
      this.rasterHeight = rasterHeight;
    }
  }
  
  public static class LightningSegment {
    public final double x1, y1, x2, y2;
    public final boolean isBranch;
    public final double intensity;
    public final int depth;
    
    public LightningSegment(double x1, double y1, double x2, double y2, boolean isBranch, double intensity, int depth) {
      this.x1 = x1;
      this.y1 = y1;
      this.x2 = x2;
      this.y2 = y2;
      this.isBranch = isBranch;
      this.intensity = intensity;
      this.depth = depth;
    }
  }
  
  // L-system symbols
  private static final char FORWARD = 'F';
  private static final char BRANCH_START = '[';
  private static final char BRANCH_END = ']';
  private static final char TURN_LEFT = '+';
  private static final char TURN_RIGHT = '-';
  
  private static final Map<Character, String> L_SYSTEM_RULES = new HashMap<>();
  static {
    L_SYSTEM_RULES.put(FORWARD, "F[+F][-F]F");
    L_SYSTEM_RULES.put(TURN_LEFT, "+");
    L_SYSTEM_RULES.put(TURN_RIGHT, "-");
    L_SYSTEM_RULES.put(BRANCH_START, "[");
    L_SYSTEM_RULES.put(BRANCH_END, "]");
  }
  
  private static class LSystemState {
    public double x, y, angle;
    public int depth;

    public LSystemState(double x, double y, double angle, int depth) {
      this.x = x;
      this.y = y;
      this.angle = angle;
      this.depth = depth;
    }

    public LSystemState copy() {
      return new LSystemState(x, y, angle, depth);
    }
  }
  
  public static void generateLightning(List<LightningSegment> segments, Parameters params) {
    // Generate L-system string
    String lSystemString = generateLSystemString(params.iterations);
    
    // Interpret L-system string to create lightning segments
    interpretLSystem(segments, lSystemString, params);
  }
  
  private static String generateLSystemString(int iterations) {
    String current = "F"; // Start with a simple forward command

    for (int i = 0; i < iterations; i++) {
      StringBuilder next = new StringBuilder();
      
      for (char c : current.toCharArray()) {
        String rule = L_SYSTEM_RULES.get(c);
        if (rule != null) {
          next.append(rule);
        } else {
          next.append(c);
        }
      }
      
      current = next.toString();
    }

    return current;
  }

  private static void interpretLSystem(List<LightningSegment> segments, String lSystemString, Parameters params) {
    // Start from top center, pointing downward
    double startX = params.rasterWidth / 2.0;
    double startY = 0;
    double startAngle = Math.PI / 2; // Point downward (90 degrees from standard orientation)

    LSystemState state = new LSystemState(startX, startY, startAngle, 0);
    List<LSystemState> stateStack = new ArrayList<>();

    double baseAngle = Math.toRadians(params.branchAngle);

    for (char c : lSystemString.toCharArray()) {
      switch (c) {
        case FORWARD:
          // Move forward and draw segment
          double length = params.segmentLength * (1 + (Math.random() - 0.5) * params.lengthVariation);
          double newX = state.x + Math.cos(state.angle) * length;
          double newY = state.y + Math.sin(state.angle) * length;
          
          // Only draw segment if both points are within bounds
          if (newX >= 0 && newX < params.rasterWidth && newY >= 0 && newY < params.rasterHeight) {
            // Calculate intensity based on depth (main trunk stronger)
            double intensity = Math.max(0.7, 1.0 - state.depth * 0.1);
            
            segments.add(new LightningSegment(state.x, state.y, newX, newY, state.depth > 0, intensity, state.depth));
            
            state.x = newX;
            state.y = newY;
          } else {
            // If segment would go out of bounds, stop this branch
            break;
          }
          break;

        case TURN_LEFT:
          // Turn left with some randomness
          double leftAngle = baseAngle + (Math.random() - 0.5) * params.angleVariation * baseAngle;
          state.angle -= leftAngle;
          break;

        case TURN_RIGHT:
          // Turn right with some randomness
          double rightAngle = baseAngle + (Math.random() - 0.5) * params.angleVariation * baseAngle;
          state.angle += rightAngle;
          break;

        case BRANCH_START:
          // Save current state and increase depth
          LSystemState savedState = state.copy();
          savedState.depth++;
          stateStack.add(savedState);
          state.depth++;
          break;

        case BRANCH_END:
          // Restore previous state
          if (!stateStack.isEmpty()) {
            state = stateStack.remove(stateStack.size() - 1);
          }
          break;
      }
    }
  }
}