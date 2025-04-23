/**
 * Quasicrystal Tiling Generator
 * This script iterates across tiling symmetries from 1-fold to 7-fold
 * Each pattern is generated dynamically based on the selected symmetry.
 * Includes 2D/3D/cylindrical radial + vertical spiral modes.
 * @author Theresa Silver
 */

// Mode toggle: false = identical pattern on each face, true = mirrored repetition (north/south flipped)
toggle("mirrorNSFaces", "Mirror North/South Faces",
       "Flip north and south faces horizontally for mirrored repetition",
       true);

// 3D coordinate blending toggle
toggle("use3D", "3D Pattern Mode",
       "Use all three normalized axes (xn, yn, zn) instead of face-local u/v",
       false);

// Radial + vertical spiral mode toggle
toggle("cylinderStyle", "Radial Cylinder Mode",
       "Use polar angle from (xn, zn) + height as horizontal coordinate",
       false);

// Core pattern controls
knob("symmetry", "Symmetry", "Tiling symmetry (1 to 7)", 0.2);
knob("scale", "Scale", "Pattern scale factor", 0.1);
knob("rotation", "Rotation", "Pattern rotation", 0.5);
knob("hue1", "Hue A", "First mapped hue", 1.0);
knob("hue2", "Hue B", "Second mapped hue", 0.8);
knob("hue3", "Hue C", "Third mapped hue", 0.3);
knob("sat", "Saturation", "Color saturation", 0.85);
knob("brt", "Brightness", "Overall brightness", 1.0);
knob("contrast", "Contrast", "Brightness contrast multiplier", 0.65);

// Optional fade control (to dim toward top or center if desired)
toggle("fade", "Fade", "Dim brightness toward center", false);

//Generate a quasicrystal tiling pattern with variable-fold symmetry
function renderPoint(point, deltaMs) {
    var angle = rotation * Math.PI * 2;
    var symmetryFactor = Math.round(1 + symmetry * 6);
    var effectiveScale = 0.2 + scale * 1.5;
    var px, py;
    
    if (use3D) {
        if (cylinderStyle) {
            // Spiral pattern: polar angle plus height for twisting effect
            px = (Math.atan2(point.zn - 0.5, point.xn - 0.5) / (2 * Math.PI)) + point.yn;
        } else {
            var horiz = (point.xn - 0.5) + (point.zn - 0.5);
            px = (horiz * symmetryFactor * effectiveScale) % 1 - 0.5;
        }
        py = ((point.yn * symmetryFactor * effectiveScale) % 1 - 0.5);
    } else {
        var faceIndex, u;
        if (Math.abs(point.xn - 0.5) > Math.abs(point.zn - 0.5)) {
            if (point.xn > 0.5) {
                faceIndex = 1; u = point.zn; // East
            } else {
                faceIndex = 3; u = 1 - point.zn; // West
            }
        } else {
            if (point.zn > 0.5) {
                faceIndex = 2; u = mirrorNSFaces ? point.xn : 1 - point.xn; // South
            } else {
                faceIndex = 0; u = mirrorNSFaces ? 1 - point.xn : point.xn; // North
            }
        }
        var v = point.yn;
        px = ((u * symmetryFactor * effectiveScale) % 1 - 0.5);
        py = ((v * symmetryFactor * effectiveScale) % 1 - 0.5);
    }
    
    // Pattern tiling and scale
    var q = (2/3) * px;
    var r = (-1/3) * px + (Math.sqrt(3)/3) * py;
    var s = -q - r;
    
    var tileValue = Math.cos(symmetryFactor * Math.atan2(s, q) + angle)
    + Math.cos(symmetryFactor * Math.atan2(r, s) + angle);
    
    // Calculate hue from tileValue using three-color blend
    var blend = 0.5 + 0.5 * Math.sin(tileValue * Math.PI);
    var hueBase = (blend < 0.5) ? (hue1 * (1 - 2 * blend) + hue2 * (2 * blend))
    : (hue2 * (2 - 2 * blend) + hue3 * (2 * blend - 1));
    
    // Brightness level with contrast boost
    var level = brt * Math.abs(tileValue) * contrast;
    
    if (fade) {
        var dx = px, dy = py;
        var distCenter = Math.sqrt(dx*dx + dy*dy);
        level *= Math.max(0, 1 - distCenter * 2);
    }
    
    return hsb(hueBase * 360, sat * 100, level * 100);
}
