package apotheneum.doved.components;

import heronarts.lx.LXComponent;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.LX;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.pattern.image.ImagePattern.Image;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.color.LXColor;
import heronarts.lx.transform.LXVector;
import heronarts.lx.transform.LXMatrix;
import heronarts.lx.utils.LXUtils;
import heronarts.glx.GLXUtils;

import apotheneum.doved.utils.AssetPaths;
import apotheneum.doved.utils.Kaleidoscope;
import heronarts.lx.parameter.StringParameter;

public class DeformableImage extends Image implements LXComponent.Renamable, LXOscComponent {

  // Auto-cycle eligibility (following SlideshowPattern pattern)
  public final BooleanParameter autoCycleEligible = new BooleanParameter("Cycle", true)
      .setDescription("Whether the image is eligible for auto-cycle");

  // Kaleidoscope parameters - this is the main additional functionality we need
  public final Kaleidoscope kaleidoscope = new Kaleidoscope();

  public final StringParameter fileRelativePath = new StringParameter("FileRelativePath", (String) null);

  private int index;

  // Our own matrix for transformations since the parent's matrix is private
  private final LXMatrix transformMatrix = new LXMatrix();

  public DeformableImage(LX lx) {
    super(lx);

    addParameter("autoCycleEligible", this.autoCycleEligible);

    // Add kaleidoscope parameters
    addParameter("segments", this.kaleidoscope.params.segments);
    addParameter("krtheta", this.kaleidoscope.params.rotateTheta);
    addParameter("krphi", this.kaleidoscope.params.rotatePhi);
    addParameter("kx", this.kaleidoscope.params.x);
    addParameter("ky", this.kaleidoscope.params.y);
    addParameter("kz", this.kaleidoscope.params.z);
    addParameter("fileRelativePath", this.fileRelativePath);
  }

  @Override
  public void onParameterChanged(LXParameter p) {
    if (p == this.fileRelativePath) {
      // Convert relative path to absolute path for loading, following the original
      // pattern
      String relativePath = this.fileRelativePath.getString();

      String absolutePath = AssetPaths.toAbsolutePathFromAssets(relativePath);

      this.fileName.setValue(absolutePath);

      super.onParameterChanged(this.fileName);
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

  /**
   * Compute our own transformation matrix (replicating parent's computeMatrix
   * logic)
   * since the parent's matrix field is private
   */
  private void computeTransformMatrix(LXModel model) {
    float tx = this.translateX.getValuef();
    float ty = this.translateY.getValuef();
    float tz = this.translateZ.getValuef();
    float imageAspect = this.hasImage() ? this.getImage().getAspectRatio() : 1.0f;
    float modelAspect = model.xRange / model.yRange;
    float xAspect = imageAspect > modelAspect ? 1.0f
        : LXUtils.lerpf(1.0f, modelAspect / imageAspect, this.stretchAspect.getValuef());
    float yAspect = imageAspect < modelAspect ? 1.0f
        : LXUtils.lerpf(1.0f, imageAspect / modelAspect, this.stretchAspect.getValuef());
    float scale = this.scale.getValuef() * this.scaleRange.getValuef();

    this.transformMatrix.identity()
        .translate(0.5f, 0.5f, 0.5f)
        .scale(
            xAspect * this.stretchX.getValuef() / LXUtils.maxf(1.0E-4f, scale * this.scaleX.getValuef()),
            yAspect * this.stretchY.getValuef() / LXUtils.maxf(1.0E-4f, scale * this.scaleY.getValuef()),
            1.0f)
        .rotateZ(this.roll.getValuef() * 3.1415927f / 180.0f)
        .rotateX(this.pitch.getValuef() * 3.1415927f / 180.0f)
        .rotateY(this.yaw.getValuef() * 3.1415927f / 180.0f)
        .translate(-0.5f - tx, -0.5f + ty, -0.5f - tz);
  }

  /**
   * Render the image with kaleidoscope deformation applied
   * This is the key method that adds kaleidoscope functionality to the base Image
   * class
   */
  public void render(LXModel model, int[] colors) {
    if (this.hasImage()) {
      this.computeTransformMatrix(model);

      // Get the coordinate function from imageMode (using reflection-like access)
      float scrollX = (1.0f - this.scrollX.getValuef()) % 1.0f;
      float scrollY = (1.0f - this.scrollY.getValuef()) % 1.0f;
      GLXUtils.Image glxImage = this.getImage();

      if (glxImage != null) {
        for (LXPoint p : model.points) {
          // Apply kaleidoscope deformation first - this is our key addition
          LXVector pD = this.kaleidoscope.deform(p.xn, p.yn, p.zn);

          // Apply matrix transformation for position, rotation, scale (same as parent)
          float xn = pD.x * this.transformMatrix.m11 + (1.0f - pD.y) * this.transformMatrix.m12
              + pD.z * this.transformMatrix.m13 + this.transformMatrix.m14;
          float yn = pD.x * this.transformMatrix.m21 + (1.0f - pD.y) * this.transformMatrix.m22
              + pD.z * this.transformMatrix.m23 + this.transformMatrix.m24;

          // Apply coordinate function wrapping (simplified to TILE mode for now)
          if (xn < 0.0f || yn < 0.0f) {
            colors[p.index] = LXColor.BLACK;
          } else {
            // Apply scroll and wrapping
            xn = (xn + scrollX) % 1.0f;
            yn = (yn + scrollY) % 1.0f;
            colors[p.index] = glxImage.getNormalized(xn, yn);
          }
        }
      } else {
        for (LXPoint p : model.points) {
          colors[p.index] = LXColor.BLACK;
        }
      }
    } else {
      // Fill with black if no image
      for (LXPoint p : model.points) {
        colors[p.index] = LXColor.BLACK;
      }
    }
  }

  // All other functionality (hasImage, render, position, scaling, etc.)
  // is inherited from ImagePattern.Image
}