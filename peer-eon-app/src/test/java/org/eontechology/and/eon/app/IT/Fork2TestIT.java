package org.eontechology.and.eon.app.IT;

import org.eontechology.and.peer.core.Constant;

public class Fork2TestIT extends Fork1TestIT {

    protected int DIFF = 60;

    public Fork2TestIT() {
        super();

        BEGIN = Constant.BLOCK_PERIOD * BEGIN_H + DIFF;
        END = Constant.BLOCK_PERIOD * END_H + DIFF;
        END2 = Constant.BLOCK_PERIOD * END_H2 + DIFF;
    }
}
