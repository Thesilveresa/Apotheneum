package apotheneum.doved.lightning;

public enum LightningAlgorithm {
  MIDPOINT("Midpoint"),
  L_SYSTEM("L-System"),
  RRT("RRT"),
  PHYSICAL("Physical");

  private final String displayName;

  LightningAlgorithm(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public static String[] getDisplayNames() {
    LightningAlgorithm[] values = values();
    String[] names = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      names[i] = values[i].displayName;
    }
    return names;
  }
}