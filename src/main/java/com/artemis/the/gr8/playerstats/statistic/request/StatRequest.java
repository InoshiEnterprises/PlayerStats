package com.artemis.the.gr8.playerstats.statistic.request;

import com.artemis.the.gr8.playerstats.api.PlayerStats;
import com.artemis.the.gr8.playerstats.statistic.result.StatResult;
import com.artemis.the.gr8.playerstats.enums.Target;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

/**
 * Holds all the information PlayerStats needs to perform
 * a lookup, and can be executed to get the results. Calling
 * {@link #execute()} on a Top- or ServerRequest can take some
 * time (especially if there is a substantial amount of
 * OfflinePlayers on this particular server), so I strongly
 * advice you to call this asynchronously!
 */
public abstract class StatRequest<T> {

  protected final RequestSettings requestSettings;

  protected StatRequest(RequestSettings request) {
    requestSettings = request;
  }

  /**
   * Executes this StatRequest. For a Top- or ServerRequest, this can
   * take some time!
   *
   * @return a StatResult containing the value of this lookup, both as
   * numerical value and as formatted message
   * @see PlayerStats
   * @see StatResult
   */
  public abstract StatResult<T> execute();

  /**
   * Gets the Statistic that calling {@link #execute()} will calculate
   * the data for.
   * @return the Statistic
   */
  public Statistic getStatisticSetting() {
    return requestSettings.getStatistic();
  }

  /**
   * If the Statistic setting for this StatRequest is of Type.Block,
   * this will get the Material that was set.
   *
   * @return a Material for which #isBlock is true, or null if no
   * Material was set
   */
  public @Nullable Material getBlockSetting() {
    return requestSettings.getBlock();
  }

  /**
   * If the Statistic setting for this StatRequest is of Type.Item,
   * this will get the Material that was set.
   *
   * @return a Material for which #isItem is true, or null if no
   * Material was set
   */
  public @Nullable Material getItemSetting() {
    return requestSettings.getItem();
  }

  /**
   * If the Statistic setting for this StatRequest is of Type.Entity,
   * this will get the EntityType that was set.
   *
   * @return an EntityType, or null if no EntityType was set
   */
  public @Nullable EntityType getEntitySetting() {
    return requestSettings.getEntity();
  }

  /**
   * Gets the Target that will be used when calling {@link #execute()}.
   *
   * @return the Target for this lookup (either Player, Server or Top)
   */
  public Target getTargetSetting() {
    return requestSettings.getTarget();
  }
}