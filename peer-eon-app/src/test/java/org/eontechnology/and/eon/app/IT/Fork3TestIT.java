package org.eontechnology.and.eon.app.IT;

public class Fork3TestIT extends Fork1TestIT {

  protected int DIFF = 180;

  public Fork3TestIT() {
    super();

    BEGIN = 180 * BEGIN_H + DIFF;
    END = 180 * END_H + DIFF;
    END2 = 180 * END_H2 + DIFF;
  }
}
