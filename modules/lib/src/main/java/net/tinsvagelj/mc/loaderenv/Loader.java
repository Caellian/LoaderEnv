package net.tinsvagelj.mc.loaderenv;

import java.util.List;

/**
 * Enumeration of all mod loaders.
 * <p>
 * Loaders aren't necessarily mutually exclusive. Some loaders have historically worked alongside others.
 * <p>
 * Ordinal value of loaders is not stable.
 */
public enum Loader {
    /**
     * Links: <a href="https://neoforged.net/">Website</a> | <a href="https://github.com/neoforged">GitHub</a><br/>
     * Min. version: 1.20.1
     */
    NEO_FORGE("NeoForge", "net.neoforged.neoforge.internal.versions.neoforge.NeoForgeVersion"),
    /**
     * Links: <a href="https://fabricmc.net/">Website</a> | <a href="https://github.com/FabricMC">GitHub</a><br/>
     * Min. version: 1.14
     */
    FABRIC("Fabric", "net.fabricmc.api.ModInitializer"),
    /**
     * Links: <a href="https://minecraftforge.net/">Website</a> | <a href="https://github.com/MinecraftForge">GitHub</a><br/>
     * Min. version: 1.1
     */
    FORGE("Forge", "net.minecraftforge.versions.forge.ForgeVersion"),
    /**
     * Links: <a href="https://quiltmc.org/en//">Website</a> | <a href="https://github.com/MinecraftForge">GitHub</a><br/>
     * Min. version: 1.13
     */
    QUILT("Quilt", "org.quiltmc.qsl.base.api.entrypoint.ModInitializer"),
    /**
     * Links: <a href="http://www.liteloader.com">Website</a> | <a href="http://develop.liteloader.com/liteloader/LiteLoader">GitLab</a><br/>
     * Max. version: 1.12.2<br/>
     * Min. version: 1.5.2
     */
    LITE_LOADER("LiteLoader", "com.mumfrey.liteloader.LiteMod"),
    /**
     * Links: <a href="https://www.cuchazinteractive.com/minecraft/m3l/">Website</a> | <a href="https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/wip-mods/1446050-magic-mojo-mod-loader-a-mod-loader-for-advanced">MC Forum</a><br/>
     * Max. version: 1.8.3<br/>
     * Min. version: 1.8.3
     */
    M3L("MagicMojoModLoader", "cuchaz.m3l.M3L"),
    /**
     * Links: <a href="https://github.com/phase/OpenModLoader">GitHub</a><br/>
     */
    OPEN_MOD_LOADER("OpenModLoader", "xyz.openmodloader.OpenModLoader"),
    /**
     * Links: <a href="https://github.com/NOVA-Team">GitHub</a><br/>
     * Min. version: 1.7.10<br/>
     * Max. version: 1.8
     */
    NOVA("NOVA", "nova.internal.core.Game"),
    /**
     * Links: <a href="https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/1272333-risugamis-mods-updated">MC Forum</a><br/>
     * Max. version: 1.6.2<br/>
     * Min. version: a1.2.0
     */
    RISUGAMI("Risugami", "ModLoader"),
    /**
     * Links: <a href="https://papermc.io/">Website</a> | <a href="https://github.com/PaperMC">PaperMC</a><br/>
     * Min. version: 1.8.8<br/>
     * Side: Server
     */
    PAPER_MC("PaperMC", "org.spigotmc.SpigotConfig"),
    /**
     * Links: <a href="https://www.spigotmc.org/">Website</a> | <a href="https://hub.spigotmc.org/stash/projects/SPIGOT">Bitbucket</a><br/>
     * Min. version: 1.7<br/>
     * Side: Server
     */
    SPIGOT("Spigot", "org.spigotmc.SpigotConfig"),
    /**
     * Links: <a href="https://dev.bukkit.org/">Website</a> | <a href="https://github.com/Bukkit/Bukkit">GitHub</a><br/>
     * Min. version: b1.6.6<br/>
     * Side: Server
     */
    BUKKIT("Bukkit", "org.bukkit.Bukkit");

    public final String prettyName;
    public final String[] detectClasses;

    Loader(String prettyName, String... detectClass) {
        this.prettyName = prettyName;
        this.detectClasses = detectClass;
    }

    public static Loader parse(String forceEnv) {
        String checked = forceEnv.toLowerCase().replace("_", "").replace("-", "");

        for (Loader it : Loader.values()) {
            if (it.prettyName.toLowerCase().equals(checked) || it.name().toLowerCase().replace("_", "").equals(checked)) {
                return it;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return this.prettyName;
    }

    public static List<Loader> getAvailable() {
        java.util.ArrayList<Loader> result = new java.util.ArrayList<>(1);
        for (Loader env : Loader.values()) {
            for (String className : env.detectClasses) {
                try {
                    Class.forName(className, false, ClassLoader.getSystemClassLoader());
                    result.add(env);
                    break;
                } catch (ClassNotFoundException ignore) {}
            }
        }
        return result;
    }
}
