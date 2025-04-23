/**
 * Copyright 2025- Mark C. Slee, Heron Arts LLC
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

package apotheneum.mcslee;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import apotheneum.Apotheneum;
import apotheneum.ApotheneumPattern;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponentName;
import heronarts.lx.color.LXColor;
import heronarts.lx.midi.MidiNoteOn;
import heronarts.lx.parameter.CompoundDiscreteParameter;
import heronarts.lx.parameter.TriggerParameter;
import heronarts.lx.utils.LXUtils;

@LXCategory("Apotheneum/mcslee")
@LXComponentName("DNA Letters")
public class DNALetters extends ApotheneumPattern implements ApotheneumPattern.Midi {

  public static final int LETTER_SIZE = 5;
  public static final int WIDTH = Apotheneum.GRID_WIDTH / LETTER_SIZE;
  public static final int HEIGHT = Apotheneum.GRID_HEIGHT / LETTER_SIZE;

  public final int[] A = {
    0, 1, 1, 1, 0,
    0, 1, 0, 1, 0,
    0, 1, 1, 1, 0,
    0, 1, 0, 1, 0,
    0, 1, 0, 1, 0
  };

  public final int[] C = {
    0, 1, 1, 1, 0,
    1, 0, 0, 0, 0,
    1, 0, 0, 0, 0,
    1, 0, 0, 0, 0,
    0, 1, 1, 1, 0
  };

  public final int[] T = {
    1, 1, 1, 1, 1,
    0, 0, 1, 0, 0,
    0, 0, 1, 0, 0,
    0, 0, 1, 0, 0,
    0, 0, 1, 0, 0
  };

  public final int[] G = {
    0, 1, 1, 1, 0,
    1, 0, 0, 0, 0,
    1, 0, 1, 1, 0,
    1, 0, 0, 0, 1,
    0, 1, 1, 1, 0
  };

  public final int[][] LETTERS = { A, C, T, G };

  private final int[] state = new int[WIDTH * HEIGHT];

  public final TriggerParameter update =
    new TriggerParameter("Update", this::update)
    .setDescription("Updates the letters");

  public final CompoundDiscreteParameter numUpdate =
    new CompoundDiscreteParameter("Num", 0, this.state.length + 1)
    .setDescription("Number of places to change on each update");

  private final List<Integer> shuffle = new ArrayList<>();

  public DNALetters(LX lx) {
    super(lx);
    addParameter("update", this.update);
    addParameter("numUpdate", this.numUpdate);
    randomize();
    for (int i = 0; i < this.state.length; ++i) {
      this.shuffle.add(i);
    }
  }

  private void randomize() {
    for (int i = 0; i < this.state.length; ++i) {
      this.state[i] = LXUtils.randomi(LETTERS.length-1);
    }
  }

  private void update() {
    Collections.shuffle(this.shuffle);
    int numUpdate = this.numUpdate.getValuei();
    for (int i = 0; i < numUpdate; ++i) {
      int idx = this.shuffle.get(i);
      this.state[idx] = (this.state[idx] + LXUtils.randomi(LETTERS.length-2)) % LETTERS.length;
    }
  }

  private void renderLetter(int[] letters, int x, int y, Apotheneum.Cube.Face face) {
    int z = 0;
    for (int j = 0; j < LETTER_SIZE; ++j) {
      for (int i = 0; i < LETTER_SIZE; ++i) {
        if (letters[z++] > 0) {
          int idx = (x+i) * Apotheneum.GRID_HEIGHT + (y+j);
          colors[face.model.points[idx].index] = LXColor.WHITE;
        }
      }
    }
  }

  @Override
  protected void render(double deltaMs) {
    setColors(LXColor.BLACK);
    for (int x = 0; x < WIDTH; ++x) {
      for (int y = 0; y < HEIGHT; ++y) {
        renderLetter(LETTERS[this.state[x * HEIGHT + y]], x * LETTER_SIZE, y * LETTER_SIZE, Apotheneum.cube.exterior.front);
      }
    }

    // Blit onto the other faces
    copy(Apotheneum.cube.exterior.front, Apotheneum.cube.exterior.right);
    copy(Apotheneum.cube.exterior.front, Apotheneum.cube.exterior.left);
    copy(Apotheneum.cube.exterior.front, Apotheneum.cube.exterior.back);
    copyCubeExterior();
  }

  @Override
  public void noteOnReceived(MidiNoteOn note) {
    update();
  }

}
