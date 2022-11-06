/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import android.util.Log;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.telegram.messenger.time.FastDateFormat;
import org.telegram.messenger.video.MediaCodecVideoConvertor;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.LaunchActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Locale;

public class FileLog {
    private OutputStreamWriter streamWriter = null;
    private FastDateFormat dateFormat = null;
    private DispatchQueue logQueue = null;

    private File currentFile = null;
    private File networkFile = null;
    private File tonlibFile = null;
    private boolean initied;

    private OutputStreamWriter tlStreamWriter = null;
    private File tlRequestsFile = null;

    private final static String tag = "tmessages";
    private final static String mtproto_tag = "MTProto";

    private static volatile FileLog Instance = null;
    public static FileLog getInstance() {
        FileLog localInstance = Instance;
        if (localInstance == null) {
            synchronized (FileLog.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new FileLog();
                }
            }
        }
        return localInstance;
    }

    public FileLog() {
        if (!BuildVars.LOGS_ENABLED) {
            return;
        }
        init();
    }


    private static Gson gson;
    private static HashSet<String> excludeRequests;

    public static void dumpResponseAndRequest(TLObject request, TLObject response, TLRPC.TL_error error, long requestMsgId, long startRequestTimeInMillis, int requestToken) {
        if (!BuildVars.DEBUG_PRIVATE_VERSION || !BuildVars.LOGS_ENABLED || request == null) {
            return;
        }
        String requestSimpleName = request.getClass().getSimpleName();
        checkGson();

        if (excludeRequests.contains(requestSimpleName)) {
            return;
        }
        try {
            String req = "req -> " + requestSimpleName + " : " + gson.toJson(request);
            String res = "null";
            if (response != null) {
                res = "res -> " + response.getClass().getSimpleName() + " : " + gson.toJson(response);
            } else if (error != null) {
                res = "err -> " + error.getClass().getSimpleName() + " : " + gson.toJson(error);
            }
            String finalRes = res;
            long time = System.currentTimeMillis();
            FileLog.getInstance().logQueue.postRunnable(() -> {
                try {
                    String metadata = "requestMsgId=" + requestMsgId + " requestingTime=" + (System.currentTimeMillis() - startRequestTimeInMillis) +  " request_token=" + requestToken;
                    FileLog.getInstance().tlStreamWriter.write(getInstance().dateFormat.format(time) + " " + metadata);
                    FileLog.getInstance().tlStreamWriter.write(req);
                    FileLog.getInstance().tlStreamWriter.write("\n");
                    FileLog.getInstance().tlStreamWriter.write(finalRes);
                    FileLog.getInstance().tlStreamWriter.write("\n\n");
                    FileLog.getInstance().tlStreamWriter.flush();

                    Log.d(mtproto_tag, metadata);
                    Log.d(mtproto_tag, req);
                    Log.d(mtproto_tag, finalRes);
                    Log.d(mtproto_tag, " ");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    public static void dumpUnparsedMessage(TLObject message, long messageId) {
        if (!BuildVars.DEBUG_PRIVATE_VERSION || !BuildVars.LOGS_ENABLED || message == null) {
            return;
        }
        try {
            getInstance().dateFormat.format(System.currentTimeMillis());
            String messageStr = "receive message -> " + message.getClass().getSimpleName() + " : " + gson.toJson(message);
            String res = "null";
            long time = System.currentTimeMillis();
            FileLog.getInstance().logQueue.postRunnable(() -> {
                try {
                    String metadata = getInstance().dateFormat.format(time);// + " msgId=" + messageId;

                    FileLog.getInstance().tlStreamWriter.write(metadata);
                    FileLog.getInstance().tlStreamWriter.write(messageStr);
                    FileLog.getInstance().tlStreamWriter.write("\n\n");
                    FileLog.getInstance().tlStreamWriter.flush();

                    Log.d(mtproto_tag, "msgId=" + messageId);
                    Log.d(mtproto_tag, messageStr);
                    Log.d(mtproto_tag, " ");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static void checkGson() {
        if (gson == null) {
            HashSet<String> privateFields = new HashSet<>();
            privateFields.add("message");
            privateFields.add("phone");
            privateFields.add("about");
            privateFields.add("status_text");
            privateFields.add("bytes");
            privateFields.add("secret");
            privateFields.add("stripped_thumb");

            privateFields.add("networkType");
            privateFields.add("disableFree");

            //exclude file loading
            excludeRequests = new HashSet<>();
            excludeRequests.add("TL_upload_getFile");
            excludeRequests.add("TL_upload_getWebFile");

            gson = new GsonBuilder().addSerializationExclusionStrategy(new ExclusionStrategy() {

                @Override
                public boolean shouldSkipField(FieldAttributes f) {
                    if (privateFields.contains(f.getName())) {
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            }).create();
        }
    }



    public void init() {
        if (initied) {
            return;
        }
        dateFormat = FastDateFormat.getInstance("dd_MM_yyyy_HH_mm_ss", Locale.US);
        String date = dateFormat.format(System.currentTimeMillis());
        try {
            File sdCard = ApplicationLoader.applicationContext.getExternalFilesDir(null);
            if (sdCard == null) {
                return;
            }
            File dir = new File(sdCard.getAbsolutePath() + "/logs");
            dir.mkdirs();

            currentFile = new File(dir, date + ".txt");
            tlRequestsFile = new File(dir, date + "_mtproto.txt");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            logQueue = new DispatchQueue("logQueue");
            currentFile.createNewFile();
            FileOutputStream stream = new FileOutputStream(currentFile);
            streamWriter = new OutputStreamWriter(stream);
            streamWriter.write("-----start log " + date + "-----\n");
            streamWriter.flush();

            FileOutputStream tlStream = new FileOutputStream(tlRequestsFile);
            tlStreamWriter = new OutputStreamWriter(tlStream);
            tlStreamWriter.write("-----start log " + date + "-----\n");
            tlStreamWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
        initied = true;
    }

    public static void ensureInitied() {
        getInstance().init();
    }

    public static String getNetworkLogPath() {
        if (!BuildVars.LOGS_ENABLED) {
            return "";
        }
        try {
            File sdCard = ApplicationLoader.applicationContext.getExternalFilesDir(null);
            if (sdCard == null) {
                return "";
            }
            File dir = new File(sdCard.getAbsolutePath() + "/logs");
            dir.mkdirs();
            getInstance().networkFile = new File(dir, getInstance().dateFormat.format(System.currentTimeMillis()) + "_net.txt");
            return getInstance().networkFile.getAbsolutePath();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getTonlibLogPath() {
        if (!BuildVars.LOGS_ENABLED) {
            return "";
        }
        try {
            File sdCard = ApplicationLoader.applicationContext.getExternalFilesDir(null);
            if (sdCard == null) {
                return "";
            }
            File dir = new File(sdCard.getAbsolutePath() + "/logs");
            dir.mkdirs();
            getInstance().tonlibFile = new File(dir, getInstance().dateFormat.format(System.currentTimeMillis()) + "_tonlib.txt");
            return getInstance().tonlibFile.getAbsolutePath();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void e(final String message, final Throwable exception) {
        if (!BuildVars.LOGS_ENABLED) {
            return;
        }
        ensureInitied();
        Log.e(tag, message, exception);
        if (getInstance().streamWriter != null) {
            getInstance().logQueue.postRunnable(() -> {
                try {
                    getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " E/tmessages: " + message + "\n");
                    getInstance().streamWriter.write(exception.toString());
                    getInstance().streamWriter.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public static void e(final String message) {
        if (!BuildVars.LOGS_ENABLED) {
            return;
        }
        ensureInitied();
        Log.e(tag, message);
        if (getInstance().streamWriter != null) {
            getInstance().logQueue.postRunnable(() -> {
                try {
                    getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " E/tmessages: " + message + "\n");
                    getInstance().streamWriter.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public static void e(final Throwable e) {
        e(e, true);
    }

    public static void e(final Throwable e, boolean logToAppCenter) {
        if (!BuildVars.LOGS_ENABLED) {
            return;
        }
        /*if (BuildVars.DEBUG_VERSION && needSent(e) && logToAppCenter) {
            AndroidUtilities.appCenterLog(e);
        }*/
        ensureInitied();
        e.printStackTrace();
        if (getInstance().streamWriter != null) {
            getInstance().logQueue.postRunnable(() -> {
                try {
                    getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " E/tmessages: " + e + "\n");
                    StackTraceElement[] stack = e.getStackTrace();
                    for (int a = 0; a < stack.length; a++) {
                        getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " E/tmessages: " + stack[a] + "\n");
                    }
                    getInstance().streamWriter.flush();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            });
        } else {
            e.printStackTrace();
        }
    }

    public static void fatal(final Throwable e) {
        fatal(e, true);
    }

    public static void fatal(final Throwable e, boolean logToAppCenter) {
        if (!BuildVars.LOGS_ENABLED) {
            return;
        }
        /*if (BuildVars.DEBUG_VERSION && needSent(e) && logToAppCenter) {
            AndroidUtilities.appCenterLog(e);
        }*/
        ensureInitied();
        e.printStackTrace();
        if (getInstance().streamWriter != null) {
            getInstance().logQueue.postRunnable(() -> {
                try {
                    getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " E/tmessages: " + e + "\n");
                    StackTraceElement[] stack = e.getStackTrace();
                    for (int a = 0; a < stack.length; a++) {
                        getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " E/tmessages: " + stack[a] + "\n");
                    }
                    getInstance().streamWriter.flush();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }

                if (BuildVars.DEBUG_PRIVATE_VERSION) {
                    System.exit(2);
                }
            });
        } else {
            e.printStackTrace();
            if (BuildVars.DEBUG_PRIVATE_VERSION) {
                System.exit(2);
            }
        }
    }

    private static boolean needSent(Throwable e) {
        if (e instanceof InterruptedException || e instanceof MediaCodecVideoConvertor.ConversionCanceledException || e instanceof IgnoreSentException) {
            return false;
        }
        return true;
    }

    public static void d(final String message) {
        if (!BuildVars.LOGS_ENABLED) {
            return;
        }
        ensureInitied();
        Log.d(tag, message);
        if (getInstance().streamWriter != null) {
            getInstance().logQueue.postRunnable(() -> {
                try {
                    getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " D/tmessages: " + message + "\n");
                    getInstance().streamWriter.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                    if (AndroidUtilities.isENOSPC(e)) {
                        LaunchActivity.checkFreeDiscSpaceStatic(1);
                    }
                }
            });
        }
    }

    public static void w(final String message) {
        if (!BuildVars.LOGS_ENABLED) {
            return;
        }
        ensureInitied();
        Log.w(tag, message);
        if (getInstance().streamWriter != null) {
            getInstance().logQueue.postRunnable(() -> {
                try {
                    getInstance().streamWriter.write(getInstance().dateFormat.format(System.currentTimeMillis()) + " W/tmessages: " + message + "\n");
                    getInstance().streamWriter.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public static void cleanupLogs() {
        ensureInitied();
        File sdCard = ApplicationLoader.applicationContext.getExternalFilesDir(null);
        if (sdCard == null) {
            return;
        }
        File dir = new File (sdCard.getAbsolutePath() + "/logs");
        File[] files = dir.listFiles();
        if (files != null) {
            for (int a = 0; a < files.length; a++) {
                File file = files[a];
                if (getInstance().currentFile != null && file.getAbsolutePath().equals(getInstance().currentFile.getAbsolutePath())) {
                    continue;
                }
                if (getInstance().networkFile != null && file.getAbsolutePath().equals(getInstance().networkFile.getAbsolutePath())) {
                    continue;
                }
                if (getInstance().tonlibFile != null && file.getAbsolutePath().equals(getInstance().tonlibFile.getAbsolutePath())) {
                    continue;
                }
                file.delete();
            }
        }
    }

    public static class IgnoreSentException extends Exception{

        public IgnoreSentException(String e) {
            super(e);
        }

    }
}
