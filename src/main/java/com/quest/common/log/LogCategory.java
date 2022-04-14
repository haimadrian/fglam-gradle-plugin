package com.quest.common.log;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

/**
 * Put this in classpath to replace the horrible forge-common implementation, so we can redirect all
 * logs to console, and see what AgentCompiler is talking about...
 *
 * @author Haim Adrian
 * @since 14-Apr-2022
 */
public class LogCategory {

  private final String mName;

  public static LogCategory getInstance(Class clazz) {
    return new LogCategory(clazz);
  }

  public static LogCategory getInstance(Class clazz, LogOncePolicy policy) {
    return new LogCategory(clazz);
  }

  public static synchronized LogCategory getInstance(String name) {
    return new LogCategory(name);
  }

  public static String getErrorUnexpectedPrefix() {
    return "UnexpectedError: ";
  }

  protected LogCategory(String name) {
    mName = name;
  }

  protected LogCategory(Class clazz) {
    this(clazz.getSimpleName());
  }

  public String getName() {
    return mName;
  }

  public boolean isDebugEnabled() {
    return false;
  }

  public int getDebugLevel() {
    return 1;
  }

  public int getDebugLevel(String source) {
    return getDebugLevel();
  }

  public void debug(String message) {
    logRaw(LogLevel.DEBUG, message, null);
  }

  public void debug(String message, String source) {
    logRaw(LogLevel.DEBUG, message, null, false, source);
  }

  public void debug(String message, Throwable throwable) {
    logRaw(LogLevel.DEBUG, message, throwable);
  }

  public void debug(String message, Throwable throwable, String source) {
    logRaw(LogLevel.DEBUG, message, throwable, false, source);
  }

  public void debug2(String message) {
    logRaw(LogLevel.DEBUG2, message, null);
  }

  public void debug2(String message, Throwable throwable) {
    logRaw(LogLevel.DEBUG2, message, throwable);
  }

  public void debug2(String message, String source) {
    logRaw(LogLevel.DEBUG2, message, null, false, source);
  }

  public void debug2(String message, Throwable throwable, String source) {
    logRaw(LogLevel.DEBUG2, message, throwable, false, source);
  }

  public void errorUnexpected(String message) {
    //noinspection ThrowableInstanceNeverThrown
    logRaw(LogLevel.ERROR, getErrorUnexpectedPrefix() + message,
        new Exception("Stack Trace"), true, null);
  }

  public void errorUnexpected(String message, String source) {
    //noinspection ThrowableInstanceNeverThrown
    logRaw(LogLevel.ERROR, getErrorUnexpectedPrefix() + message,
        new Exception("Stack Trace"), true, source);
  }

  public void errorUnexpected(String message, Throwable throwable) {
    logRaw(LogLevel.ERROR, getErrorUnexpectedPrefix() + message, throwable,
        true, null);
  }

  public void errorUnexpected(String message, Throwable throwable, String source) {
    logRaw(LogLevel.ERROR, getErrorUnexpectedPrefix() + message, throwable,
        true, source);
  }

  public void ignoreException(String message, Throwable throwable) {
    ignoreException(message, throwable, null);
  }

  public void ignoreException(String message, Throwable throwable, String source) {
    switch (getDebugLevel()) {
      case 0:
        // In non-debug mode, do nothing at all
        break;
      case 1:
        // In normal debug mode, just log the message -- don't fill up
        // the logs with ignorable stack traces
        logRaw(LogLevel.DEBUG,
            "Caught unimportant exception; safe to ignore: " + message,
            null, false, source);
        break;
      default:
        // In higher than normal debug mode, log the message with the
        // stack trace
        logRaw(LogLevel.DEBUG2,
            "Caught unimportant exception; safe to ignore: " + message,
            throwable, false, source);
        break;
    }
  }

  public void log(String id) {
    log(id, null, null, false, null);
  }

  public void logOnce(String id) {
    log(id, null, null, true, null);
  }

  public void log(String id, Throwable throwable) {
    log(id, null, throwable, false, null);
  }

  public void log(String id, Throwable throwable, String source) {
    log(id, null, throwable, false, source);
  }

  public void logOnce(String id, Throwable throwable) {
    log(id, null, throwable, true, null);
  }

  public void log(String id, String param0) {
    log(id, new Object[]{param0}, null, false, null);
  }

  public void logOnce(String id, String param0) {
    log(id, new Object[]{param0}, null, true, null);
  }

  public void log(String id, String param0, Throwable throwable) {
    log(id, new Object[]{param0}, throwable, false, null);
  }

  public void log(String id, String param0, Throwable throwable,
      String source) {
    log(id, new Object[]{param0}, throwable, false, source);
  }

  public void logOnce(String id, String param0, Throwable throwable) {
    log(id, new Object[]{param0}, throwable, true, null);
  }

  public void log(String id, Object[] params) {
    log(id, params, null, false, null);
  }

  public void log(String id, Object[] params, String source) {
    log(id, params, null, false, source);
  }

  public void logOnce(String id, Object[] params) {
    log(id, params, null, true, null);
  }

