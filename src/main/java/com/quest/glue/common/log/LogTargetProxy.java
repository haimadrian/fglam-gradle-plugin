package com.quest.glue.common.log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.quest.common.log.LogLevel;
import com.quest.common.log.LogMessage;
import com.quest.common.log.LogTarget;

/**
 * Put this in classpath to replace the horrible glue-common implementation, so we can close targets
 * immediately upon registration. We log to console, not to anywhere else. Leaving open streams will
 * fail the build and use handles for no reason.
 *
 * @author Haim Adrian
 * @since 17-Apr-2022
 */
public final class LogTargetProxy extends LogTarget {

  private static final String NAME = "GLUE_PROXY_LOGGER";

  private static class SingletonInstance {

    public static final LogTargetProxy sInstance = new LogTargetProxy();
  }

  private LogTargetProxy() {
    // log nothing until we have loggers added to this class
    // We can't actually use LogLevel.OFF as them log manager removes
    // all loggers with level == OFF and we must never be removed from
    // the manager. This log level will never be seen by the user.

    super(new LogLevel(LogLevel.OFF_LEVEL - 1, "NOTOFF"));

  }

  public static LogTargetProxy getInstance() {
    return SingletonInstance.sInstance;
  }

  public void addLogTarget(LogTarget target) {
    try {
      target.close();
    } catch (Throwable ignore) {
      // IDC
    }
  }

  public void removeLogTarget(LogTarget target) {
  }

  public Collection<LogTarget> getLogTargets() {
    return Collections.unmodifiableCollection(new ArrayList<>());
  }

  @Override
  public void close(long timeout) {
    // Nothing to close
  }

  @Override
  public void open() {
    // Nothing to open
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  protected void logImpl(LogMessage message) {

  }

  @Override
  public void setThreshold(LogLevel threshold) {

  }
}