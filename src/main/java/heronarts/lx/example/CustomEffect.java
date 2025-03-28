/**
 * Copyright 2022- Mark C. Slee, Heron Arts LLC
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

package heronarts.lx.example;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponentName;
import heronarts.lx.color.LXColor;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.parameter.CompoundParameter;

@LXCategory("Custom")
@LXComponentName("Custom")
public class CustomEffect extends LXEffect {

  public final CompoundParameter knob =
    new CompoundParameter("Knob", 0)
    .setDescription("An example parameter");

	public CustomEffect(LX lx) {
		super(lx);
		addParameter("knob", this.knob);
	}

  @Override
  protected void run(double deltaMs, double enabledAmount) {
    final float knob = this.knob.getValuef();
    final int gray = LXColor.gray(knob * 100);
    final int alpha = (int) (0x100 * enabledAmount);
    for (int i = 0; i < colors.length; ++i) {
      colors[i] = LXColor.lerp(colors[i], gray, alpha);
    }
  }

}
