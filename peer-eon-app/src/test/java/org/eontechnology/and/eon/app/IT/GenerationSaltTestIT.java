package org.eontechnology.and.eon.app.IT;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.eontechnology.and.eon.app.cfg.PeerStarter;
import org.eontechnology.and.peer.core.Constant;
import org.eontechnology.and.peer.core.common.Format;
import org.eontechnology.and.peer.core.common.TimeProvider;
import org.eontechnology.and.peer.core.data.identifier.BlockID;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class GenerationSaltTestIT {
  protected static String[] GENERATORS = {
    "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e",
    "d2005ef0df1f6926082aefa09917874cfb212d1ff4eb55c78f670ef9dd23ef6c",
    "391b34d7f878c7f327fd244370edb9d521472e36816a36299341d0220662e0c2",
    "2806d51f6bcf1054a4ab484e3e3a30c33c441c9d9141f31b44e60cb798e9fa7d",
    "ec941f556e5bfc5e803140e496a1a972a9764a64d8ede972993e4f6f818f1210"
  };

  @Test
  public void test() throws Exception {
    TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
    long timestamp = Utils.getGenesisBlockTimestamp();
    Mockito.when(timeProvider.get()).thenReturn((int) timestamp);

    PeerStarter[] starters = new PeerStarter[GENERATORS.length];
    PeerContext[] ctxs = new PeerContext[GENERATORS.length];

    for (int i = 0; i < GENERATORS.length; i++) {
      starters[i] =
          PeerStarterFactory.create()
              .beginFork(getTimeString(timestamp))
              .add(getTimeString(timestamp + 2 * Constant.SECONDS_IN_DAY), 3600, 0)
              .add(getTimeString(timestamp + 3 * Constant.SECONDS_IN_DAY), 900, 0)
              .add(getTimeString(timestamp + 4 * Constant.SECONDS_IN_DAY), 900, 1)
              .add(getTimeString(timestamp + 5 * Constant.SECONDS_IN_DAY), 900, 2)
              .buildFork()
              .seed(GENERATORS[i])
              .build(timeProvider);
      ctxs[i] = new PeerContext(starters[i]);
    }

    for (int i = 1; i <= 5 * 24 * 60; i++) {
      Mockito.when(timeProvider.get()).thenReturn((int) (timestamp + i * 60) + 1);

      Set<BlockID> ids = new HashSet<>();
      for (int j = 0; j < GENERATORS.length; j++) {
        ctxs[j].generateBlockForNow();
        ids.add(ctxs[j].blockchain.getLastBlock().getID());
      }

      if (ids.size() > 1) {
        for (int j = 0; j < GENERATORS.length; j++) {
          for (int k = 0; k < GENERATORS.length; k++) {
            if (j != k) {
              ctxs[j].setPeerToConnect(ctxs[k]);
              ctxs[j].fullBlockSync();
            }
          }
        }
      }
    }

    // Check generation end on unknown salt version
    int targetHeight = 2 * Constant.SECONDS_IN_DAY / 3600 + 2 * Constant.SECONDS_IN_DAY / 900;

    Assert.assertNotNull(ctxs[0].blockExplorerService.getByHeight(targetHeight));
    Assert.assertNull(ctxs[0].blockExplorerService.getByHeight(targetHeight + 1));

    // Check generation signature without salt and with salt
    int changeHeight = 2 * Constant.SECONDS_IN_DAY / 3600 + Constant.SECONDS_IN_DAY / 900;
    Assert.assertEquals(
        Format.convert(
            ctxs[0].blockExplorerService.getByHeight(changeHeight - 1).getGenerationSignature()),
        Format.convert(
            ctxs[0].blockExplorerService.getByHeight(changeHeight - 2).getGenerationSignature()));
    Assert.assertEquals(
        Format.convert(
            ctxs[0].blockExplorerService.getByHeight(changeHeight - 1).getGenerationSignature()),
        Format.convert(
            ctxs[0].blockExplorerService.getByHeight(changeHeight - 3).getGenerationSignature()));
    Assert.assertEquals(
        Format.convert(
            ctxs[0].blockExplorerService.getByHeight(changeHeight - 1).getGenerationSignature()),
        Format.convert(
            ctxs[0].blockExplorerService.getByHeight(changeHeight - 4).getGenerationSignature()));

    Assert.assertNotEquals(
        Format.convert(
            ctxs[0].blockExplorerService.getByHeight(changeHeight).getGenerationSignature()),
        Format.convert(
            ctxs[0].blockExplorerService.getByHeight(changeHeight + 1).getGenerationSignature()));
    Assert.assertNotEquals(
        Format.convert(
            ctxs[0].blockExplorerService.getByHeight(changeHeight).getGenerationSignature()),
        Format.convert(
            ctxs[0].blockExplorerService.getByHeight(changeHeight + 2).getGenerationSignature()));
    Assert.assertNotEquals(
        Format.convert(
            ctxs[0].blockExplorerService.getByHeight(changeHeight).getGenerationSignature()),
        Format.convert(
            ctxs[0].blockExplorerService.getByHeight(changeHeight + 3).getGenerationSignature()));
  }

  private String getTimeString(long timestamp) {
    return Instant.ofEpochSecond(timestamp).toString();
  }
}
