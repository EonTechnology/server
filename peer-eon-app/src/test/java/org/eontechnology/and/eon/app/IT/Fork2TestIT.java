package org.eontechnology.and.eon.app.IT;

public class Fork2TestIT extends Fork1TestIT {

  protected int DIFF = 60;

  public Fork2TestIT() {
    super();

    BEGIN = 180 * BEGIN_H + DIFF;
    END = 180 * END_H + DIFF;
    END2 = 180 * END_H2 + DIFF;
  }
}
