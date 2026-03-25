package mindustry.core;

import arc.*;
import arc.Files.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;

public class Version{
    /** Build type. 'official' for official releases; 'custom' or 'bleeding edge' are also used. */
    public static String type = "unknown";
    /** Build modifier, e.g. 'alpha' or 'release' */
    public static String modifier = "unknown";
    /** Git commit hash (short) */
    public static String commitHash = "unknown";
    /** Date that this version was built. */
    public static String buildDate = "unknown";
    /** Full build label shown in UI/logs, e.g. '146.2' or '0.6.2'. */
    public static String displayBuild = "0";
    /** Number specifying the major version, e.g. '4' */
    public static int number;
    /** Build number, e.g. '43'. set to '-1' for custom builds. */
    public static int build = 0;
    /** Revision number. Used for hotfixes. Does not affect server compatibility. */
    public static int revision = 0;
    /** Whether version loading is enabled. */
    public static boolean enabled = true;

    public static void init(){
        if(!enabled) return;

        Fi file = OS.isAndroid || OS.isIos ? Core.files.internal("version.properties") : new Fi("version.properties", FileType.internal);

        ObjectMap<String, String> map = new ObjectMap<>();
        PropertiesUtils.load(map, file.reader());

        type = map.get("type");
        number = Integer.parseInt(map.get("number", "4"));
        modifier = map.get("modifier");
        commitHash = map.get("commitHash", "unknown");
        buildDate = map.get("buildDate", "unknown");
        displayBuild = map.get("displayBuild", map.get("build", "0"));
        String rawBuild = map.get("build", displayBuild);
        if(rawBuild.contains(".")){
            String[] split = rawBuild.split("\\.");
            try{
                build = Integer.parseInt(split[0]);
                revision = split.length > 1 ? Integer.parseInt(split[1]) : 0;
            }catch(Throwable e){
                e.printStackTrace();
                build = -1;
                revision = 0;
            }
        }else{
            build = Strings.canParseInt(rawBuild) ? Integer.parseInt(rawBuild) : -1;
            revision = 0;
        }
    }

    /** @return whether the current game version is greater than the specified version string, e.g. "120.1"*/
    public static boolean isAtLeast(String str){
        if(str == null || str.isEmpty()) return true;
        return isAtLeast(buildString(), str);
    }

    /** @return whether the version numbers are greater than the specified version string, e.g. "120.1"*/
    public static boolean isAtLeast(int build, int revision, String str){
        if(str == null || str.isEmpty() || build < 0) return true;
        return isAtLeast(build + (revision == 0 ? "" : "." + revision), str);
    }

    public static String buildString(){
        if(displayBuild != null && !displayBuild.isEmpty()) return displayBuild;
        return build < 0 ? "custom" : build + (revision == 0 ? "" : "." + revision);
    }

    public static boolean isInit(){
        return build == 0 && revision == 0 && (displayBuild == null || displayBuild.isEmpty() || displayBuild.equals("0"));
    }

    /** get menu version without colors */
    public static String combined(){
        if(build == -1){
            return buildString().equals("custom") ? "custom build" : buildString();
        }
        return (type.equals("official") ? modifier : type) + " build " + buildString() + (commitHash.equals("unknown") ? "" : " (" + commitHash + ")");
    }

    private static boolean isAtLeast(String current, String required){
        IntSeq currentParts = versionParts(current), requiredParts = versionParts(required);
        if(currentParts.size == 0 || requiredParts.size == 0) return true;

        int max = Math.max(currentParts.size, requiredParts.size);
        for(int i = 0; i < max; i++){
            int cur = i < currentParts.size ? currentParts.get(i) : 0;
            int req = i < requiredParts.size ? requiredParts.get(i) : 0;
            if(cur != req) return cur > req;
        }
        return true;
    }

    private static IntSeq versionParts(String version){
        IntSeq parts = new IntSeq();
        if(version == null || version.isEmpty()) return parts;

        for(String part : version.split("\\.")){
            if(!Strings.canParseInt(part)) return new IntSeq();
            parts.add(Integer.parseInt(part));
        }

        return parts;
    }
}
