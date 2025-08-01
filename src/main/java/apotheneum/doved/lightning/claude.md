# Lightning Pattern Algorithm

## Overview
This file describes the algorithm for the Lightning pattern in the Apotheneum installation.

## Algorithm Description

Drawing realistic 2D lightning involves creating a jagged, branching line that simulates the path of a discharge. Several algorithms can achieve this, often incorporating randomness to capture lightning's unpredictable nature.

### 1. Midpoint Displacement Algorithm (Recommended)
This is a popular and relatively simple method:

**Initialization**: Start with a straight line segment between the bolt's origin (e.g., top of the screen) and end point (e.g., bottom of the screen).

**Recursive Displacement**:
- Choose a point (S) on the current line segment, typically near the midpoint
- Displace S randomly perpendicular to the segment by a small amount
- Recursively apply this process to the two new sub-segments created by S (from start to S, and from S to end)
- Continue this recursion until the segments become very small or a desired level of detail is reached

**Branching (Optional)**: To create the realistic branching effect, add a third line segment (a branch) from S with a small random chance during the displacement step.

### 2. L-systems (Lindenmayer Systems)
L-systems offer a powerful way to generate fractal-like structures, including lightning:

- **Define Rules**: Create a set of rules that dictate how a line segment can be replaced with more complex segments and branching structures
- **Apply Recursively**: Apply these rules iteratively to an initial line segment, creating the lightning bolt structure
- **Randomization**: Introduce randomness to the angles and lengths generated by the L-system rules to create a more organic appearance

### 3. Rapidly Exploring Random Trees (RRT)
This algorithm can also generate interesting lightning patterns:

- **Goal-Oriented Growth**: Instead of a simple recursive midpoint displacement, RRT starts at one end and grows the lightning towards a target point
- **Random Samples**: At each step, a random point near the goal is chosen, and the closest point on the existing lightning structure extends towards that random sample
- **Flexibility**: Modifying how the random points are sampled and how the lightning extends can lead to different lightning styles, including ones that wrap around obstacles

### 4. Physically-Based Algorithm
This algorithm simulates the actual physics of lightning formation, based on the stepped leader and return stroke process:

- **Stepped Leader Simulation**: Models the downward-propagating negative charge channel that branches as it seeks ground connection
- **Electric Field Influence**: Each step is influenced by electric field calculations and charge distribution
- **Natural Branching**: Branches occur based on electric potential and field conditions, not random probability
- **Return Stroke**: Once ground connection is made, bright return strokes travel back up the established channels
- **Charge Decay**: Electric potential decreases along the channel, affecting intensity and branching behavior

#### Physically-Based Algorithm Implementation Details

**Phase 1: Stepped Leader Formation**:
- Initialize with high electric potential at cloud base
- Propagate downward in discrete steps based on electric field direction
- Each step influenced by ground attraction and charge distribution
- Create branches when electric field conditions favor it
- Continue until ground connection is made within connection distance

**Phase 2: Return Stroke Generation**:
- Trace path from connection point back to cloud base
- Generate bright return stroke along established channel
- Create secondary return strokes for major branches with sufficient charge
- Return stroke intensity based on accumulated charge in channel

**Phase 3: Rendering**:
- Stepped leader segments rendered dim (30% intensity) to show channel formation
- Return stroke segments rendered bright (100% intensity) for the main discharge
- Thickness varies based on segment intensity and charge distribution
- Corona glow effect for high-intensity return stroke segments

#### RRT Algorithm Implementation Details

**Initialization**:
- Create a tree with a single root node at the lightning origin point
- Define a goal region (target area for lightning to reach)
- Set step size for tree growth (segment length)
- Define bias probability for goal-directed sampling

**Core Algorithm Loop**:
1. **Sample Point**: Generate a random point in the search space
   - With probability `goalBias` (e.g., 0.1), sample directly from goal region
   - Otherwise, sample uniformly from the entire space
