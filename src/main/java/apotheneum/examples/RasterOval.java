package apotheneum.examples;

import java.awt.Color;
import java.awt.Graphics2D;

import apotheneum.ApotheneumRasterPattern;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponentName;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;

@LXCategory("Apotheneum/Examples")
@LXComponentName("Raster Oval")
public class RasterOval extends ApotheneumRasterPattern implements UIDeviceControls<RasterOval> {

  public final CompoundParameter width =
    new CompoundParameter("Width", .5)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Oval Width");

  public final CompoundParameter height =
    new CompoundParameter("Height", .5)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setDescription("Oval Height");

  public RasterOval(LX lx) {
    super(lx);
    addParameter("width", this.width);
    addParameter("height", this.height);
  }

  @Override
  protected void render(double deltaMs, Graphics2D graphics) {
    clear();
    graphics.setColor(Color.RED);
    graphics.fillOval(0, 0, (int) (RASTER_WIDTH * this.width.getValue()), (int) (RASTER_HEIGHT * this.height.getValue()));
  }

  @Override
  public void buildDeviceControls(UI ui, UIDevice uiDevice, RasterOval rasterExample) {
    uiDevice.setLayout(UIDevice.Layout.HORIZONTAL);
    uiDevice.setChildSpacing(4);

    addColumn(uiDevice, "Position",
      buildFaceControls(ui, uiDevice, 120)
    ).setWidth(120);

    addVerticalBreak(ui, uiDevice);

    addColumn(uiDevice, "Size",
      newKnob(this.width),
      newKnob(this.height)
    );
  }

}
