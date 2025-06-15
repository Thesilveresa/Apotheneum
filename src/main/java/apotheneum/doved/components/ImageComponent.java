package apotheneum.doved.components;

import heronarts.lx.LXComponent;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.LX;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.pattern.image.ImagePattern.Image;

import apotheneum.doved.utils.AssetPaths;
import apotheneum.doved.utils.Kaleidoscope;

public class ImageComponent extends Image implements LXComponent.Renamable, LXOscComponent {

  // Auto-cycle eligibility (following SlideshowPattern pattern)
  public final BooleanParameter autoCycleEligible = new BooleanParameter("Cycle", true)
      .setDescription("Whether the image is eligible for auto-cycle");

  // Kaleidoscope parameters - this is the main additional functionality we need
  public final Kaleidoscope kaleidoscope = new Kaleidoscope();

  private int index;

  public ImageComponent(LX lx, LXComponent parent) {
    super(lx);
    setParent(parent);

    addParameter("autoCycleEligible", this.autoCycleEligible);

    // Add kaleidoscope parameters
    addParameter("segments", this.kaleidoscope.params.segments);
    addParameter("krtheta", this.kaleidoscope.params.rotateTheta);
    addParameter("krphi", this.kaleidoscope.params.rotatePhi);
    addParameter("kx", this.kaleidoscope.params.x);
    addParameter("ky", this.kaleidoscope.params.y);
    addParameter("kz", this.kaleidoscope.params.z);
  }

  @Override
  public void onParameterChanged(LXParameter p) {
    if (p == this.fileName) {
      // Convert relative path to absolute path for loading, following the original
      // pattern
      String fileName = AssetPaths.toAbsolutePathFromAssets(this.fileName.getString());
      if (fileName == null) {
        // Clear the image if path is invalid - let parent handle this
        super.onParameterChanged(p);
      } else {
        // Temporarily set the absolute path for the parent to load, then restore
        // relative path
        String relativePath = this.fileName.getString();
        this.fileName.setValue(fileName); // Set absolute path for parent
        super.onParameterChanged(p); // Let parent load the image
        this.fileName.setValue(relativePath); // Restore relative path for storage
      }
    } else {
      super.onParameterChanged(p); // Call parent's parameter handling for other parameters
    }
  }

  public void advance(double ms) {
    this.animateGif(ms); // Use the parent's GIF animation
  }

  public int getIndex() {
    return this.index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  @Override
  public String getPath() {
    return "image/" + (this.index + 1);
  }

  // All other functionality (hasImage, render, position, scaling, etc.)
  // is inherited from ImagePattern.Image
}