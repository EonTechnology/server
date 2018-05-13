package com.exscudo.eon.app.IT;

import com.exscudo.peer.core.Constant;

public class Fork3TestIT extends Fork1TestIT {

    protected int DIFF = 180;

    public Fork3TestIT() {
        super();

        BEGIN = Constant.BLOCK_PERIOD * BEGIN_H + DIFF;
        END = Constant.BLOCK_PERIOD * END_H + DIFF;
        END2 = Constant.BLOCK_PERIOD * END_H2 + DIFF;
    }
}