  public void log(String id, Object[] params, Throwable throwable) {
    log(id, params, throwable, false, null);
  }

  public void log(String id, Object[] params, Throwable throwable, String source) {
    log(id, params, throwable, false, source);
  }

  public void logOnce(String id, Object[] params, Throwable throwable) {
    log(id, params, throwable, true, null);
  }

  public void logOnce(String id, Object[] params, Throwable throwable, String source) {
    log(id, params, throwable, true, source);
  }

  public void logOnce(String id, Object[] params, String source) {
    log(id, params, null, true, source);
  }

  public void log(String id, Object[] params, Throwable throwable, boolean logOnce) {
    log(id, params, throwable, logOnce, null);
  }

  public void log(String id, Object[] params, Throwable throwable, boolean logOnce, String source) {
    final LogManager manager = LogManager.getInstance();
    final long timestamp = manager.getTimestamp();
    final LogMessage message = getMessage(logOnce, timestamp, id, params,
        throwable, source);

    logRaw(message);
  }

  public void log(String id, LogLevel level, Object[] params, Throwable throwable,
      boolean logOnce, String source, boolean fallbackToFormat) {

    final LogManager manager = LogManager.getInstance();
    final long timestamp = manager.getTimestamp();

    final LogMessage message = getMessage(logOnce,
        timestamp,
        id,
        level, // nullable
        params,
        throwable,
        source,
        fallbackToFormat // allow using the format key as the format string
    );

    logRaw(message);
  }

  public void verbose(String format) {
    verbose(format, null, null, null);
  }

  public void verbose(String format, Throwable t) {
    verbose(format, null, null, t);
  }

  public void verbose(String format, Object[] args) {
    verbose(format, args, null, null);
  }

  public void verbose(String format, Object[] args, String source) {
    verbose(format, args, source, null);
  }

  public void verbose(String format, Object[] args, Throwable t) {
    verbose(format, args, null, t);
  }

  public void verbose(String format, Object[] args, String source, Throwable t) {
    log(format, LogLevel.VERBOSE, args, t, false, source, true);
  }

  public void verboseOnce(String format) {
    verboseOnce(format, null, null, null);
  }

  public void verboseOnce(String format, Throwable t) {
    verboseOnce(format, null, null, t);
  }

  public void verboseOnce(String format, Object[] args) {
    verboseOnce(format, args, null, null);
  }

  public void verboseOnce(String format, Object[] args, String source) {
    verboseOnce(format, args, source, null);
  }

  public void verboseOnce(String format, Object[] args, Throwable t) {
    verboseOnce(format, args, null, t);
  }

  public void verboseOnce(String format, Object[] args, String source, Throwable t) {
    log(format, LogLevel.VERBOSE, args, t, true, source, true);
  }

  public void info(String format) {
    info(format, null, null, null);
  }

  public void info(String format, Throwable t) {
    info(format, null, null, t);
  }

  public void info(String format, Object[] args) {
    info(format, args, null, null);
  }

  public void info(String format, Object[] args, String source) {
    info(format, args, source, null);
  }

  public void info(String format, Object[] args, Throwable t) {
    info(format, args, null, t);
  }

  public void info(String format, Object[] args, String source, Throwable t) {
    log(format, LogLevel.INFO, args, t, false, source, true);
  }

  public void infoOnce(String format) {
    infoOnce(format, null, null, null);
  }

  public void infoOnce(String format, Throwable t) {
    infoOnce(format, null, null, t);
  }

  public void infoOnce(String format, Object[] args) {
    infoOnce(format, args, null, null);
  }

  public void infoOnce(String format, Object[] args, String source) {
    infoOnce(format, args, source, null);
  }

  public void infoOnce(String format, Object[] args, Throwable t) {
    infoOnce(format, args, null, t);
  }

  public void infoOnce(String format, Object[] args, String source, Throwable t) {
    log(format, LogLevel.INFO, args, t, true, source, true);
  }

  public void warn(String format) {
    warn(format, null, null, null);
  }

  public void warn(String format, Throwable t) {
    warn(format, null, null, t);
  }

  public void warn(String format, Object[] args) {
    warn(format, args, null, null);
  }

  public void warn(String format, Object[] args, String source) {
    warn(format, args, source, null);
  }

  public void warn(String format, Object[] args, Throwable t) {
    warn(format, args, null, t);
  }

  public void warn(String format, Object[] args, String source, Throwable t) {
    log(format, LogLevel.WARN, args, t, false, source, true);
  }

  public void warnOnce(String format) {
    warnOnce(format, null, null, null);
  }

  public void warnOnce(String format, Throwable t) {
    warnOnce(format, null, null, t);
  }

  public void warnOnce(String format, Object[] args) {
    warnOnce(format, args, null, null);
  }

  public void warnOnce(String format, Object[] args, String source) {
    warnOnce(format, args, source, null);
  }

  public void warnOnce(String format, Object[] args, Throwable t) {
    warnOnce(format, args, null, t);
  }

