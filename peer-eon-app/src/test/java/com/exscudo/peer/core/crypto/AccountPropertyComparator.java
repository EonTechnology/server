package com.exscudo.peer.core.crypto;

import java.io.IOException;
import java.util.Comparator;

import com.exscudo.peer.core.services.AccountProperty;

public class AccountPropertyComparator implements Comparator<AccountProperty> {

	public int compare(AccountProperty a, AccountProperty b) {

		int res = a.getType().compareTo(b.getType());
		if (res < 0) {

			return -1;

		} else if (res > 0) {

			return 1;

		} else {

			long account = a.getAccountID();
			long oaccount = b.getAccountID();

			if (account < oaccount) {

				return -1;

			} else if (account > oaccount) {

				return 1;

			} else {

				try {
					final byte[] data = BencodeMessage.getBytes(AccountPropertyMapper.convert(a));
					final byte[] odata = BencodeMessage.getBytes(AccountPropertyMapper.convert(b));

					int length = Math.min(data.length, odata.length);
					for (int i = 0; i < length; i++) {

						if (data[i] < odata[i]) {
							return -1;
						} else if (data[i] > odata[i]) {
							return 1;
						}

					}
					return 0;
				} catch (IOException e) {
					throw new RuntimeException(e);
				}

			}
		}

	}
}
