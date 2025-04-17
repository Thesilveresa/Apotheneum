knobi("index", "Index", "Swatch color to render", 0, 5);

/**
 * Return a color value for the given LXPoint
 * @param {LXPoint} point - The point to render
 * @param {number} deltaMs - Milliseconds elapsed since previous frame
 * @return {number} Color value returned from an LXColor method like hsb/rgb
 */
function renderPoint(point, deltaMs) {
  return _swatch.colors[index].color;  
}
