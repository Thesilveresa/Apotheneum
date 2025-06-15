package apotheneum.doved.ui;

/**
 * Copyright 2023- Mark C. Slee, Heron Arts LLC
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

import java.io.File;

import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UIButton;
import heronarts.glx.ui.component.UIKnob;
import heronarts.glx.ui.component.UITextBox;
import heronarts.lx.LX;
import heronarts.lx.command.LXCommand;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.studio.ui.device.UIControls;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import apotheneum.doved.patterns.DeformableImagePattern;
import apotheneum.doved.components.DeformableImage;
import apotheneum.doved.utils.AssetPaths;

public class UIDeformableImagePattern implements UIDeviceControls<DeformableImagePattern> {

  static class Command {

    static class ReplaceImage extends LXCommand {

      private final ComponentReference<DeformableImage> image;
      private final String oldPath, newPath;
      private final String oldLabel;

      ReplaceImage(DeformableImage image, String relativePath) {
        this.image = new ComponentReference<DeformableImage>(image);
        this.oldPath = image.fileName.getString();
        this.oldLabel = image.label.getString();
        this.newPath = relativePath;
      }

      @Override
      public String getDescription() {
        return "Replace Image";
      }

      @Override
      public void perform(LX lx) throws InvalidCommandException {
        setImagePath(this.image.get(), this.newPath);
      }

      @Override
      public void undo(LX lx) throws InvalidCommandException {
        this.image.get().fileName.setValue(this.oldPath);
        this.image.get().label.setValue(this.oldLabel);
      }

      static void setImagePath(DeformableImage image, String relativePath) {
        int separator = relativePath.lastIndexOf(File.separatorChar);
        String name;
        if (separator > 0) {
          name = relativePath.substring(separator + 1);
        } else {
          name = relativePath;
        }
        image.label.setValue(name);
        image.fileRelativePath.setValue(relativePath);
      }

    }
  }

  public static class ImageControls extends UI2dContainer implements UIControls {

    public final DeformableImage image;

    protected ImageControls(UI ui, DeformableImage image) {
      super(0, 0, 0, UIDevice.CONTENT_HEIGHT);
      this.image = image;
      setLayout(UI2dContainer.Layout.HORIZONTAL, 4);

      addImageColumn(ui);

      addVerticalBreak(ui, this);

      // Add kaleidoscope controls - this is the key addition
      addColumn(this, UIKnob.WIDTH, "Kaleidoscope",
          newKnob(image.kaleidoscope.params.segments, 0),
          newKnob(image.kaleidoscope.params.rotatePhi, 0),
          newKnob(image.kaleidoscope.params.rotateTheta, 0)).setChildSpacing(6);

      addColumn(this, UIKnob.WIDTH, "K-Center",
          newKnob(image.kaleidoscope.params.x, 0),
          newKnob(image.kaleidoscope.params.y, 0),
          newKnob(image.kaleidoscope.params.z, 0)).setChildSpacing(6);

      addVerticalBreak(ui, this);

      addColumn(this, UIKnob.WIDTH, "Rotate",
          newKnob(image.yaw, 0),
          newKnob(image.pitch, 0),
          newKnob(image.roll, 0)).setChildSpacing(6);

      addVerticalBreak(ui, this);

      addColumn(this, UIKnob.WIDTH, "Move",
          newKnob(image.translateX, 0),
          newKnob(image.translateY, 0),
          newKnob(image.translateZ, 0)).setChildSpacing(6);

      addVerticalBreak(ui, this);

      addColumn(this, UIKnob.WIDTH, "Scale",
          newKnob(image.scaleX, 0),
          newKnob(image.scaleY, 0),
          newKnob(image.scale, 0)).setChildSpacing(6);

      addColumn(this, UIKnob.WIDTH, "Scroll",
          newKnob(image.scrollX, 0),
          newKnob(image.scrollY, 0),
          newDoubleBox(image.scaleRange, UIKnob.WIDTH).setTopMargin(6),
          controlLabel(ui, "Range", UIKnob.WIDTH).setTopMargin(1)).setChildSpacing(6).setLeftMargin(2);

      addColumn(this, UIKnob.WIDTH, "Stretch",
          newKnob(image.stretchX, 0),
          newKnob(image.stretchY, 0),
          newKnob(image.stretchAspect, 0)).setChildSpacing(6).setLeftMargin(2).setRightMargin(2);

    }

    protected void addGifColumn(UI ui) {
      final UITextBox numFrames;
      final UI2dComponent gifDivider = addVerticalBreak(ui, this);

      final UI2dContainer gif = addColumn(this, "GIF",
          newButton(image.gifAnimating).setTriggerable(true),
          newButton(image.gifLooping).setTriggerable(true),
          new UIButton.Action(COL_WIDTH, 16, "Restart").setParameter(image.gifRestart).setTriggerable(true),
          numFrames = (UITextBox) newTextBox(null).setTopMargin(27),
          newDoubleBox(image.gifCycleTimeMs),
          controlLabel(ui, "Frame").setTopMargin(0)).setChildSpacing(4);

      addListener(image.isAnimatedGif, p -> {
        final boolean isGif = image.isAnimatedGif.isOn();
        numFrames.setValue(String.format("%d / %d", image.getGifIndex() + 1, image.getNumGifFrames()));
        gifDivider.setVisible(isGif);
        gif.setVisible(isGif);
      }, true);

      addListener(image.gifIndexChanged, p -> {
        numFrames.setValue(String.format("%d / %d", image.getGifIndex() + 1, image.getNumGifFrames()));
      }, true);
    }

    protected UI2dContainer addDefaultImageColumn(UI ui) {
      final UITextBox fileName;

      final UI2dContainer col = addColumn(this, "Image",
          fileName = (UITextBox) newTextBox(null).setEditable(false),
          newDropMenu(image.imageMode),
          sectionLabel("Actions").setTopMargin(4),
          new UIButton.Action(COL_WIDTH, 16, "Open") {
            @Override
            public void onClick() {
              ui.lx.showOpenFileDialog(
                  "Open Image",
                  "Image File",
                  new String[] { "jpg", "jpeg", "png", "gif" },
                  new File("Assets/").toString(),
                  (path) -> {
                    // Check if the given path is within the "Assets" directory
                    if (AssetPaths.isInAssetsFolder(path)) {
                      String relativePath = AssetPaths.toRelativePathFromAssets(path);
                      ui.lx.command.perform(new Command.ReplaceImage(image, relativePath));
                    } else {
                      LX.error(null, "Image file must be within the Assets directory");
                    }
                  });
            }
          }.setDescription("Use this button to load a new image file into this slot"),
          new UIButton.Action(COL_WIDTH, 16, "Reload").setParameter(image.reload)).setChildSpacing(4);

      addListener(image.fileRelativePath, p -> {
        final String name = image.fileRelativePath.getString();

        if (name == null) {
          fileName.setValue("-");
        } else {
          final int slash = name.lastIndexOf(File.separator);
          String displayName = slash >= 0 ? name.substring(slash + 1) : name;
          fileName.setValue(displayName);
        }
      }, true);

      addGifColumn(ui);

      return col;
    }

    protected void addImageColumn(UI ui) {
      addDefaultImageColumn(ui);
    }
  }

  @Override
  public void buildDeviceControls(UI ui, UIDevice uiDevice, DeformableImagePattern device) {
    uiDevice.setLayout(UIDevice.Layout.HORIZONTAL, 4);
    new ImageControls(ui, device.image).addToContainer(uiDevice);
  }

}
