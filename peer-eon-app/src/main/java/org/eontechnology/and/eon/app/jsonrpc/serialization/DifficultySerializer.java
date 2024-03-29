package org.eontechnology.and.eon.app.jsonrpc.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.eontechnology.and.eon.app.utils.mapper.DifficultyMapper;
import org.eontechnology.and.peer.core.api.Difficulty;

/**
 * JSON custom serialisation of {@code Difficulty}
 *
 * @see Difficulty
 */
public class DifficultySerializer extends StdSerializer<Difficulty> {
  private static final long serialVersionUID = -1290081864260004542L;

  public DifficultySerializer() {
    super(Difficulty.class);
  }

  @Override
  public void serialize(Difficulty value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeObject(DifficultyMapper.convert(value));
  }
}
