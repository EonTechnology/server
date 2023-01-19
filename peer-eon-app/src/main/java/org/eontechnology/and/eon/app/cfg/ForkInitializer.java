package org.eontechnology.and.eon.app.cfg;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.eontechnology.and.eon.app.cfg.forks.ForkItem;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.eontechnology.and.peer.core.middleware.ITransactionParser;
import org.eontechnology.and.peer.tx.TransactionType;

/** Initializing the current fork */
public class ForkInitializer {

  public static Fork init(BlockID networkID, ForkProperties props, int genesisTimestamp)
      throws IOException {

    long minDepositSize = props.getMinDepositSize();

    ForkProperties.Period[] periodSet = props.getPeriods();
    ForkItem[] itemSet = new ForkItem[periodSet.length];

    for (int i = 0; i < periodSet.length; i++) {
      ForkProperties.Period period = periodSet[i];
      ForkItem item = new ForkItem(period.getNumber(), period.getDateEnd());

      for (int k = 0; k <= i; k++) {
        ForkProperties.Period p = periodSet[k];

        if (p.getRemovedTxTypes() != null) {
          for (String t : p.getRemovedTxTypes()) {
            Integer type = TransactionType.getType(t);
            Objects.requireNonNull(type, "Unknown type: " + t);

            item.removeTxType(type);
          }
        }

        if (p.getAddedTxTypes() != null) {
          for (Map.Entry<String, String> entry : p.getAddedTxTypes().entrySet()) {
            Integer type = TransactionType.getType(entry.getKey());
            Objects.requireNonNull(type, "Unknown type: " + entry.getKey());

            try {

              Class<?> clazz = Class.forName(entry.getValue());
              if (!ITransactionParser.class.isAssignableFrom(clazz)) {
                throw new ClassCastException();
              }
              Constructor<?> ctor = clazz.getConstructor();
              Object obj = ctor.newInstance();
              item.addTxType(type, (ITransactionParser) obj);
            } catch (Exception e) {
              throw new IOException(e);
            }
          }
        }

        List<String> rules = item.getValidationRules();
        if (rules == null) {
          rules = new LinkedList<>();
        }

        if (p.getRemovedRules() != null) {
          for (String r : p.getRemovedRules()) {
            if (!rules.remove(r)) {
              throw new IllegalArgumentException();
            }
          }
        }

        if (p.getAddedRules() != null) {
          for (String r : p.getAddedRules()) {
            if (rules.contains(r)) {
              throw new IllegalArgumentException();
            }
            rules.add(r);
          }
        }
        item.setValidationRules(rules);

        if (p.getParams() != null) {
          if (p.getParams().containsKey("block_period")) {
            item.setBlockPeriod(p.getParams().get("block_period").intValue());
          }
          if (p.getParams().containsKey("block_size")) {
            item.setBlockSize(p.getParams().get("block_size").longValue());
          }
          if (p.getParams().containsKey("generation_salt_version")) {
            item.setGenerationSaltVersion(p.getParams().get("generation_salt_version").intValue());
          }
        }
      }

      if (item.getBlockPeriod() == 0 || item.getBlockSize() == 0) {
        throw new IOException("Illegal fork - empty block period or block size");
      }

      itemSet[i] = item;
    }

    Fork fork = new Fork(networkID, itemSet, genesisTimestamp * 1000L);

    fork.setMinDepositSize(minDepositSize);

    // Check fork timestamps
    fork.getTargetBlockHeight(Integer.MAX_VALUE);

    return fork;
  }
}
