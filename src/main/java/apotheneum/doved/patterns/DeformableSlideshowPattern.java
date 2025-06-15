/**
 * Copyright 2020- Mark C. Slee, Heron Arts LLC
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
 * @author Mark C. Slee <mark@heronarts.com>
 */

package apotheneum.doved.patterns;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.UIDuplicate;
import heronarts.glx.ui.component.UIButton;
import heronarts.glx.ui.component.UIDoubleBox;
import heronarts.glx.ui.component.UIDropMenu;
import heronarts.glx.ui.component.UIInputBox;
import heronarts.glx.ui.component.UIItemList;
import heronarts.glx.ui.component.UIKnob;
import heronarts.glx.ui.component.UIParameterControl;
import heronarts.glx.ui.component.UITextBox;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponentName;
import heronarts.lx.LXSerializable;
import heronarts.lx.ModelBuffer;
import heronarts.lx.blend.AddBlend;
import heronarts.lx.blend.DarkestBlend;
import heronarts.lx.blend.DifferenceBlend;
import heronarts.lx.blend.DissolveBlend;
import heronarts.lx.blend.LXBlend;
import heronarts.lx.blend.LightestBlend;
import heronarts.lx.blend.MultiplyBlend;
import heronarts.lx.clipboard.LXClipboardComponent;
import heronarts.lx.clipboard.LXClipboardItem;
import heronarts.lx.color.LXColor;
import heronarts.lx.command.LXCommand;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.ObjectParameter;
import heronarts.lx.parameter.StringParameter;
import heronarts.lx.pattern.LXPattern;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import heronarts.lx.utils.LXUtils;
import heronarts.lx.transform.LXVector;
import heronarts.glx.GLXUtils;
import apotheneum.doved.utils.AssetPaths;
import heronarts.lx.studio.LXStudio.UI;
import apotheneum.doved.components.ImageComponent;
import apotheneum.Apotheneum;

/**
 * DeformableSlideshowPattern - A slideshow pattern with kaleidoscope
 * deformation
 * 
 * NOTE: This class duplicates much of the functionality from SlideshowPattern
 * because:
 * 1. SlideshowPattern uses SlideshowPattern.Image objects, but we need
 * ImageComponent objects with kaleidoscope functionality
 * 2. Many methods in SlideshowPattern are final and cannot be overridden
 * 3. The rendering logic needs to be completely different to apply kaleidoscope
 * deformation
 * 
 * This follows the same structure and patterns as SlideshowPattern but with
 * kaleidoscope deformation added.
 */
