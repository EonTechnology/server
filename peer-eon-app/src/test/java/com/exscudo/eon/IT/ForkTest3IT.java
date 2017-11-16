package com.exscudo.eon.IT;

import com.exscudo.peer.core.Constant;

public class ForkTest3IT extends ForkTest1IT {

	protected int DIFF = 180;

	public ForkTest3IT() {
		super();

		BEGIN = Constant.BLOCK_PERIOD * BEGIN_H + DIFF;
		END = Constant.BLOCK_PERIOD * END_H + DIFF;
		END2 = Constant.BLOCK_PERIOD * END_H2 + DIFF;
	}

}
