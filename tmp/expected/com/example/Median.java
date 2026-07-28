package com.example;

import org.apache.commons.math4.legacy.exception.MathIllegalArgumentException;
import org.apache.commons.math4.legacy.stat.ranking.NaNStrategy;

public class Median extends Percentile {

  private static final double FIXED_QUANTILE_50 = 0.0d;

  private Median(
      final EstimationType estimationType,
      final NaNStrategy nanStrategy,
      final KthSelector kthSelector)
      throws MathIllegalArgumentException {
    super(FIXED_QUANTILE_50, estimationType, nanStrategy, kthSelector);
  }

  public Median withNaNStrategy(final NaNStrategy newNaNStrategy) {
    return new Median(getEstimationType(), newNaNStrategy, getKthSelector());
  }
}