@LXCategory(Apotheneum.IMAGE_CATEGORY)
@LXComponentName("Kaleidoscope Slideshow")
public class DeformableSlideshowPattern extends LXPattern
    implements UIDeviceControls<DeformableSlideshowPattern> {

  public enum AutoCycleMode {
    NEXT,
    RANDOM;

    @Override
    public String toString() {
      switch (this) {
        case NEXT:
          return "Next";
        default:
        case RANDOM:
          return "Random";
      }
    }
  };

  public interface Listener {
    public void imageAdded(DeformableSlideshowPattern slideshow, ImageComponent image);

    public void imageRemoved(DeformableSlideshowPattern slideshow, ImageComponent image);

    public void imageMoved(DeformableSlideshowPattern slideshow, ImageComponent image);

    public void imageWillChange(DeformableSlideshowPattern slideshow, ImageComponent from, ImageComponent to);

    public void imageDidChange(DeformableSlideshowPattern slideshow, ImageComponent image);
  }

  private final List<Listener> listeners = new ArrayList<Listener>();
  private final List<ImageComponent> mutableImages = new ArrayList<ImageComponent>();
  public final List<ImageComponent> images = Collections.unmodifiableList(this.mutableImages);

  // Slideshow parameters (following SlideshowPattern structure)
  public final BooleanParameter autoCycleEnabled = new BooleanParameter("Auto-Cycle", false)
      .setDescription("When enabled, this channel will automatically cycle between its patterns");

  public final EnumParameter<AutoCycleMode> autoCycleMode = new EnumParameter<AutoCycleMode>("Auto-Cycle Mode",
      AutoCycleMode.NEXT)
      .setDescription("Mode of auto cycling");

  public final BoundedParameter autoCycleTimeSecs = new BoundedParameter("Cycle Time", 1, 0, 60 * 60 * 4)
      .setDescription("Sets the number of seconds after which the channel cycles to the next image")
      .setUnits(LXParameter.Units.SECONDS);

  public final BoundedParameter transitionTimeSecs = new BoundedParameter("Transition Time", 5, .1, 180)
      .setDescription("Sets the duration of blending transitions between images")
      .setUnits(LXParameter.Units.SECONDS);

  public final BooleanParameter transitionEnabled = new BooleanParameter("Transitions", true)
      .setDescription("When enabled, transitions between images use a blend");

  public final ObjectParameter<LXBlend> transitionBlendMode = new ObjectParameter<LXBlend>("Blend Mode", new LXBlend[] {
      new DissolveBlend(lx),
      new AddBlend(lx),
      new MultiplyBlend(lx),
      new LightestBlend(lx),
      new DarkestBlend(lx),
      new DifferenceBlend(lx)
  })
      .setDescription("Which blend to use between images");

  public final DiscreteParameter focusedImage = new DiscreteParameter("Focused Image", 0, 1)
      .setDescription("Which image has focus in the UI");

  private static final int NO_IMAGE_INDEX = -1;
  private double autoCycleProgress = 0;
  private double transitionProgress = 0;
  private int activeImageIndex = NO_IMAGE_INDEX;
  private int nextImageIndex = NO_IMAGE_INDEX;
  private long transitionMillis = 0;
  private boolean inTransition = false;
  private final ModelBuffer blendBuffer = new ModelBuffer(lx);
  private final List<ImageComponent> randomEligible = new ArrayList<ImageComponent>();

  public DeformableSlideshowPattern(LX lx) {
    super(lx);

    addParameter("autoCycleEnabled", this.autoCycleEnabled);
    addParameter("autoCycleMode", this.autoCycleMode);
    addParameter("autoCycleTimeSecs", this.autoCycleTimeSecs);
    addParameter("transitionEnabled", this.transitionEnabled);
    addParameter("transitionBlendMode", this.transitionBlendMode);
    addParameter("transitionTimeSecs", this.transitionTimeSecs);
    addParameter("focusedImage", this.focusedImage);
    addArray("image", this.images);
  }

  private void _reindexImages() {
    int i = 0;
    for (ImageComponent image : this.images) {
      image.setIndex(i++);
    }
  }

  public ImageComponent getActiveImage() {
    return (this.activeImageIndex >= 0) ? this.images.get(this.activeImageIndex) : null;
  }

  public ImageComponent getNextImage() {
    return (this.nextImageIndex >= 0) ? this.images.get(this.nextImageIndex) : null;
  }

  public ImageComponent getTargetImage() {
    return (this.inTransition) ? getNextImage() : getActiveImage();
  }

  private DeformableSlideshowPattern goImage(ImageComponent image) {
    return goImage(image.getIndex());
  }

  public DeformableSlideshowPattern goImage(int index) {
    if (index < 0 || index >= this.images.size()) {
      throw new IllegalArgumentException("Image index out of bounds: " + index);
    }
    if (this.inTransition) {
      finishTransition();
    }
    this.nextImageIndex = index;
    startTransition();
    return this;
  }

  public DeformableSlideshowPattern goNextImage() {
    if (this.inTransition) {
      return this;
    }
    if (this.images.size() <= 1) {
      return this;
    }
    this.nextImageIndex = this.activeImageIndex;
    do {
      this.nextImageIndex = (this.nextImageIndex + 1) % this.images.size();
    } while ((this.nextImageIndex != this.activeImageIndex) && !getNextImage().autoCycleEligible.isOn());
    if (this.nextImageIndex != this.activeImageIndex) {
      startTransition();
    }
    return this;
  }

  public final DeformableSlideshowPattern goRandomImage() {
    if (this.inTransition) {
      return this;
    }
    if (this.images.size() <= 1) {
      return this;
    }
    ImageComponent activeImage = getActiveImage();
    this.randomEligible.clear();
    for (ImageComponent image : this.images) {
      if (image != activeImage && image.autoCycleEligible.isOn()) {
        this.randomEligible.add(image);
      }
    }
    int numEligible = this.randomEligible.size();
    if (numEligible > 0) {
      return goImage(
          this.randomEligible.get(LXUtils.constrain((int) LXUtils.random(0, numEligible), 0, numEligible - 1)));
    }
    return this;
  }

  private void startTransition() {
    ImageComponent activeImage = getActiveImage();
    ImageComponent nextImage = getNextImage();
    if (activeImage == nextImage) {
      return;
    }
    for (Listener listener : this.listeners) {
      listener.imageWillChange(this, activeImage, nextImage);
    }
    if (this.transitionEnabled.isOn()) {
      this.inTransition = true;
      this.transitionMillis = this.lx.engine.nowMillis;
    } else {
      finishTransition();
    }
  }

  private void cancelTransition() {
    if (this.inTransition) {
      this.inTransition = false;
      this.transitionMillis = this.lx.engine.nowMillis;
      this.nextImageIndex = this.activeImageIndex;
      ImageComponent activeImage = getActiveImage();
      for (Listener listener : listeners) {
        listener.imageDidChange(this, activeImage);
      }
    }
  }

  private void finishTransition() {
    this.activeImageIndex = this.nextImageIndex;
    ImageComponent activeImage = getActiveImage();
    this.inTransition = false;
    this.transitionMillis = this.lx.engine.nowMillis;
    for (Listener listener : listeners) {
      listener.imageDidChange(this, activeImage);
    }
  }

  public ImageComponent getFocusedImage() {
    if (this.images.isEmpty()) {
      return null;
    }
    return this.images.get(this.focusedImage.getValuei());
  }

  public ImageComponent addImage() {
    return addImage(-1, null);
  }

  public ImageComponent addImage(int index) {
    return addImage(index, null);
  }

  public ImageComponent addImage(int index, JsonObject imageObj) {
    if (index > this.images.size()) {
      throw new IllegalArgumentException("Cannot add image at invalid index: " + index);
    }

    ImageComponent focusedImage = getFocusedImage();
    ImageComponent image = new ImageComponent(this.lx, this);

    if (index < 0) {
      this.mutableImages.add(image);
    } else {
      this.mutableImages.add(index, image);
    }
    _reindexImages();

    if (imageObj != null) {
      image.load(lx, imageObj);
    } else {
      image.label.setValue("Image-" + (image.getIndex() + 1));
    }

    for (Listener listener : this.listeners) {
      listener.imageAdded(this, image);
    }

    this.focusedImage.setRange(Math.max(1, this.images.size()));
    if (focusedImage != null) {
      this.focusedImage.setValue(focusedImage.getIndex());
    } else {
      this.focusedImage.bang();
    }

    // First image added, automatically becomes active
    if (this.images.size() == 1) {
      this.activeImageIndex = this.nextImageIndex = 0;
      ImageComponent activeImage = getActiveImage();
      for (Listener listener : this.listeners) {
        listener.imageDidChange(this, activeImage);
      }
    }

    return image;
  }

  public void removeImage(ImageComponent image) {
    int index = this.mutableImages.indexOf(image);
    if (index < 0) {
      throw new IllegalArgumentException("Cannot remove image not in slideshow: " + image);
    }

    int focusedImageIndex = this.focusedImage.getValuei();

    if (this.inTransition) {
      if (image == getNextImage()) {
        cancelTransition();
      } else if (image == getActiveImage()) {
        finishTransition();
      }
    }

    ImageComponent activeImage = getActiveImage();

    this.mutableImages.remove(image);
    _reindexImages();

    // Update indices
    if (this.activeImageIndex > index) {
      --this.activeImageIndex;
    } else if (this.activeImageIndex >= this.mutableImages.size()) {
      this.activeImageIndex = this.mutableImages.size() - 1;
    }
    if (this.nextImageIndex > index) {
      --this.nextImageIndex;
    } else if (this.nextImageIndex >= this.mutableImages.size()) {
      this.nextImageIndex = this.mutableImages.size() - 1;
    }

    if (focusedImageIndex > index) {
      --focusedImageIndex;
    } else if (focusedImageIndex >= this.images.size()) {
      focusedImageIndex = this.images.size() - 1;
    }
    if ((focusedImageIndex >= 0) && (this.focusedImage.getValuei() != focusedImageIndex)) {
      this.focusedImage.setValue(focusedImageIndex);
    } else {
      this.focusedImage.bang();
    }

    this.focusedImage.setRange(Math.max(1, this.images.size()));

    for (Listener listener : this.listeners) {
      listener.imageRemoved(this, image);
    }

    // If we removed the active image, update it
    if ((activeImage == image) && !this.images.isEmpty()) {
      activeImage = getActiveImage();
      for (Listener listener : this.listeners) {
        listener.imageDidChange(this, activeImage);
      }
    }

    image.dispose();
  }

  public void moveImage(ImageComponent image, int index) {
    ImageComponent focusedImage = getFocusedImage();
    ImageComponent activeImage = getActiveImage();
    ImageComponent nextImage = getNextImage();

    this.mutableImages.remove(image);
    this.mutableImages.add(index, image);
    _reindexImages();

    this.activeImageIndex = activeImage.getIndex();
    this.nextImageIndex = nextImage.getIndex();

    if (image == focusedImage) {
      this.focusedImage.setValue(image.getIndex());
    }

    for (Listener listener : this.listeners) {
      listener.imageMoved(this, image);
    }
  }

  public void addListener(Listener listener) {
    Objects.requireNonNull(listener, "May not add null DeformableSlideshowPattern.Listener");
    if (this.listeners.contains(listener)) {
      throw new IllegalStateException("May not add duplicate DeformableSlideshowPattern.Listener: " + listener);
    }
    this.listeners.add(listener);
  }

  public void removeListener(Listener listener) {
    if (!this.listeners.contains(listener)) {
      throw new IllegalStateException("May not remove non-registered DeformableSlideshowPattern.Listener: " + listener);
    }
    this.listeners.remove(listener);
  }

  public double getAutoCycleProgress() {
    return this.autoCycleProgress;
  }

  public double getTransitionProgress() {
    return this.transitionProgress;
  }

  @Override
  protected void run(double deltaMs) {
    // Check for transition completion
    if (this.inTransition) {
      double transitionMs = this.lx.engine.nowMillis - this.transitionMillis;
      double transitionDone = 1000 * this.transitionTimeSecs.getValue();
      if (transitionMs >= transitionDone) {
        finishTransition();
      }
    }

    // Auto-cycle if appropriate
    if (!this.inTransition) {
      this.autoCycleProgress = (this.lx.engine.nowMillis - this.transitionMillis)
          / (1000 * this.autoCycleTimeSecs.getValue());
      if (this.autoCycleProgress >= 1) {
        this.autoCycleProgress = 1;
        if (this.autoCycleEnabled.isOn()) {
          switch (this.autoCycleMode.getEnum()) {
            case NEXT:
              goNextImage();
              break;
            case RANDOM:
              goRandomImage();
              break;
          }
        }
      }
    }

    // Calculate transition progress
    if (this.inTransition) {
      this.autoCycleProgress = 1.;
      this.transitionProgress = (this.lx.engine.nowMillis - this.transitionMillis)
          / (1000 * this.transitionTimeSecs.getValue());
    } else {
      this.transitionProgress = 0;
    }

    ImageComponent image = getActiveImage();
    if (image == null || !image.hasImage()) {
      setColors(LXColor.BLACK);
    } else {
      image.advance(deltaMs);
      renderImageWithKaleidoscope(image, this.colors, deltaMs);

      if (this.inTransition) {
        int[] blendColors = this.blendBuffer.getArray();
        renderImageWithKaleidoscope(getNextImage(), blendColors, deltaMs);
        this.transitionBlendMode.getObject().lerp(this.colors, blendColors, transitionProgress, this.colors,
            this.model);
      }
    }
  }

  /**
   * Custom rendering with kaleidoscope deformation
   * This is the key difference from SlideshowPattern - we apply kaleidoscope
   * deformation to each pixel
   */
  protected void renderImageWithKaleidoscope(ImageComponent image, int[] buffer, double deltaMs) {
    if (image != null && image.hasImage()) {
      // ImageComponent IS an Image, so we can use it directly
      image.computeMatrix(this.model);

      for (LXPoint p : model.points) {
        // Apply kaleidoscope deformation first - this is our key addition
        LXVector pD = image.kaleidoscope.deform(p.xn, p.yn, p.zn);

        // Then render using the Image's computed matrix and getNormalized method
        GLXUtils.Image glxImage = image.getImage();
        if (glxImage != null) {
          // Use TILE wrapping (equivalent to the TILE mode from the original
          // ImageComponent)
          float xn = pD.x - (float) Math.floor(pD.x);
          float yn = pD.y - (float) Math.floor(pD.y);
          buffer[p.index] = glxImage.getNormalized(xn, yn);
        } else {
          buffer[p.index] = LXColor.BLACK;
        }
      }
    } else {
      // Fill with black if no image
      for (LXPoint p : model.points) {
        buffer[p.index] = LXColor.BLACK;
      }
    }
  }

  private static final String KEY_IMAGES = "images";

  @Override
  public void save(LX lx, JsonObject obj) {
    super.save(lx, obj);
    obj.add(KEY_IMAGES, LXSerializable.Utils.toArray(lx, this.images));
  }

  @Override
  public void load(LX lx, JsonObject obj) {
    for (int i = this.images.size() - 1; i >= 0; --i) {
      removeImage(this.images.get(i));
    }
    if (obj.has(KEY_IMAGES)) {
      JsonArray imageArr = obj.getAsJsonArray(KEY_IMAGES);
      for (JsonElement imageElem : imageArr) {
        addImage().load(lx, imageElem.getAsJsonObject());
      }
    }
    super.load(lx, obj);
  }

  @Override
  public void dispose() {
    for (ImageComponent image : this.images) {
      image.dispose();
    }
    this.mutableImages.clear();
    this.blendBuffer.dispose();
    this.listeners.clear();
    super.dispose();
  }

  private ImageList imageList;

  // UI Controls implementation (simplified version of the original)
  @Override
  public void buildDeviceControls(UI ui, UIDevice uiDevice, DeformableSlideshowPattern slideshow) {
    uiDevice.setLayout(UIDevice.Layout.HORIZONTAL, 4);

    final int listWidth = 140;

    this.imageList = new ImageList(ui, 0, 0, listWidth, 100);
    this.imageList.setDescription("Images loaded into the slideshow, click to select, double-click to activate");
    this.imageList
        .setReorderable(true)
        .setRenamable(true)
        .setDeletable(true)
        .setShowCheckboxes(true);

    // Basic slideshow controls
    addColumn(uiDevice, 140,
        new UIButton.Action(140, 16, "Add Image") {
          @Override
          public void onClick() {
            ui.lx.showOpenFileDialog(
                "Open Image",
                "Image File",
                new String[] { "jpg", "jpeg", "png", "gif" },
                new File("Assets/").toString(),
                (path) -> {
                  if (AssetPaths.isInAssetsFolder(path)) {
                    ImageComponent image = slideshow.addImage();
                    // Store the relative path for portability
                    String relativePath = AssetPaths.toRelativePathFromAssets(path);
                    image.fileName.setValue(relativePath);
                    // Set a nice label from the filename
                    String name = new File(path).getName();
                    int dotIndex = name.lastIndexOf('.');
                    if (dotIndex > 0) {
                      name = name.substring(0, dotIndex);
                    }
                    image.label.setValue(name);
                  } else {
                    LX.error(null, "Image file must be within the Assets directory");
                  }
                });
          }
        }.setDescription("Add a new image to the slideshow"));

    // Kaleidoscope controls for the focused image
    ImageComponent focusedImage = slideshow.getFocusedImage();
    if (focusedImage != null) {
      addColumn(uiDevice, UIKnob.WIDTH, "Kaleidoscope",
          newKnob(focusedImage.kaleidoscope.params.segments, 0),
          newKnob(focusedImage.kaleidoscope.params.rotatePhi, 0),
          newKnob(focusedImage.kaleidoscope.params.rotateTheta, 0)).setChildSpacing(6);

      addColumn(uiDevice, UIKnob.WIDTH, "K-Center",
          newKnob(focusedImage.kaleidoscope.params.x, 0),
          newKnob(focusedImage.kaleidoscope.params.y, 0),
          newKnob(focusedImage.kaleidoscope.params.z, 0)).setChildSpacing(6);

      addColumn(uiDevice, UIKnob.WIDTH, "Transform",
          newKnob(focusedImage.translateX, 0),
          newKnob(focusedImage.translateY, 0),
          newKnob(focusedImage.scale, 0)).setChildSpacing(6);

      addColumn(uiDevice, UIKnob.WIDTH, "Scroll",
          newKnob(focusedImage.scrollX, 0),
          newKnob(focusedImage.scrollY, 0)).setChildSpacing(6);
    }
  }

  @Override
  public void disposeDeviceControls(UI ui, UIDevice uiDevice, DeformableSlideshowPattern slideshow) {
    // Clean up if needed
  }
}