  public void warnOnce(String format, Object[] args, String source, Throwable t) {
    log(format, LogLevel.WARN, args, t, true, source, true);
  }

  public void error(String format) {
    error(format, null, null, null);
  }

  public void error(String format, Throwable t) {
    error(format, null, null, t);
  }

  public void error(String format, Object[] args) {
    error(format, args, null, null);
  }

  public void error(String format, Object[] args, String source) {
    error(format, args, source, null);
  }

  public void error(String format, Object[] args, Throwable t) {
    error(format, args, null, t);
  }

  public void error(String format, Object[] args, String source, Throwable t) {
    log(format, LogLevel.ERROR, args, t, false, source, true);
  }

  public void errorOnce(String format) {
    errorOnce(format, null, null, null);
  }

  public void errorOnce(String format, Throwable t) {
    errorOnce(format, null, null, t);
  }

  public void errorOnce(String format, Object[] args) {
    errorOnce(format, args, null, null);
  }

  public void errorOnce(String format, Object[] args, String source) {
    errorOnce(format, args, source, null);
  }

  public void errorOnce(String format, Object[] args, Throwable t) {
    errorOnce(format, args, null, t);
  }

  public void errorOnce(String format, Object[] args, String source, Throwable t) {
    log(format, LogLevel.ERROR, args, t, true, source, true);
  }

  public void fatal(String format) {
    fatal(format, null, null, null);
  }

  public void fatal(String format, Throwable t) {
    fatal(format, null, null, t);
  }

  public void fatal(String format, Object[] args) {
    fatal(format, args, null, null);
  }

  public void fatal(String format, Object[] args, String source) {
    fatal(format, args, source, null);
  }

  public void fatal(String format, Object[] args, Throwable t) {
    fatal(format, args, null, t);
  }

  public void fatal(String format, Object[] args, String source, Throwable t) {
    log(format, LogLevel.FATAL, args, t, false, source, true);
  }

  public void fatalOnce(String format) {
    fatalOnce(format, null, null, null);
  }

  public void fatalOnce(String format, Throwable t) {
    fatalOnce(format, null, null, t);
  }

  public void fatalOnce(String format, Object[] args) {
    fatalOnce(format, args, null, null);
  }

  public void fatalOnce(String format, Object[] args, String source) {
    fatalOnce(format, args, source, null);
  }

  public void fatalOnce(String format, Object[] args, Throwable t) {
    fatalOnce(format, args, null, t);
  }

  public void fatalOnce(String format, Object[] args, String source, Throwable t) {
    log(format, LogLevel.FATAL, args, t, true, source, true);
  }

  public void logRaw(LogMessage message) {
    logRaw(message.getLevel(), message.getMessage(), message.getThrowable());
  }

  protected void logRaw(LogLevel level, String message, Throwable throwable) {
    logRaw(level, message, throwable, false, null);
  }

  protected void logRaw(LogLevel level, String message, Throwable throwable, boolean logOnce,
      String source) {
    if (level.getLevel() >= LogLevel.INFO_LEVEL) {
      String msg = message;
      if (throwable != null) {
        msg += System.lineSeparator() + getStackTrace(throwable);
      }

      System.out.println(mName + ": " + msg);
    }
  }

  private static String getStackTrace(Throwable throwable) {
    try (StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer)) {
      throwable.printStackTrace(printWriter);
      return writer.toString();
    } catch (IOException e) {
      return "Failed to get stack trace of: " + throwable + ". Reason: " + e;
    }
  }

  public void config(String id, Object[] params, Map config) {
    config(id, params, config, null);
  }

  public void config(String id, Object[] params, Map config, String source) {
    final String threadName = Thread.currentThread().getName();

    final ConfigLogMessage message = new ConfigLogMessage(System.currentTimeMillis(), mName,
        threadName, id, params, config, source);

    logRaw(message.getLevel(), message.getMessage(), message.getThrowable());
  }

  public void config(String id, Map config) {
    config(id, null, config);
  }

  public void config(String id, Map config, String source) {
    config(id, null, config, source);
  }

  public LogMessage getMessage(String id, Object[] params, Throwable throwable) {
    return getMessage(false,
        LogManager.getInstance().getTimestamp(),
        id,
        params,
        throwable,
        null);
  }

  // Only use by the unit tests to flush the current cached value.
  /*pkg*/ void clearCachedLevels() {
  }

  public LogMessage getMessage(boolean logOnce,
      long timestamp,
      String id,
      Object[] params,
      Throwable throwable,
      String source) {

    return getMessage(logOnce,
        timestamp,
        id,
        null, // legacy default
        params,
        throwable,
        source,
        false // legacy default
    );
  }

  public LogMessage getMessage(boolean logOnce,
      long timestamp,
      String id,
      LogLevel level, // nullable
      Object[] params,
      Throwable throwable,
      String source,  // nullable
      boolean fallbackToFormat
  ) {

    final String threadName = Thread.currentThread().getName();

    // Otherwise create the log message normally.
    return new LocalizableLogMessage(timestamp,
        mName,
        threadName,
        level,
        id,
        params,
        throwable,
        source);
  }

}