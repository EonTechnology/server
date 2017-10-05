package com.exscudo.peer.eon;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.exceptions.ValidateException;
import com.exscudo.peer.core.utils.Format;

/**
 * Implementation of the algorithm for calculating "cumulative difficulty".
 * <p>
 * 
 * The "cumulative difficulty" is used as a criterion on the basis of which the
 * chain matching occurs.
 */
public class DifficultyHelper {

	public static BigInteger calculateDifficulty(Block block, Block prevBlock, long generatingBalance)
			throws ValidateException {

		if (generatingBalance < EonConstant.MIN_DEPOSIT_SIZE) {
			throw new ValidateException("Too small deposit.");
		}

		byte[] generationSignatureHash;
		try {
			generationSignatureHash = MessageDigest.getInstance("SHA-512").digest(block.getGenerationSignature());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		BigInteger hit = new BigInteger(1,
				new byte[]{generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5],
						generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2],
						generationSignatureHash[1], generationSignatureHash[0]});

		Long scale = generatingBalance / EonConstant.DECIMAL_POINT;
		if (scale != 0) {
			hit = hit.divide(BigInteger.valueOf(scale));
		}
		BigInteger value = Format.two64.divide(hit);

		BigInteger cumulativeDifficulty = prevBlock.getCumulativeDifficulty().add(value);
		return cumulativeDifficulty;
	}

}