2. **Find Nearest**: Locate the closest node in the existing tree to the sampled point
3. **Extend**: Create a new node by extending from the nearest node towards the sample
   - Move a fixed step size in the direction of the sample
   - Add random perpendicular displacement for lightning-like jaggedness
4. **Add Node**: Add the new node to the tree if it doesn't violate constraints
5. **Check Goal**: If the new node reaches the goal region, terminate or continue for denser branching

**Branching Strategy**:
- **Natural Branching**: The tree structure inherently creates branches
- **Forced Branching**: With small probability, extend multiple nodes from the same parent
- **Pruning**: Remove branches that haven't grown recently to focus energy on active paths

**Lightning-Specific Adaptations**:
- **Electrical Field Simulation**: Bias sampling towards areas of high "electrical potential"
- **Stepped Leader Model**: Implement stepped progression with pauses and redirections
- **Return Stroke**: Once main path is established, trace back with higher intensity
- **Charge Accumulation**: Model charge buildup that influences subsequent branching

**Advantages for Lightning**:
- Creates naturally branching, tree-like structures
- Goal-oriented growth mimics electrical discharge seeking shortest path
- Can handle complex obstacles and boundaries
- Produces organic, non-repetitive patterns
- Easily parameterized for different lightning styles

**Implementation Parameters**:
- `stepSize`: Distance of each tree extension (affects detail level)
- `goalBias`: Probability of sampling from goal region (0.0-1.0)
- `maxIterations`: Maximum number of tree extensions
- `branchProbability`: Chance of creating multiple branches from one node
- `jaggedness`: Amount of random perpendicular displacement
- `goalRadius`: Size of the target region considered "reached"
- `electricalField`: Function defining charge distribution for biased sampling

### Visual Enhancement Tips

**Jaggedness and Randomness**: Lightning rarely travels in a perfectly straight line, so introduce random deviations and sharp turns to create a natural, chaotic look.

**Branching and Subtlety**: Add thinner, secondary branches that extend from the main strike, mimicking the branching nature of real lightning. These branches can be dimmer or have a more subtle blur than the main strike.

**Corners and Thickness**: The points where the lightning changes direction can be slightly thicker or form sharp angles or even open loops with negative space inside.

**Color and Glow**: Lightning often emits a brilliant, blue-white light. Experiment with colors and add a glowing or blurred effect around the lightning bolt to enhance its visual impact.

**Animation**: Animating the lightning by rapidly changing its shape and intensity can further enhance the realism and visual appeal. 

## Implementation Notes

### Current Implementation

The lightning package contains one main implementation:

**Lightning.java** - 2D Raster Lightning Pattern
- Extends `ApotheneumRasterPattern` for 2D graphics rendering
- Supports four algorithms: Midpoint Displacement, L-System, RRT, and Physically-Based
- Renders to 2D raster buffer, then maps to installation geometry
- Uses `Graphics2D` for drawing lightning segments with glow effects
- Features dynamic UI that shows only controls relevant to the selected algorithm
- All algorithms use the common `LightningSegment` class for consistency

### Algorithm Implementation Details

**Midpoint Displacement Algorithm**
- Implemented in `MidpointDisplacementAlgorithm.java`
- Recursive subdivision with perpendicular displacement
- Configurable branching probability and branch angles
- Handles boundary constraints to prevent edge artifacts

**L-System Algorithm**
- Implemented in `LSystemAlgorithm.java`
- Uses rule: `F → F[+F][-F]F` for fractal branching
- Fixed intensity calculation to prevent excessive fading: `Math.max(0.4, 1.0 - depth * 0.15)`
- Improved angle calculations for proper downward lightning direction
- Boundary checking prevents segments from creating edge artifacts

