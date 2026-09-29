package org.cloudburstmc.server.diagnostics;

import lombok.experimental.UtilityClass;

import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Comparator;

@UtilityClass
public class ThreadDump {

    public static String capture() {
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        ThreadInfo[] snapshot = threads.dumpAllThreads(threads.isObjectMonitorUsageSupported(), threads.isSynchronizerUsageSupported());
        Arrays.sort(snapshot, Comparator.comparingLong(ThreadInfo::getThreadId));
        StringBuilder dump = new StringBuilder("Platform threads only. Virtual threads are not included.\n");
        for (ThreadInfo thread : snapshot) {
            appendThread(dump, thread);
        }
        return dump.toString();
    }

    private static void appendThread(StringBuilder dump, ThreadInfo thread) {
        dump.append('\n').append('"').append(thread.getThreadName()).append('"');
        if (thread.isDaemon()) {
            dump.append(" daemon");
        }

        dump.append(" prio=").append(thread.getPriority()).append(" Id=").append(thread.getThreadId())
                .append(' ').append(thread.getThreadState());
        if (thread.isSuspended()) {
            dump.append(" (suspended)");
        }

        if (thread.isInNative()) {
            dump.append(" (in native)");
        }

        dump.append('\n');
        if (thread.getLockInfo() != null) {
            dump.append("\tWaiting on: ").append(thread.getLockInfo()).append('\n');
            if (thread.getLockOwnerId() != -1) {
                dump.append("\tOwned by: \"").append(thread.getLockOwnerName()).append("\" Id=")
                        .append(thread.getLockOwnerId()).append('\n');
            }
        }

        StackTraceElement[] stack = thread.getStackTrace();
        MonitorInfo[] monitors = thread.getLockedMonitors();
        for (int index = 0; index < stack.length; index++) {
            dump.append("\tat ").append(stack[index]).append('\n');
            for (MonitorInfo monitor : monitors) {
                if (monitor.getLockedStackDepth() == index) {
                    dump.append("\t- locked ").append(monitor).append('\n');
                }
            }
        }

        for (MonitorInfo monitor : monitors) {
            if (monitor.getLockedStackDepth() < 0) {
                dump.append("\t- locked ").append(monitor).append(" (frame unavailable)\n");
            }
        }

        for (var synchronizer : thread.getLockedSynchronizers()) {
            dump.append("\t- locked synchronizer ").append(synchronizer).append('\n');
        }
    }
}
