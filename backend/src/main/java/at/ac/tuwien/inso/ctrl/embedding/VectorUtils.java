package at.ac.tuwien.inso.ctrl.embedding;

import java.util.List;

public final class VectorUtils {
  private VectorUtils() {}

  public static float[] fromList(List<Float> vectorList) {
    if (vectorList == null || vectorList.isEmpty()) {
      return null;
    }

    boolean hasData = false;
    float[] vector = new float[vectorList.size()];
    for (int i = 0; i < vectorList.size(); i++) {
      float val = vectorList.get(i) != null ? vectorList.get(i) : 0f;
      vector[i] = val;
      if (val != 0f) {
        hasData = true;
      }
    }

    return hasData ? vector : null;
  }
}
