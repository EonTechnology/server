package com.exscudo.eon.jsonrpc.serialization;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.exscudo.peer.core.data.Block;
import com.exscudo.peer.core.data.Transaction;
import com.exscudo.peer.core.utils.Format;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * JSON custom deserialization of {@code Block}
 *
 * @see Block
 */
public class BlockDeserializer extends StdDeserializer<Block> {
	private static final long serialVersionUID = 7107449742243526806L;

	public BlockDeserializer() {
		super(Block.class);
	}

	@Override
	public Block deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {

		ObjectMapper mapper = (ObjectMapper) p.getCodec();
		JsonNode node = (JsonNode) mapper.readTree(p);

		try {

			int version = node.get(StringConstant.version).asInt();
			int timestamp = node.get(StringConstant.timestamp).asInt();
			long previousBlock = Format.ID.blockId(node.get(StringConstant.previousBlock).asText());
			long generator = Format.ID.accountId(node.get(StringConstant.generator).asText());
			byte[] generationSignature = Format.convert(node.get(StringConstant.generationSignature).asText());
			byte[] blockSignature = Format.convert(node.get(StringConstant.signature).asText());
			BigInteger cumDif = new BigInteger(node.get(StringConstant.cumulativeDifficulty).asText());

			Block block = new Block();
			block.setVersion(version);
			block.setTimestamp(timestamp);
			block.setPreviousBlock(previousBlock);
			block.setGenerationSignature(generationSignature);
			block.setSenderID(generator);
			block.setSignature(blockSignature);
			block.setCumulativeDifficulty(cumDif);

			JsonNode arrayNode = node.get(StringConstant.transactions);
			if (arrayNode != null) {
				List<Transaction> txMap = new ArrayList<>();
				if (arrayNode.isArray()) {
					for (JsonNode itemNode : arrayNode) {
						Transaction tx = mapper.treeToValue(itemNode, Transaction.class);
						txMap.add(tx);
					}
					block.setTransactions(txMap);
				} else {
					throw new ClassCastException();
				}
			} else {
				block.setTransactions(new ArrayList<>());
			}

			return block;

		} catch (IllegalArgumentException e) {
			throw new IOException(e);
		}

	}
}
