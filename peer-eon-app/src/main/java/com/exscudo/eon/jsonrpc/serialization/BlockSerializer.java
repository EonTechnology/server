package com.exscudo.eon.jsonrpc.serialization;

import java.io.IOException;

import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.utils.Format;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * JSON custom serialisation of {@code Block}
 *
 * @see Block
 */
public class BlockSerializer extends StdSerializer<Block> {
	private static final long serialVersionUID = -1693121409934138590L;

	public BlockSerializer() {
		super(Block.class);
	}

	@Override
	public void serialize(Block value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		gen.writeStartObject();

		gen.writeNumberField(StringConstant.version, value.getVersion());
		gen.writeNumberField(StringConstant.timestamp, value.getTimestamp());
		gen.writeStringField(StringConstant.previousBlock, Format.ID.blockId(value.getPreviousBlock()));
		gen.writeStringField(StringConstant.generator, Format.ID.accountId(value.getSenderID()));
		gen.writeStringField(StringConstant.generationSignature, Format.convert(value.getGenerationSignature()));
		gen.writeStringField(StringConstant.signature, Format.convert(value.getSignature()));
		gen.writeStringField(StringConstant.id, Format.ID.blockId(value.getID()));
		gen.writeNumberField(StringConstant.height, value.getHeight());
		gen.writeStringField(StringConstant.nextBlock, Format.ID.blockId(value.getNextBlock()));
		gen.writeStringField(StringConstant.cumulativeDifficulty, value.getCumulativeDifficulty().toString());

		Transaction[] transactions = value.getTransactions().toArray(new Transaction[0]);
		if (transactions.length != 0) {
			gen.writeArrayFieldStart(StringConstant.transactions);
			for (Transaction tx : transactions) {
				gen.writeObject(tx);
			}
			gen.writeEndArray();
		}

		if (value.getVersion() >= 2) {
			gen.writeStringField(StringConstant.snapshot, Format.convert(value.getSnapshot()));
		}

		gen.writeEndObject();
	}
}
