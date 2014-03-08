/**
 * Copyright 2014 ashigeru.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ashigeru.eclipse.internal.codereading.ui;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Utilities for logging.
 */
public class LogUtil {

    private static final String PLUGIN_ID = Activator.PLUGIN_ID;

    /**
     * Adds a log record.
     * @param severity {@code IStatus}
     * @param message log message
     * @return the log record
     */
    public static IStatus log(int severity, String message) {
        return log(severity, PLUGIN_ID, message);
    }

    /**
     * Adds a log record.
     * @param severity {@code IStatus}
     * @param exception an exception
     * @param message log message
     * @return the log record
     */
    public static IStatus log(int severity, Throwable exception, String message) {
        return log(severity, PLUGIN_ID, message, exception);
    }

    /**
     * Adds a log record.
     * @param status status
     * @return the log record
     */
    public static IStatus log(IStatus status) {
        if (status == null) {
            throw new NullPointerException("status"); //$NON-NLS-1$
        }
        log0(status);
        return status;
    }

    /**
     * Adds a debug log record.
     * @param pattern the message pattern in {@link MessageFormat}
     * @param arguments the message arguments
     */
    public static void debug(String pattern, Object... arguments) {
        if (Activator.getDefault().isDebugging()) {
            log(IStatus.INFO, PLUGIN_ID, MessageFormat.format(pattern, arguments));
        }
    }

    /**
     * Adds a debug log record.
     * @param status status
     */
    public static void debug(IStatus status) {
        if (Activator.getDefault().isDebugging()) {
            log0(status);
        }
    }

    private static IStatus log(int severity, String pluginId, String message) {
        Status status = new Status(severity, pluginId, message);
        log0(status);
        return status;
    }

    private static IStatus log(int severity, String pluginId, String message, Throwable exception) {
        Status status = new Status(
            severity,
            pluginId,
            message == null ? "" : message, //$NON-NLS-1$
            exception);
        log0(status);
        return status;
    }

    private static void log0(IStatus status) {
        Activator.getDefault().getLog().log(status);
    }

    private LogUtil() {
        return;
    }
}
