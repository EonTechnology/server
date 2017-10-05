package com.exscudo.eon.IT;

public class ForkTest3IT extends ForkTest1IT {

	protected int DIFF = -60;

	public ForkTest3IT() {
		super();

		BEGIN = BEGIN + DIFF;
		END = END + DIFF;
		END2 = END2 + DIFF;
	}

}
