package org.eontechnology.and.eon.app.jsonrpc.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.eontechnology.and.eon.app.utils.mapper.TransportBlockMapper;
import org.eontechnology.and.peer.core.data.Block;

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
  public void serialize(Block value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeObject(TransportBlockMapper.convert(value));
  }
}