**RRT Algorithm**
- Implemented in `RRTAlgorithm.java`
- Tree-based growth from origin towards goal region
- Goal-biased sampling with configurable bias probability
- Natural branching through tree structure expansion
- Stepped leader model with charge accumulation simulation
- Configurable electrical field for realistic discharge patterns
- Support for obstacle avoidance and complex boundary handling
- Integrated into Lightning.java pattern with full UI support
- Optimized rendering with reduced glow for sharper lightning appearance

**Physically-Based Algorithm**
- Implemented in `PhysicallyBasedAlgorithm.java`
- Simulates actual lightning physics: stepped leader formation and return stroke
- Electric field calculations influence step direction and branching
- Charge distribution affects intensity and propagation behavior
- Two-phase rendering: dim stepped leaders followed by bright return strokes
- Natural branching based on electric potential, not random probability
- Corona glow effects for high-intensity return stroke segments
- Realistic blue-white lightning coloration with slight purple tint

### Common Classes

**LightningSegment.java**
- Common segment representation for all algorithms
- Used directly by all three algorithms (no conversion methods needed)
- Stores position, intensity, branching info, and depth

### Apotheneum-Specific Considerations
- Uses raster pattern for face-based rendering across cube and cylinder surfaces
- Supports MIDI triggering and external envelope control for fade effects
- Lightning originates from configurable X position across the top
- Dynamic UI shows only relevant controls for the selected algorithm
- All three algorithms maintain consistent segment representation

### UI Features
- **Dynamic Interface**: UI automatically shows only controls relevant to the selected algorithm
- **Algorithm Selection**: Dropdown to choose between Midpoint, L-System, and RRT
- **Real-time Updates**: UI rebuilds immediately when algorithm changes
- **Organized Controls**: Maximum 3 controls per column for clean layout
- **Algorithm-Specific Parameters**: Each algorithm has its own dedicated control sections

## Parameters

### Lightning Pattern Parameters

**Common Controls (Always Visible)**:
- `algorithm` - Choose between Midpoint Displacement, L-System, and RRT
- `trig` - Manual trigger for lightning strikes
- `intensity` - Overall brightness multiplier
- `startX` - Starting X position across the top (0=left, 1=right)
- `fade` - External envelope control for lightning fade
- `thickness` - Base thickness of lightning bolts
- `bleeding` - Glow/bleeding effect strength for all algorithms

**Midpoint Displacement Controls**:
- `displacement` - Maximum perpendicular displacement
- `recursionDepth` - Subdivision levels for detail
- `startSpread` - How spread out lightning start points are
- `endSpread` - How spread out lightning end points are
- `branchProbability` - Likelihood of creating branches
- `branchDistance` - Maximum distance branches can extend
- `branchAngle` - How much branches can deviate from main bolt

**L-System Controls**:
- `lsIterations` - Number of L-system iterations
- `lsSegmentLength` - Base length of L-system segments
- `lsBranchAngle` - Base angle for L-system branches in degrees
- `lsAngleVariation` - Random variation in L-system angles
- `lsLengthVariation` - Random variation in L-system segment lengths

**RRT Controls**:
- `rrtStepSize` - Distance of each RRT tree extension
- `rrtGoalBias` - Probability of sampling from goal region (0.0-1.0)
- `rrtMaxIterations` - Maximum number of RRT tree extensions
- `rrtJaggedness` - Amount of random perpendicular displacement
- `rrtGoalRadius` - Size of target region considered "reached"
- `rrtElectricalField` - Electrical field strength for biased sampling

### Known Issues Fixed
- **L-System Intensity**: Fixed excessive fading by improving intensity calculation
- **L-System Direction**: Fixed angle calculations for proper downward movement
- **Edge Artifacts**: Prevented segments from being drawn when constrained to bounds
- **Angle Variation**: Reduced excessive randomness in branch directions
- **RRT Blur**: Reduced glow effect for sharper lightning appearance
- **RRT Thickness**: Fixed thickness parameter scaling for proper visual control
- **Dynamic UI**: Implemented proper parameter-based visibility using UI component listeners
- **Segment Consistency**: All algorithms now use common LightningSegment class directly