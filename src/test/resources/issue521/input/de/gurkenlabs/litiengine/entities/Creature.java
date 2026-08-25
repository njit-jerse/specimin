package de.gurkenlabs.litiengine.entities;

import javax.annotation.Nullable;

@MovementInfo
public class Creature {
  @Nullable private Attribute<Float> velocity;

  public Creature(@Nullable String spritesheetName) {
    MovementInfo movementInfo = this.getClass().getAnnotation(MovementInfo.class);
    if (movementInfo != null) {
      this.velocity = new Attribute<>(movementInfo.velocity());
    }
  }
}
