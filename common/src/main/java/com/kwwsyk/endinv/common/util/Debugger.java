package com.kwwsyk.endinv.common.util;

/**
 * Used for a remote debug of modpacks that can not run/debug directly via ide
 */
@Deprecated
public class Debugger {

    public static final String itemTagField = "f_54190_";

    public static final String getOrCreateTagMethod = "m_41784_";
    public static final String getOrCreateTag = "getOrCreateTag";
    public static final String getGetOrCreateTagSig = "()Lnet/minecraft/nbt/CompoundTag;";

    private static final org.apache.logging.log4j.Logger LOG;

    static {
        var ctx = (org.apache.logging.log4j.core.LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
        var cfg = ctx.getConfiguration();
        var file = net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("logs/tagwatch-workbench_b.log").toString();

        var layout = org.apache.logging.log4j.core.layout.PatternLayout.newBuilder()
                .withPattern("[%d{HH:mm:ss}] %m%n").withConfiguration(cfg).build();
        var app = org.apache.logging.log4j.core.appender.FileAppender.newBuilder()
                .withFileName(file).withAppend(true).setName("TagWatchFile").setConfiguration(cfg).setLayout(layout).build();
        app.start(); cfg.addAppender(app);

        var loggerCfg = org.apache.logging.log4j.core.config.LoggerConfig.createLogger(
                false, org.apache.logging.log4j.Level.INFO, "TagWatch", "true",
                new org.apache.logging.log4j.core.config.AppenderRef[]{}, null, cfg, null);
        loggerCfg.addAppender(app, null, null); cfg.addLogger("TagWatch", loggerCfg); ctx.updateLoggers();

        LOG = org.apache.logging.log4j.LogManager.getLogger("TagWatch");
    }

    public static void log(String msg) {
        LOG.info(msg);
        var sb = new StringBuilder("Stack:\n");
        for (var e : Thread.currentThread().getStackTrace()) sb.append("  ").append(e).append('\n');
        LOG.info(sb.toString());
    }

    public static boolean isWorkbenchB(net.minecraft.world.item.ItemStack s) {
        var key = s.getItem().builtInRegistryHolder().key().location();
        return "workbench_b".equals(key.getPath()); // 只按 path 过滤，需更严谨可比对 namespace
    }
}
