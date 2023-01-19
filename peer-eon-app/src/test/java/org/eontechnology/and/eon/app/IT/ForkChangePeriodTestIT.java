package org.eontechnology.and.eon.app.IT;

import java.time.Instant;
import org.eontechnology.and.eon.app.cfg.PeerStarter;
import org.eontechnology.and.peer.core.Constant;
import org.eontechnology.and.peer.core.common.Format;
import org.eontechnology.and.peer.core.common.TimeProvider;
import org.eontechnology.and.peer.core.data.Block;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class ForkChangePeriodTestIT {
  protected static String GENERATOR1 =
      "eba54bbb2dd6e55c466fac09707425145ca8560fe40de3fa3565883f4d48779e";
  protected static String GENERATOR2 =
      "d2005ef0df1f6926082aefa09917874cfb212d1ff4eb55c78f670ef9dd23ef6c";

  @Test
  public void test() throws Exception {
    TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
    long timestamp = Utils.getGenesisBlockTimestamp();
    Mockito.when(timeProvider.get()).thenReturn((int) timestamp);

    PeerStarter starter1 =
        PeerStarterFactory.create()
            .beginFork(getTimeString(timestamp))
            .add(getTimeString(timestamp + Constant.SECONDS_IN_DAY), 3600, 0)
            .add(getTimeString(timestamp + 2 * Constant.SECONDS_IN_DAY), 1800, 0)
            .add(getTimeString(timestamp + 3 * Constant.SECONDS_IN_DAY), 900, 0)
            .buildFork()
            .seed(GENERATOR1)
            .build(timeProvider);
    PeerContext ctx1 = new PeerContext(starter1);

    PeerStarter starter2 =
        PeerStarterFactory.create()
            .beginFork(getTimeString(timestamp))
            .add(getTimeString(timestamp + Constant.SECONDS_IN_DAY), 3600, 0)
            .add(getTimeString(timestamp + 2 * Constant.SECONDS_IN_DAY), 1800, 0)
            .add(getTimeString(timestamp + 3 * Constant.SECONDS_IN_DAY), 900, 0)
            .buildFork()
            .seed(GENERATOR2)
            .build(timeProvider);
    PeerContext ctx2 = new PeerContext(starter2);

    ctx1.setPeerToConnect(ctx2);
    ctx2.setPeerToConnect(ctx1);

    for (int i = 1; i <= 3 * 24 * 60; i++) {
      Mockito.when(timeProvider.get()).thenReturn((int) (timestamp + i * 60) + 1);
      ctx1.generateBlockForNow();
      ctx2.generateBlockForNow();

      ctx1.fullBlockSync();
      ctx2.fullBlockSync();

      Assert.assertEquals(
          starter1.getFork().getTargetBlockHeight(timeProvider.get()),
          ctx1.blockExplorerService.getLastBlock().getHeight());
    }

    int targetHeight =
        Constant.SECONDS_IN_DAY / 3600
            + Constant.SECONDS_IN_DAY / 1800
            + Constant.SECONDS_IN_DAY / 900;

    Assert.assertEquals(targetHeight, ctx1.blockExplorerService.getLastBlock().getHeight());
    Assert.assertEquals(
        ctx1.blockExplorerService.getLastBlock().getID(),
        ctx2.blockExplorerService.getLastBlock().getID());
  }

  @Test
  public void testGenerationSignature() throws Exception {
    TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
    long timestamp = Utils.getGenesisBlockTimestamp();
    Mockito.when(timeProvider.get()).thenReturn((int) timestamp);

    PeerStarter starter1 =
        PeerStarterFactory.create()
            .beginFork(getTimeString(timestamp))
            .add(getTimeString(timestamp + 2 * Constant.SECONDS_IN_DAY), 3600, 0)
            .add(getTimeString(timestamp + 3 * Constant.SECONDS_IN_DAY), 1800, 0)
            .add(getTimeString(timestamp + 4 * Constant.SECONDS_IN_DAY), 900, 0)
            .buildFork()
            .seed(GENERATOR1)
            .build(timeProvider);
    PeerContext ctx1 = new PeerContext(starter1);

    Mockito.when(timeProvider.get())
        .thenReturn((int) (timestamp + 4 * Constant.SECONDS_IN_DAY + 1));
    ctx1.generateBlockForNow();

    Assert.assertEquals(
        starter1.getFork().getTargetBlockHeight(timeProvider.get()),
        ctx1.blockExplorerService.getLastBlock().getHeight());

    for (int i = 0; i < Constant.SECONDS_IN_DAY / 3600; i++) {
      Block b0 =
          ctx1.blockExplorerService.getByHeight(Constant.SECONDS_IN_DAY / 3600 * 2 + 2 * i + 0);
      Block b1 =
          ctx1.blockExplorerService.getByHeight(Constant.SECONDS_IN_DAY / 3600 * 2 + 2 * i + 1);

      Assert.assertEquals(
          Format.convert(b0.getGenerationSignature()), Format.convert(b1.getGenerationSignature()));
    }

    for (int i = 0; i < Constant.SECONDS_IN_DAY / 3600; i++) {
      Block b0 =
          ctx1.blockExplorerService.getByHeight(Constant.SECONDS_IN_DAY / 3600 * 4 + 4 * i + 0);
      Block b1 =
          ctx1.blockExplorerService.getByHeight(Constant.SECONDS_IN_DAY / 3600 * 4 + 4 * i + 1);
      Block b2 =
          ctx1.blockExplorerService.getByHeight(Constant.SECONDS_IN_DAY / 3600 * 4 + 4 * i + 2);
      Block b3 =
          ctx1.blockExplorerService.getByHeight(Constant.SECONDS_IN_DAY / 3600 * 4 + 4 * i + 3);

      Assert.assertEquals(
          Format.convert(b0.getGenerationSignature()), Format.convert(b1.getGenerationSignature()));
      Assert.assertEquals(
          Format.convert(b0.getGenerationSignature()), Format.convert(b2.getGenerationSignature()));
      Assert.assertEquals(
          Format.convert(b0.getGenerationSignature()), Format.convert(b3.getGenerationSignature()));
    }
  }

  private String getTimeString(long timestamp) {
    return Instant.ofEpochSecond(timestamp).toString();
  }
}
