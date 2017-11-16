package com.exscudo.eon.IT;

import com.exscudo.peer.core.Constant;

public class ForkTest2IT extends ForkTest1IT {

	protected int DIFF = 60;

	public ForkTest2IT() {
		super();

		BEGIN = Constant.BLOCK_PERIOD * BEGIN_H + DIFF;
		END = Constant.BLOCK_PERIOD * END_H + DIFF;
		END2 = Constant.BLOCK_PERIOD * END_H2 + DIFF;
	}

}
