package org.eontechnology.and.eon.app.utils.mapper;

import java.math.BigInteger;
import java.util.Map;
import java.util.TreeMap;
import org.eontechnology.and.peer.core.api.Difficulty;
import org.eontechnology.and.peer.core.data.identifier.BlockID;

public class DifficultyMapper {

  public static Map<String, Object> convert(Difficulty difficulty) {
    Map<String, Object> map = new TreeMap<>();
    map.put(Constants.CUMMULATIVE_DIFFICULTY, difficulty.getDifficulty().toString());
    map.put(Constants.LAST_BLOCK_ID, difficulty.getLastBlockID().toString());
    return map;
  }

  public static Difficulty convert(Map<String, Object> map) {

    BlockID lastBlockID = new BlockID(String.valueOf(map.get(Constants.LAST_BLOCK_ID)));
    BigInteger difficulty =
        new BigInteger(String.valueOf(map.get(Constants.CUMMULATIVE_DIFFICULTY)));

    return new Difficulty(lastBlockID, difficulty);
  }
}
