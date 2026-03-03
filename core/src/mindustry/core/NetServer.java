package mindustry.core;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.CommandHandler.*;
import arc.util.io.*;
import arc.util.serialization.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.logic.*;
import mindustry.maps.Map;
import mindustry.net.*;
import mindustry.net.Administration.*;
import mindustry.net.Packets.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.*;
import java.util.Date;
import java.util.regex.*;
import java.util.zip.*;

import static arc.util.Log.*;
import static mindustry.Vars.*;

public class NetServer implements ApplicationListener{
    /** note that snapshots are compressed, so the max snapshot size here is above the typical UDP safe limit */
    private static final int maxSnapshotSize = 800;
    private static final int timerBlockSync = 0, timerHealthSync = 1;
    private static final float blockSyncTime = 60 * 6, healthSyncTime = 30;
    private static final int defaultStartCountdownSeconds = 10;
    private static final int maxStartCountdownSeconds = 600;
    private static final Pattern startTimePattern = Pattern.compile("\\[@\\s*startTime\\s*=\\s*(\\d+)\\s*\\]", Pattern.CASE_INSENSITIVE);
    private static final FloatBuffer fbuffer = FloatBuffer.allocate(20);
    private static final Writes dataWrites = new Writes(null);
    private static final IntSeq hiddenIds = new IntSeq();
    private static final IntSeq healthSeq = new IntSeq(maxSnapshotSize / 4 + 1);
    private static final Vec2 vector = new Vec2();
    private static final float identityPingInterval = 60f * 8f;
    private static final long identityTimeoutMs = 1000L * 20L;
    /** If a player goes away of their server-side coordinates by this distance, they get teleported back. */
    private static final float correctDist = tilesize * 14f;

    public Administration admins = new Administration();
    public CommandHandler clientCommands = new CommandHandler("/");
    public TeamAssigner assigner = (player, players) -> {
        if(state.rules.pvp){
            //find team with minimum amount of players and auto-assign player to that.
            TeamData re = state.teams.getActive().min(data -> {
                if((state.rules.waveTeam == data.team && state.rules.waves) || !data.hasCore() || data.team == Team.derelict || !data.team.rules().protectCores) return Integer.MAX_VALUE;

                int count = 0;
                for(Player other : players){
                    if(other.team() == data.team && other != player){
                        count++;
                    }
                }
                return (float)count + Mathf.random(-0.1f, 0.1f); //if several have the same playercount pick random
            });
            return re == null ? null : re.team;
        }

        return state.rules.defaultTeam;
    };
    /** Converts a message + NULLABLE player sender into a single string. Override for custom prefixes/suffixes. */
    public ChatFormatter chatFormatter = (player, message) -> player == null ? message : "[coral][[" + player.coloredName() + "[coral]]:[white] " + message;

    /** Handles an incorrect command response. Returns text that will be sent to player. Override for customisation. */
    public InvalidCommandHandler invalidHandler = (player, response) -> {
        if(response.type == ResponseType.manyArguments){
            return "[scarlet]Too many arguments. Usage:[lightgray] " + response.command.text + "[gray] " + response.command.paramText;
        }else if(response.type == ResponseType.fewArguments){
            return "[scarlet]Too few arguments. Usage:[lightgray] " + response.command.text + "[gray] " + response.command.paramText;
        }else{ //unknown command
            int minDst = 0;
            Command closest = null;

            for(Command command : netServer.clientCommands.getCommandList()){
                int dst = Strings.levenshtein(command.text, response.runCommand);
                if(dst < 3 && (closest == null || dst < minDst)){
                    minDst = dst;
                    closest = command;
                }
            }

            if(closest != null){
                return "[scarlet]Unknown command. Did you mean \"[lightgray]" + closest.text + "[]\"?";
            }else{
                return "[scarlet]Unknown command. Check [lightgray]/help[scarlet].";
            }
        }
    };

    private boolean closing = false, pvpAutoPaused = true;
    private Interval timer = new Interval(10);
    private IntSet buildHealthChanged = new IntSet();
    private Rand identityRand = new Rand();
    private ObjectMap<String, String> identityChallenges = new ObjectMap<>();
    private ObjectMap<String, Long> identityChallengeTimes = new ObjectMap<>();
    private ObjectSet<String> integrityBans = new ObjectSet<>();
    private final ObjectSet<String> adminOnlyClientCommands = new ObjectSet<>();
    private final Fi banListFile = Core.settings.getDataDirectory().child("banList");
    private final ObjectMap<String, Effect> wayzerEffects = new ObjectMap<>();
    private long gatherLastTime = -1000000L;
    private int gatherTileX = Integer.MIN_VALUE, gatherTileY = Integer.MIN_VALUE;
    private @Nullable String scheduledRestartMessage;
    private boolean restarting;
    private final long serverStartTimeMs = Time.millis();
    private boolean customWelcomeEnabled = Core.settings.getBool("wayzer-custom-welcome", false);
    private String welcomeTemplate = Core.settings.getString("wayzer-welcome-template", "Welcome to this Server\n[green]Welcome {player.name}[green] to this server[]");
    private String welcomeJoinTemplate = Core.settings.getString("wayzer-welcome-join-template", "[cyan][+] {player.name} [goldenrod]joined the server");
    private String welcomeLeaveTemplate = Core.settings.getString("wayzer-welcome-leave-template", "[coral][-] {player.name} [brick]left the server");
    private boolean alertEnabled = Core.settings.getBool("wayzer-alert-enabled", false);
    private int alertIntervalSeconds = Math.max(30, Core.settings.getInt("wayzer-alert-interval-seconds", 600));
    private Seq<String> alertMessages = new Seq<>();
    private int alertIndex;
    private int nextPlayerNumber = 1;
    private ObjectIntMap<String> playerNumbers = new ObjectIntMap<>();
    private final ObjectMap<String, TransferServerInfo> transferServers = new ObjectMap<>();
    private boolean tpsLimitEnabled = Core.settings.getBool("wayzer-tpslimit-enabled", false);
    private float tpsLimitMaxDelta = Mathf.clamp(Core.settings.getFloat("wayzer-tpslimit-maxdelta", 6f), 0.1f, 60f);
    private @Nullable Floatp baseDeltaProvider;
    private String resourceSiteApiRoot = Core.settings.getString("wayzer-resource-api-root", "https://api.mindustry.top");
    private boolean matchPreviewActive = false;
    private boolean skipNextPreview = false;
    private final IntIntMap teamHandicapPercent = new IntIntMap();
    private final IntSet handicappedUnits = new IntSet();
    private final int[] handicapOptions = {100, 90, 80, 70, 60, 50};
    private int startCountdownToken = 0;
    private boolean startCountdownActive = false;
    private final int handicapMenuId;

    /** Current kick session. */
    public @Nullable VoteSession currentlyKicking = null;
    /** Current generic vote session (ported from WayZer vote commands). */
    public @Nullable GenericVoteSession currentlyVoting = null;
    /** Duration of a kick in seconds. */
    public static int kickDuration = 60 * 60;
    /** Voting round duration in seconds. */
    public static float voteDuration = 0.5f * 60;
    /** Cooldown between votes in seconds. */
    public static int voteCooldown = 60 * 5;

    private ReusableByteOutStream writeBuffer = new ReusableByteOutStream(127);
    private Writes outputBuffer = new Writes(new DataOutputStream(writeBuffer));

    /** Stream for writing player sync data to. */
    private ReusableByteOutStream syncStream = new ReusableByteOutStream();
    /** Data stream for writing player sync data to. */
    private DataOutputStream dataStream = new DataOutputStream(syncStream);
    private Writes dataStreamWrites = new Writes(dataStream);
    /** Packet handlers for custom types of messages. */
    private ObjectMap<String, Seq<Cons2<Player, String>>> customPacketHandlers = new ObjectMap<>();
    /** Packet handlers for custom types of messages - binary version. */
    private ObjectMap<String, Seq<Cons2<Player, byte[]>>> customBinaryPacketHandlers = new ObjectMap<>();
    /** Packet handlers for logic client data */
    private ObjectMap<String, Seq<Cons2<Player, Object>>> logicClientDataHandlers = new ObjectMap<>();

    public NetServer(){
        handicapMenuId = Menus.registerMenu(this::handleHandicapMenu);

        loadBanList();
        loadWayzerEffects();
        loadAlertConfig();
        loadTransferServers();
        initTpsLimit();
        addPacketHandler("identity-pong", this::handleIdentityPong);
        Config.showConnectMessages.set(!customWelcomeEnabled);

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/ext/welcomeMsg.kts
        Events.on(PlayerJoin.class, event -> {
            event.player.sendMessage(formatWelcomeTemplate(welcomeTemplate, event.player));
            if(customWelcomeEnabled){
                Call.sendMessage(formatWelcomeTemplate(welcomeJoinTemplate, event.player));
            }
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/ext/welcomeMsg.kts
        Events.on(PlayerLeave.class, event -> {
            if(customWelcomeEnabled && !"[Silent_Leave]".equals(event.player.lastText())){
                Call.sendMessage(formatWelcomeTemplate(welcomeLeaveTemplate, event.player));
            }
        });

        Events.on(WorldLoadEvent.class, event -> {
            if(!net.server() || state.map == null || !state.isGame()) return;

            handicappedUnits.clear();
            resetTeamHandicaps();

            if(skipNextPreview){
                skipNextPreview = false;
                return;
            }

            enterMapPreview();
        });

        Events.on(UnitCreateEvent.class, event -> applyTeamHandicap(event.unit));
        Events.on(UnitSpawnEvent.class, event -> applyTeamHandicap(event.unit));
        Events.on(UnitDestroyEvent.class, event -> handicappedUnits.remove(event.unit.id));
        Events.on(UnitChangeEvent.class, event -> {
            if(event.unit != null){
                applyTeamHandicap(event.unit);
            }
        });

        net.handleServer(Connect.class, (con, connect) -> {
            Events.fire(new ConnectionEvent(con));

            if(admins.isIPBanned(connect.addressTCP) || admins.isSubnetBanned(connect.addressTCP)){
                con.kick(KickReason.banned);
            }
        });

        net.handleServer(Disconnect.class, (con, packet) -> {
            if(con.player != null){
                onDisconnect(con.player, packet.reason);
            }
        });

        net.handleServer(ConnectPacket.class, (con, packet) -> {
            if(con.kicked) return;

            if(con.address.startsWith("steam:")){
                packet.uuid = con.address.substring("steam:".length());
            }

            Events.fire(new ConnectPacketEvent(con, packet));

            con.connectTime = Time.millis();

            String deviceHash = packet.deviceHash == null ? null : packet.deviceHash.trim();
            if(deviceHash == null || deviceHash.isEmpty() || !deviceHash.startsWith("$2") || deviceHash.length() > 200){
                con.kick("This server requires a valid local device hash identity.", 0);
                return;
            }

            String uuid = deviceHash;

            if(admins.isIPBanned(con.address) || admins.isSubnetBanned(con.address) || con.kicked || !con.isConnected()) return;

            if(con.hasBegunConnecting){
                con.kick(KickReason.idInUse);
                return;
            }

            PlayerInfo info = admins.getInfo(uuid);

            con.hasBegunConnecting = true;
            con.mobile = packet.mobile;

            if(packet.uuid == null || packet.usid == null){
                con.kick(KickReason.idInUse);
                return;
            }

            //there's no reason to tell users that their name is inappropriate, as they may try to bypass it
            if(admins.isIDBanned(uuid) || isIntegrityBanned(uuid) || admins.isNameBanned(packet.name)){
                con.kick(KickReason.banned);
                return;
            }

            if(Time.millis() < admins.getKickTime(uuid, con.address)){
                con.kick(KickReason.recentKick);
                return;
            }

            if(admins.getPlayerLimit() > 0 && Groups.player.size() >= admins.getPlayerLimit() && !netServer.admins.isAdmin(uuid, packet.usid)){
                con.kick(KickReason.playerLimit);
                return;
            }

            Seq<String> extraMods = packet.mods.copy();
            Seq<String> missingMods = mods.getIncompatibility(extraMods);

            if(!extraMods.isEmpty() || !missingMods.isEmpty()){
                //can't easily be localized since kick reasons can't have formatted text with them
                StringBuilder result = new StringBuilder("[accent]Incompatible mods![]\n\n");
                if(!missingMods.isEmpty()){
                    result.append("Missing:[lightgray]\n").append("> ").append(missingMods.toString("\n> "));
                    result.append("[]\n");
                }

                if(!extraMods.isEmpty()){
                    result.append("Unnecessary mods:[lightgray]\n").append("> ").append(extraMods.toString("\n> "));
                }
                con.kick(result.toString(), 0);
                return;
            }

            if(!admins.isWhitelisted(uuid, packet.usid)){
                info.adminUsid = packet.usid;
                info.lastName = packet.name;
                info.id = uuid;
                admins.save();
                Call.infoMessage(con, "You are not whitelisted here.");
                info("&lcDo &lywhitelist add @&lc to whitelist the player &lb'@'", uuid, packet.name);
                con.kick(KickReason.whitelist);
                return;
            }

            if(packet.versionType == null || ((packet.version == -1 || !packet.versionType.equals(Version.type)) && Version.build != -1 && !admins.allowsCustomClients())){
                con.kick(!Version.type.equals(packet.versionType) ? KickReason.typeMismatch : KickReason.customClient);
                return;
            }

            boolean preventDuplicates = headless && netServer.admins.isStrict();

            if(preventDuplicates){
                if(Groups.player.contains(p -> Strings.stripColors(p.name).trim().equalsIgnoreCase(Strings.stripColors(packet.name).trim()))){
                    con.kick(KickReason.nameInUse);
                    return;
                }

                if(Groups.player.contains(player -> player.uuid().equals(uuid) || player.usid().equals(packet.usid))){
                    con.uuid = uuid;
                    con.kick(KickReason.idInUse);
                    return;
                }

                for(var otherCon : net.getConnections()){
                    if(otherCon != con && uuid.equals(otherCon.uuid)){
                        con.uuid = uuid;
                        con.kick(KickReason.idInUse);
                        return;
                    }
                }
            }

            packet.name = fixName(packet.name);

            if(packet.name.trim().length() <= 0){
                con.kick(KickReason.nameEmpty);
                return;
            }

            if(packet.locale == null){
                packet.locale = "en";
            }

            String ip = con.address;

            admins.updatePlayerJoined(uuid, ip, packet.name);
            String shortUid = admins.bindDeviceIdentity(uuid, deviceHash);

            if(packet.version != Version.build && Version.build != -1 && packet.version != -1){
                con.kick(packet.version > Version.build ? KickReason.serverOutdated : KickReason.clientOutdated);
                return;
            }

            if(packet.version == -1){
                con.modclient = true;
            }

            Player player = Player.create();
            player.admin = admins.isAdmin(uuid, packet.usid);
            player.con = con;
            player.con.usid = packet.usid;
            player.con.uuid = uuid;
            player.con.mobile = packet.mobile;
            player.name = withUidSuffix(packet.name, shortUid);
            player.locale = packet.locale;
            player.color.set(packet.color).a(1f);

            //save admin ID but don't overwrite it
            if(!player.admin && !info.admin){
                info.adminUsid = packet.usid;
            }

            if(info.timesJoined == 1){
                Call.infoMessage(con, "Your identity UID: " + shortUid);
            }

            try{
                writeBuffer.reset();
                player.write(outputBuffer);
            }catch(Throwable t){
                con.kick(KickReason.nameEmpty);
                err(t);
                return;
            }

            con.player = player;

            //assign team; if all teams are occupied, fallback is handled by assignTeam()
            Team assignedTeam = assignTeam(player);
            player.team(assignedTeam);
            int playerNumber = nextPlayerNumber++;
            playerNumbers.put(player.uuid(), playerNumber);
            Call.infoMessage(con, "Your player number: #" + playerNumber);

            sendWorldData(player);

            platform.updateRPC();

            Events.fire(new PlayerConnect(player));
        });

        net.handleServer(ServerInfoRequest.class, (con, packet) -> {
            if(!con.isConnected()) return;

            ByteBuffer buffer = NetworkIO.writeServerData();
            int length = buffer.position();
            buffer.position(0);
            buffer.limit(length);

            ServerInfoResponse response = new ServerInfoResponse();
            response.data = new byte[length];
            buffer.get(response.data);
            con.send(response, true);

            //TCP info probe clients do not proceed with full ConnectPacket flow.
            if(!con.hasBegunConnecting){
                con.close();
            }
        });

        registerCommands();
    }

    @Override
    public void init(){
        mods.eachClass(mod -> mod.registerClientCommands(clientCommands));
    }

    private void markAdminOnlyCommands(String... commands){
        for(String command : commands){
            adminOnlyClientCommands.add(command);
        }
    }

    private Seq<Command> getVisibleClientCommands(Player player){
        var all = clientCommands.getCommandList();
        if(player.admin) return all;

        Seq<Command> visible = new Seq<>(all.size);
        for(Command command : all){
            if(!adminOnlyClientCommands.contains(command.text)){
                visible.add(command);
            }
        }
        return visible;
    }

    private void registerCommands(){
        markAdminOnlyCommands(
        "siteapicfg", "host", "clearunit", "dosbanclear", "showeffect", "showeffectlist",
        "spawn", "tp", "forceob", "restart", "js", "welcomecfg", "welcometpl",
        "alerttoggle", "alertinterval", "alertadd", "alertremove", "alertnext",
        "goserveradd", "goserverremove", "tpslimit", "tpslimitmax", "start", "a"
        );

        clientCommands.<Player>register("help", "[page]", "Lists all commands.", (args, player) -> {
            if(args.length > 0 && !Strings.canParseInt(args[0])){
                player.sendMessage("[scarlet]'page' must be a number.");
                return;
            }
            Seq<Command> visibleCommands = getVisibleClientCommands(player);
            if(visibleCommands.isEmpty()){
                player.sendMessage("[scarlet]No commands available.");
                return;
            }
            int commandsPerPage = 6;
            int page = args.length > 0 ? Strings.parseInt(args[0]) : 1;
            int pages = Mathf.ceil((float)visibleCommands.size / commandsPerPage);

            page--;

            if(page >= pages || page < 0){
                player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and[orange] " + pages + "[scarlet].");
                return;
            }

            StringBuilder result = new StringBuilder();
            result.append(Strings.format("[orange]-- Commands Page[lightgray] @[gray]/[lightgray]@[orange] --\n\n", (page + 1), pages));

            for(int i = commandsPerPage * page; i < Math.min(commandsPerPage * (page + 1), visibleCommands.size); i++){
                Command command = visibleCommands.get(i);
                result.append("[orange] /").append(command.text).append("[white] ").append(command.paramText).append("[lightgray] - ").append(command.description).append("\n");
            }
            player.sendMessage(result.toString());
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/cmds/mapsCmd.kts (partial, built-in map source only)
        clientCommands.<Player>register("maps", "[filter/page] [page]", "List built-in server maps, 10 per page.", (args, player) -> {
            String filter = null;
            int page = 1;

            if(args.length == 1){
                if(Strings.canParseInt(args[0])){
                    page = Strings.parseInt(args[0]);
                }else{
                    filter = args[0];
                }
            }else if(args.length >= 2){
                filter = args[0];
                if(!Strings.canParseInt(args[1])){
                    player.sendMessage("[scarlet]When filter is used, second arg 'page' must be a number.");
                    return;
                }
                page = Strings.parseInt(args[1]);
            }

            Seq<Map> builtin = maps.defaultMaps();
            if(filter != null && !filter.trim().isEmpty()){
                String lowered = filter.toLowerCase();
                builtin = builtin.select(map -> map.plainName().toLowerCase().contains(lowered));
            }

            if(builtin.isEmpty()){
                player.sendMessage("[scarlet]No built-in maps matched your query.");
                return;
            }

            int perPage = 10;
            int totalPages = Mathf.ceil((float)builtin.size / perPage);
            if(page < 1 || page > totalPages){
                player.sendMessage("[scarlet]'page' must be between [orange]1[scarlet] and [orange]" + totalPages + "[scarlet].");
                return;
            }

            int start = (page - 1) * perPage;
            int end = Math.min(start + perPage, builtin.size);

            StringBuilder out = new StringBuilder();
            if(filter != null && !filter.trim().isEmpty()){
                out.append("[lightgray]Filter: [accent]").append(filter).append('\n');
            }
            out.append(Strings.format("[orange]-- Built-in Maps [lightgray]@[gray]/[lightgray]@[orange] --\n\n", page, totalPages));
            for(int i = start; i < end; i++){
                Map map = builtin.get(i);
                out.append("[lightgray] ").append(i + 1).append(". [accent]").append(map.plainName()).append('\n');
            }
            out.append("\n[lightgray]< [accent]").append(page).append("[lightgray]/[accent]").append(totalPages).append("[lightgray] >");
            player.sendMessage(out.toString());
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/map/mapInfo.kts (partial)
        clientCommands.<Player>register("mapinfo", "Show current map details.", (args, player) -> {
            if(state.map == null){
                player.sendMessage("[scarlet]No map is currently loaded.");
                return;
            }

            String author = state.map.author();
            if(author == null || author.trim().isEmpty()){
                author = "Unknown";
            }

            String description = state.map.description();
            if(description == null){
                description = "";
            }

            String mode = state.rules.pvp ? "PvP" : state.rules.attackMode ? "Attack" : state.rules.infiniteResources ? "Sandbox" : "Survival";

            StringBuilder out = new StringBuilder();
            out.append("[green]==== [white]Map Info[] ====\n");
            out.append("[yellow]Name: [white]").append(state.map.plainName()).append('\n');
            out.append("[yellow]Author: [white]").append(author).append('\n');
            out.append("[yellow]Mode: [white]").append(mode).append('\n');
            if(!description.trim().isEmpty()){
                out.append("[yellow]Description:[white]\n").append(wrapMapInfoText(description.trim(), 52)).append('\n');
            }
            out.append("[green]=======================");
            player.sendMessage(out.toString());
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/map/resourceHelper.kts (partial, resource site map listing)
        clientCommands.<Player>register("sitemaps", "[search/page] [page]", "List maps from resource site api.mindustry.top.", (args, player) -> {
            String search = "";
            int page = 1;

            if(args.length == 1){
                if(Strings.canParseInt(args[0])){
                    page = Strings.parseInt(args[0]);
                }else{
                    search = args[0];
                }
            }else if(args.length >= 2){
                search = args[0];
                if(!Strings.canParseInt(args[1])){
                    player.sendMessage("[scarlet]When search is used, second arg 'page' must be a number.");
                    return;
                }
                page = Strings.parseInt(args[1]);
            }

            if(page < 1){
                player.sendMessage("[scarlet]'page' must be >= 1.");
                return;
            }

            String finalSearch = search == null ? "" : search.trim();
            int finalPage = page;
            fetchResourceSiteMaps(finalSearch, infos -> Core.app.post(() -> {
                if(infos.isEmpty()){
                    player.sendMessage("[scarlet]No resource-site maps matched your query.");
                    return;
                }

                int perPage = 10;
                int totalPages = Math.max(1, Mathf.ceil((float)infos.size / perPage));
                if(finalPage > totalPages){
                    player.sendMessage("[scarlet]'page' must be between [orange]1[scarlet] and [orange]" + totalPages + "[scarlet].");
                    return;
                }

                int start = (finalPage - 1) * perPage;
                int end = Math.min(start + perPage, infos.size);

                StringBuilder out = new StringBuilder();
                out.append(Strings.format("[orange]-- Resource Maps [lightgray]@[gray]/[lightgray]@[orange] --\n", finalPage, totalPages));
                out.append("[lightgray]Search: [accent]").append(finalSearch.isEmpty() ? "<all>" : finalSearch).append("\n\n");
                for(int i = start; i < end; i++){
                    ResourceMapInfo info = infos.get(i);
                    out.append("[lightgray] ").append(i + 1).append(". [accent]").append(info.id)
                    .append("[lightgray] | ").append(info.name)
                    .append("[lightgray] | ").append(info.mode)
                    .append("[lightgray] | by ").append(info.author)
                    .append('\n');
                }
                out.append("\n[lightgray]Use [accent]/votemap <id>[] or [accent]/host <id>[] to switch.");
                player.sendMessage(out.toString());
            }), err -> Core.app.post(() -> player.sendMessage("[scarlet]Failed to fetch resource maps: " + err)));
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/map/resourceHelper.kts (extended: api root config)
        clientCommands.<Player>register("siteapicfg", "<url/default>", "Admin: set resource-site API root.", (args, player) -> {
            if(!player.admin){
                player.sendMessage("[scarlet]Admin required.");
                return;
            }

            String value = args[0] == null ? "" : args[0].trim();
            if(value.isEmpty()){
                player.sendMessage("[scarlet]Usage: /siteapicfg <url/default>");
                return;
            }

            if("default".equalsIgnoreCase(value)){
                resourceSiteApiRoot = "https://api.mindustry.top";
            }else{
                resourceSiteApiRoot = value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
            }

            Core.settings.put("wayzer-resource-api-root", resourceSiteApiRoot);
            Core.settings.forceSave();
            player.sendMessage("[green]Resource-site API root set to [accent]" + resourceSiteApiRoot);
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/maps.kts + scripts/wayzer/map/resourceHelper.kts (partial)
        clientCommands.<Player>register("host", "<map/id>", "Admin: immediately change map (local map name/index or resource-site id).", (args, player) -> {
            if(!player.admin){
                player.sendMessage("[scarlet]Admin required.");
                return;
            }

            String token = args[0] == null ? "" : args[0].trim();
            if(token.isEmpty()){
                player.sendMessage("[scarlet]Usage: /host <map/id>");
                return;
            }

            if(isResourceMapIdToken(token)){
                int resourceId = Strings.parseInt(token);
                Call.sendMessage("[yellow]Downloading resource-site map [accent]" + resourceId + "[yellow]...");
                fetchResourceSiteMapById(resourceId, map -> Core.app.post(() -> {
                    try{
                        world.loadMap(map, map.applyRules(state.rules.mode()));
                        logic.play();
                        Call.sendMessage("[green]Loaded resource-site map [accent]" + map.plainName() + "[green] (id=" + resourceId + ").");
                    }catch(Throwable t){
                        Log.err("Failed to load resource-site map " + resourceId + ".", t);
                        Call.sendMessage("[scarlet]Failed to load resource-site map id=" + resourceId + ".");
                    }
                }), err -> Core.app.post(() -> Call.sendMessage("[scarlet]Failed to download resource-site map id=" + resourceId + ": " + err)));
                return;
            }

            Map target = resolveVoteMap(token);
            if(target == null){
                player.sendMessage("[scarlet]Map not found. Use [orange]/maps[] or [orange]/sitemaps[].");
                return;
            }

            try{
                world.loadMap(target, target.applyRules(state.rules.mode()));
                logic.play();
                Call.sendMessage("[green]Host changed map to [accent]" + target.plainName() + "[green].");
            }catch(Throwable t){
                Log.err("Failed to host local map " + token + ".", t);
                player.sendMessage("[scarlet]Failed to load map: " + token);
            }
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/cmds/serverStatus.kts
        clientCommands.<Player>register("status", "Show server status.", (args, player) -> {
            if(state.isMenu()){
                player.sendMessage("[scarlet]Server is not hosting a game.");
                return;
            }

            String mode = state.rules.pvp ? "PvP" : state.rules.attackMode ? "Attack" : state.rules.infiniteResources ? "Sandbox" : "Survival";
            int tps = Core.graphics.getFramesPerSecond();
            long heapMb = Core.app.getJavaHeap() / 1024 / 1024;
            int allBans = admins.getBanned().size + admins.getBannedIPs().size + integrityBans.size;

            StringBuilder out = new StringBuilder();
            out.append("[green]Server Status[]\n");
            out.append("[green]Map: [yellow]").append(state.map.plainName()).append("[green], Mode: [yellow]").append(mode).append("[green], Wave: [yellow]").append(state.wave).append('\n');
            out.append("[green]TPS: [yellow]").append(tps).append("[green], Heap: [yellow]").append(heapMb).append(" MB[green], Uptime: [yellow]").append(formatUptime()).append('\n');
            out.append("[green]Units: [yellow]").append(Groups.unit.size()).append("[green], Players: [yellow]").append(Groups.player.size()).append('\n');
            out.append("[yellow]Total bans: ").append(allBans);
            player.sendMessage(out.toString());
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/cmds/clearUnit.kts
        clientCommands.<Player>register("clearunit", "Kill all units in the world.", (args, player) -> {
            if(!player.admin){
                player.sendMessage("[scarlet]Admin required.");
                return;
            }

            Call.sendMessage("[green]Admin used clearunit.");
            for(Unit unit : Groups.unit.copy(new Seq<>())){
                unit.kill();
            }
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/cmds/helpfulCmd.kts
        clientCommands.<Player>register("showcolor", "Show all named colors.", (args, player) -> {
            StringBuilder out = new StringBuilder();
            int[] count = {0};
            Colors.getColors().each((name, color) -> {
                out.append("[#").append(color.toString()).append("]").append(name).append("[]");
                count[0]++;
                if(count[0] % 6 == 0){
                    out.append('\n');
                }else{
                    out.append(", ");
                }
            });
            player.sendMessage(out.toString());
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/cmds/helpfulCmd.kts
        clientCommands.<Player>register("dosbanclear", "Clear DOS blacklist.", (args, player) -> {
            if(!player.admin){
                player.sendMessage("[scarlet]Admin required.");
                return;
            }

            player.sendMessage("[yellow]DOS blacklist: " + admins.dosBlacklist.toString());
            admins.dosBlacklist.clear();
            player.sendMessage("[green]DOS blacklist cleared.");
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/cmds/helpfulCmd.kts
        clientCommands.<Player>register("showeffect", "<type> [radius] [color]", "Show an effect at your position.", (args, player) -> {
            if(!player.admin){
                player.sendMessage("[scarlet]Admin required.");
                return;
            }

            Effect effect = wayzerEffects.get(args[0].toLowerCase());
            if(effect == null){
                player.sendMessage("[scarlet]Unknown effect. Use [orange]/showeffectlist[] to inspect names.");
                return;
            }

            float radius = 10f;
            if(args.length > 1){
                try{
                    radius = Float.parseFloat(args[1]);
                }catch(Throwable t){
                    player.sendMessage("[scarlet]'radius' must be a number.");
                    return;
                }
            }

            Color color = Color.red;
            if(args.length > 2){
                try{
                    color = Color.valueOf(args[2]);
                }catch(Throwable t){
                    player.sendMessage("[scarlet]'color' must be a valid hex or named color.");
                    return;
                }
            }

            Call.effect(effect, player.x, player.y, radius, color);
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/cmds/helpfulCmd.kts
        clientCommands.<Player>register("showeffectlist", "[page]", "List effect names for /showeffect.", (args, player) -> {
            if(!player.admin){
                player.sendMessage("[scarlet]Admin required.");
                return;
            }

            if(args.length > 0 && !Strings.canParseInt(args[0])){
                player.sendMessage("[scarlet]'page' must be a number.");
                return;
            }

            Seq<String> names = wayzerEffects.keys().toSeq();
            names.sort();

            int perPage = 12;
            int totalPages = Math.max(1, Mathf.ceil((float)names.size / perPage));
            int page = args.length > 0 ? Strings.parseInt(args[0]) : 1;
            if(page < 1 || page > totalPages){
                player.sendMessage("[scarlet]'page' must be between [orange]1[scarlet] and [orange]" + totalPages + "[scarlet].");
                return;
            }

            int start = (page - 1) * perPage;
            int end = Math.min(start + perPage, names.size);

            StringBuilder out = new StringBuilder();
            out.append("[orange]-- Effects [lightgray]").append(page).append("[gray]/[lightgray]").append(totalPages).append("[orange] --\n\n");
            for(int i = start; i < end; i++){
                out.append("[lightgray] ").append(i + 1).append(". [accent]").append(names.get(i)).append('\n');
            }
            player.sendMessage(out.toString());
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/cmds/spawnMob.kts
        clientCommands.<Player>register("spawn", "[typeId] [teamId] [amount] [armor]", "Spawn units by unit type id.", (args, player) -> {
            if(!player.admin){
                player.sendMessage("[scarlet]Admin required.");
                return;
            }

            Seq<UnitType> types = new Seq<>();
            for(UnitType type : content.units()){
                if(!type.internal){
                    types.add(type);
                }
            }

            if(args.length == 0 || !Strings.canParseInt(args[0])){
                StringBuilder out = new StringBuilder("[yellow]Unit type IDs: ");
                for(int i = 0; i < types.size; i++){
                    if(i > 0) out.append("[lightgray], ");
                    out.append("[accent]").append(i).append("[lightgray]=").append(types.get(i).name);
                }
                player.sendMessage(out.toString());
                return;
            }

            int typeId = Strings.parseInt(args[0]);
            if(typeId < 0 || typeId >= types.size){
                player.sendMessage("[scarlet]Invalid type id.");
                return;
            }

            Team team = Team.sharded;
            if(args.length > 1){
                if(!Strings.canParseInt(args[1])){
                    player.sendMessage("[scarlet]'teamId' must be a number.");
                    return;
                }
                int teamId = Strings.parseInt(args[1]);
                if(teamId < 0 || teamId >= Team.all.length){
                    player.sendMessage("[scarlet]'teamId' must be between [orange]0[] and [orange]" + (Team.all.length - 1) + "[scarlet].");
                    return;
                }
                team = Team.all[teamId];
            }

            int amount = 1;
            if(args.length > 2){
                if(!Strings.canParseInt(args[2])){
                    player.sendMessage("[scarlet]'amount' must be a number.");
                    return;
                }
                amount = Mathf.clamp(Strings.parseInt(args[2]), 1, 200);
            }

            Float armor = null;
            if(args.length > 3){
                try{
                    armor = Float.parseFloat(args[3]);
                }catch(Throwable t){
                    player.sendMessage("[scarlet]'armor' must be a number.");
                    return;
                }
            }

            UnitType type = types.get(typeId);
            float spawnX = player.x, spawnY = player.y;
            if(player.unit() != null){
                spawnX = player.unit().x;
                spawnY = player.unit().y;
            }else if(team.core() != null){
                spawnX = team.core().x;
                spawnY = team.core().y;
            }

            for(int i = 0; i < amount; i++){
                Unit unit = type.create(team);
                if(armor != null){
                    unit.armor = armor;
                }
                unit.set(spawnX, spawnY);
                unit.add();
            }

            player.sendMessage("[green]Spawned [yellow]" + amount + "[green] [yellow]" + type.name + "[green] for [yellow]" + team.name + "[green].");
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/cmds/gatherTp.kts
        clientCommands.<Player>register("gather", "[message...]", "Send a gather request at your position.", (args, player) -> {
            if(player.dead() || !player.unit().type.targetable){
                player.sendMessage("[scarlet]Your current unit cannot use gather.");
                return;
            }

            if(Time.timeSinceMillis(gatherLastTime) < 1000L * 10L){
                player.sendMessage("[scarlet]A gather request was just created. Wait 10 seconds.");
                return;
            }

            Tile tile = player.tileOn();
            if(tile == null){
                player.sendMessage("[scarlet]You must be on a valid tile.");
                return;
            }

            gatherTileX = tile.x;
            gatherTileY = tile.y;
            gatherLastTime = Time.millis();

            String note = args.length > 0 && args[0] != null && !args[0].trim().isEmpty() ? " [lightgray]\"" + args[0] + "\"" : "";
            Call.sendMessage("[accent]" + player.name + "[] requested gather at [orange](" + tile.x + "," + tile.y + ")[]" + note);
            Call.sendMessage("[lightgray]Use [orange]/go[] to move to the latest gather point.");
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/cmds/gatherTp.kts
        clientCommands.<Player>register("go", "Teleport to the latest gather point.", (args, player) -> {
            if(gatherTileX == Integer.MIN_VALUE){
                player.sendMessage("[scarlet]No gather point exists yet.");
                return;
            }

            if(player.dead() || player.unit() == null){
                player.sendMessage("[scarlet]You cannot use /go while dead.");
                return;
            }

            Tile tile = world.tile(gatherTileX, gatherTileY);
            if(tile == null){
                player.sendMessage("[scarlet]The gather point is no longer valid.");
                return;
            }

            if(!canGatherTeleport(player.unit(), tile)){
                player.sendMessage("[scarlet]Target position is not safe for this unit.");
                return;
            }

            player.unit().set(tile.worldx(), tile.worldy());
            player.unit().snapInterpolation();
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/cmds/gatherTp.kts
        clientCommands.<Player>register("tp", "[x] [y]", "Teleport to your mouse position (or custom world coordinates).", (args, player) -> {
            if(!player.admin){
                player.sendMessage("[scarlet]Admin required.");
                return;
            }

            if(player.dead() || player.unit() == null){
                player.sendMessage("[scarlet]You cannot use /tp while dead.");
                return;
            }

            float tx = player.mouseX;
            float ty = player.mouseY;

            if(args.length >= 2){
                try{
                    tx = Float.parseFloat(args[0]);
                    ty = Float.parseFloat(args[1]);
                }catch(Throwable t){
                    player.sendMessage("[scarlet]Usage: /tp [x] [y]");
                    return;
                }
            }

            player.unit().set(tx, ty);
            player.unit().snapInterpolation();
            player.sendMessage("[green]Teleported.");
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/ext/observer.kts (partial)
        clientCommands.<Player>register("ob", "[off/teamId]", "Toggle observer mode.", (args, player) -> {
            if(args.length > 0 && ("off".equalsIgnoreCase(args[0]) || "quit".equalsIgnoreCase(args[0]))){
                if(isObserver(player) && isObserverExitLocked()){
                    player.sendMessage("[scarlet]Observer mode cannot be disabled during an active match.");
                    return;
                }

                Team team = assignTeam(player);
                if(team == Team.derelict){
                    player.sendMessage("[scarlet]No available team slot.");
                    return;
                }
                player.team(team);
                player.clearUnit();
                player.checkSpawn();
                player.sendMessage("[green]Observer mode disabled. Returned to [yellow]" + team.name + "[green].");
                return;
            }

            if(player.team() == Team.derelict){
                if(isObserverExitLocked()){
                    player.sendMessage("[scarlet]Observer mode cannot be disabled during an active match.");
                    return;
                }

                Team team = assignTeam(player);
                if(args.length > 0 && Strings.canParseInt(args[0])){
                    int teamId = Strings.parseInt(args[0]);
                    if(teamId >= 0 && teamId < Team.all.length && Team.all[teamId] != Team.derelict){
                        Team selected = Team.all[teamId];
                        if(isTeamOccupied(selected, player, Groups.player)){
                            player.sendMessage("[scarlet]That team already has a player.");
                            return;
                        }
                        team = selected;
                    }
                }

                if(team == Team.derelict){
                    player.sendMessage("[scarlet]No available team slot.");
                    return;
                }
                player.team(team);
                player.clearUnit();
                player.checkSpawn();
                player.sendMessage("[green]Observer mode disabled. Returned to [yellow]" + team.name + "[green].");
            }else{
                if(isObserverExitLocked()){
                    player.sendMessage("[scarlet]Team changes are disabled during an active match.");
                    return;
                }
                player.team(Team.derelict);
                player.clearUnit();
                player.sendMessage("[yellow]Observer mode enabled. Use [accent]/ob off[] to return.");
            }
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/cmds/voteOb.kts (partial, admin forceOB only)
        clientCommands.<Player>register("forceob", "<player> [off/teamId]", "Admin command: toggle observer mode for a target player.", (args, player) -> {
            if(!player.admin){
                player.sendMessage("[scarlet]Admin required.");
                return;
            }

            Player target;
            if(args[0].length() > 1 && args[0].startsWith("#") && Strings.canParseInt(args[0].substring(1))){
                int id = Strings.parseInt(args[0].substring(1));
                target = Groups.player.find(p -> p.id() == id);
            }else{
                target = findPlayerByUid(args[0]);
                if(target == null){
                    target = Groups.player.find(p -> matchesDisplayOrBaseName(p, args[0]));
                }
            }

            if(target == null){
                player.sendMessage("[scarlet]Player not found: [orange]" + args[0]);
                return;
            }

            boolean forceObserver = true;
            Team restoreTeam = assignTeam(target);
            if(args.length > 1){
                if("off".equalsIgnoreCase(args[1]) || "quit".equalsIgnoreCase(args[1])){
                    forceObserver = false;
                }else if(Strings.canParseInt(args[1])){
                    int teamId = Strings.parseInt(args[1]);
                    if(teamId < 0 || teamId >= Team.all.length){
                        player.sendMessage("[scarlet]'teamId' must be between [orange]0[] and [orange]" + (Team.all.length - 1) + "[scarlet].");
                        return;
                    }
                    restoreTeam = Team.all[teamId];
                    forceObserver = restoreTeam == Team.derelict;
                }
            }else if(target.team() == Team.derelict){
                forceObserver = false;
            }

            if(forceObserver){
                if(!isObserver(target) && isObserverExitLocked()){
                    player.sendMessage("[scarlet]Team changes are disabled during an active match.");
                    return;
                }
                target.team(Team.derelict);
                target.clearUnit();
                target.sendMessage("[scarlet]An admin has set you to observer mode.");
                Call.sendMessage("[yellow]Admin [accent]" + player.name + "[yellow] set [accent]" + target.name + "[yellow] to observer mode.");
            }else{
                if(isObserver(target) && isObserverExitLocked()){
                    player.sendMessage("[scarlet]Observer players cannot leave observer mode during an active match.");
                    return;
                }

                if(restoreTeam == Team.derelict){
                    restoreTeam = assignTeam(target);
                }

                if(restoreTeam == Team.derelict){
                    player.sendMessage("[scarlet]No available team slot to restore this player.");
                    return;
                }

                if(isTeamOccupied(restoreTeam, target, Groups.player)){
                    player.sendMessage("[scarlet]That team already has a player.");
                    return;
                }

                target.team(restoreTeam);
                target.clearUnit();
                target.checkSpawn();
                target.sendMessage("[green]An admin restored you from observer mode.");
                Call.sendMessage("[yellow]Admin [accent]" + player.name + "[yellow] restored [accent]" + target.name + "[yellow] to [accent]" + restoreTeam.name + "[yellow].");
            }
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/cmds/restart.kts
        clientCommands.<Player>register("restart", "[--now] [message...]", "Schedule server restart after this game.", (args, player) -> {
            if(!player.admin){
                player.sendMessage("[scarlet]Admin required.");
                return;
            }

            boolean now = args.length > 0 && "--now".equalsIgnoreCase(args[0]);
            String message = "";

            if(now){
                if(args.length > 1){
                    message = args[1];
                }
            }else if(args.length > 0){
                message = args[0];
            }

            scheduleRestart(message, now);
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/cmds/jsCmd.kts
        clientCommands.<Player>register("js", "<script...>", "UNSAFE: run server JavaScript as admin.", (args, player) -> {
            if(!player.admin){
                player.sendMessage("[scarlet]Admin required.");
                return;
            }

            player.sendMessage("[red]JS> [lightgray]" + args[0]);
            mindustry.Vars.player = player;
            try{
                Object result = mods.getScripts().runConsole(args[0]);
                player.sendMessage(String.valueOf(result));
            }catch(Throwable t){
                player.sendMessage("[scarlet]JS error: " + t.getMessage());
            }finally{
                mindustry.Vars.player = null;
            }
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/ext/welcomeMsg.kts
        clientCommands.<Player>register("welcomecfg", "<on/off>", "Toggle WayZer-style custom welcome broadcast.", (args, player) -> {
            if(!player.admin){
                player.sendMessage("[scarlet]Admin required.");
                return;
            }

            if(!"on".equalsIgnoreCase(args[0]) && !"off".equalsIgnoreCase(args[0])){
                player.sendMessage("[scarlet]Usage: /welcomecfg <on/off>");
                return;
            }

            customWelcomeEnabled = "on".equalsIgnoreCase(args[0]);
            Config.showConnectMessages.set(!customWelcomeEnabled);
            Core.settings.put("wayzer-custom-welcome", customWelcomeEnabled);
            Core.settings.forceSave();
            player.sendMessage("[green]Custom welcome is now [yellow]" + (customWelcomeEnabled ? "on" : "off") + "[green].");
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/ext/welcomeMsg.kts
        clientCommands.<Player>register("welcometpl", "<welcome/join/leave> <text...>", "Set custom welcome template text.", (args, player) -> {
            if(!player.admin){
                player.sendMessage("[scarlet]Admin required.");
                return;
            }

            String key = args[0].toLowerCase();
            switch(key){
                case "welcome" -> {
                    welcomeTemplate = args[1];
                    Core.settings.put("wayzer-welcome-template", welcomeTemplate);
                }
                case "join" -> {
                    welcomeJoinTemplate = args[1];
                    Core.settings.put("wayzer-welcome-join-template", welcomeJoinTemplate);
                }
                case "leave" -> {
                    welcomeLeaveTemplate = args[1];
                    Core.settings.put("wayzer-welcome-leave-template", welcomeLeaveTemplate);
                }
                default -> {
                    player.sendMessage("[scarlet]Usage: /welcometpl <welcome/join/leave> <text...>");
                    return;
                }
            }
            Core.settings.forceSave();
            player.sendMessage("[green]Updated template [yellow]" + key + "[green].");
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/ext/alert.kts
        clientCommands.<Player>register("alertlist", "Show alert broadcast list.", (args, player) -> {
            if(alertMessages.isEmpty()){
                player.sendMessage("[scarlet]Alert list is empty.");
                return;
            }

            StringBuilder out = new StringBuilder();
            out.append("[orange]-- Alert List --\n");
            for(int i = 0; i < alertMessages.size; i++){
                out.append("[lightgray] ").append(i + 1).append(". ").append(alertMessages.get(i)).append('\n');
            }
            out.append("[lightgray]Enabled: [accent]").append(alertEnabled).append("[lightgray], Interval: [accent]").append(alertIntervalSeconds).append("s");
            player.sendMessage(out.toString());
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/ext/alert.kts
        clientCommands.<Player>register("alerttoggle", "<on/off>", "Toggle periodic alert broadcasts.", (args, player) -> {
            if(!player.admin){
                player.sendMessage("[scarlet]Admin required.");
                return;
            }
            if(!"on".equalsIgnoreCase(args[0]) && !"off".equalsIgnoreCase(args[0])){
                player.sendMessage("[scarlet]Usage: /alerttoggle <on/off>");
                return;
            }

            alertEnabled = "on".equalsIgnoreCase(args[0]);
            saveAlertConfig();
            player.sendMessage("[green]Alert broadcast is now [yellow]" + (alertEnabled ? "on" : "off") + "[green].");
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/ext/alert.kts
        clientCommands.<Player>register("alertinterval", "<seconds>", "Set periodic alert interval in seconds.", (args, player) -> {
            if(!player.admin){
                player.sendMessage("[scarlet]Admin required.");
                return;
            }
            if(!Strings.canParseInt(args[0])){
                player.sendMessage("[scarlet]'seconds' must be a number.");
                return;
            }

            int seconds = Strings.parseInt(args[0]);
            if(seconds < 30 || seconds > 86400){
                player.sendMessage("[scarlet]'seconds' must be between [orange]30[] and [orange]86400[scarlet].");
                return;
            }

            alertIntervalSeconds = seconds;
            saveAlertConfig();
            player.sendMessage("[green]Alert interval set to [yellow]" + alertIntervalSeconds + "[green] seconds.");
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/ext/alert.kts
        clientCommands.<Player>register("alertadd", "<message...>", "Add a periodic alert message.", (args, player) -> {
            if(!player.admin){
                player.sendMessage("[scarlet]Admin required.");
                return;
            }

            alertMessages.add(args[0]);
            saveAlertConfig();
            player.sendMessage("[green]Added alert #[yellow]" + alertMessages.size + "[green].");
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/ext/alert.kts
        clientCommands.<Player>register("alertremove", "<index>", "Remove a periodic alert message by index.", (args, player) -> {
            if(!player.admin){
                player.sendMessage("[scarlet]Admin required.");
                return;
            }
            if(!Strings.canParseInt(args[0])){
                player.sendMessage("[scarlet]'index' must be a number.");
                return;
            }

            int index = Strings.parseInt(args[0]) - 1;
            if(index < 0 || index >= alertMessages.size){
                player.sendMessage("[scarlet]'index' out of range.");
                return;
            }

            alertMessages.remove(index);
            if(alertIndex >= alertMessages.size){
                alertIndex = 0;
            }
            saveAlertConfig();
            player.sendMessage("[green]Removed alert #[yellow]" + (index + 1) + "[green].");
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/ext/alert.kts
        clientCommands.<Player>register("alertnext", "Broadcast next alert immediately.", (args, player) -> {
            if(!player.admin){
                player.sendMessage("[scarlet]Admin required.");
                return;
            }
            if(alertMessages.isEmpty()){
                player.sendMessage("[scarlet]Alert list is empty.");
                return;
            }
            sendNextAlert();
            player.sendMessage("[green]Sent next alert.");
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/ext/goServer.kts
        clientCommands.<Player>register("goserver", "[name]", "Transfer to another configured server.", (args, player) -> {
            if(args.length == 0){
                if(transferServers.isEmpty()){
                    player.sendMessage("[scarlet]No transfer servers configured.");
                    return;
                }

                StringBuilder out = new StringBuilder();
                out.append("[orange]-- Transfer Servers --\n");
                for(var entry : transferServers){
                    TransferServerInfo info = entry.value;
                    out.append("[gold]").append(info.name).append("[lightgray]: ").append(info.desc)
                    .append(" [gray](").append(info.address).append(':').append(info.port).append(")\n");
                }
                player.sendMessage(out.toString());
                return;
            }

            TransferServerInfo info = findTransferServer(args[0]);
            if(info == null){
                player.sendMessage("[scarlet]Unknown transfer server name.");
                return;
            }

            Call.connect(player.con, info.address, info.port);
            Call.sendMessage("[cyan][-][salmon]" + player.name + "[salmon] transferred to [accent]" + info.name + "[salmon] (/goserver " + info.name + ")");
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/ext/goServer.kts
        clientCommands.<Player>register("goserveradd", "<name> <desc;address[:port]>", "Add a transfer server entry.", (args, player) -> {
            if(!player.admin){
                player.sendMessage("[scarlet]Admin required.");
                return;
            }

            TransferServerInfo info = parseTransferServer(args[0], args[1]);
            if(info == null){
                player.sendMessage("[scarlet]Invalid format. Use: desc;address[:port]");
                return;
            }

            transferServers.put(info.name, info);
            saveTransferServers();
            player.sendMessage("[green]Added transfer server [yellow]" + info.name + "[green].");
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/ext/goServer.kts
        clientCommands.<Player>register("goserverremove", "<name>", "Remove a transfer server entry.", (args, player) -> {
            if(!player.admin){
                player.sendMessage("[scarlet]Admin required.");
                return;
            }

            String key = findTransferServerKey(args[0]);
            if(key == null){
                player.sendMessage("[scarlet]No such transfer server.");
                return;
            }

            transferServers.remove(key);
            saveTransferServers();
            player.sendMessage("[green]Removed transfer server [yellow]" + key + "[green].");
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/ext/tpsLimit.kts
        clientCommands.<Player>register("tpslimit", "<on/off>", "Toggle WayZer TPS delta cap.", (args, player) -> {
            if(!player.admin){
                player.sendMessage("[scarlet]Admin required.");
                return;
            }
            if(!"on".equalsIgnoreCase(args[0]) && !"off".equalsIgnoreCase(args[0])){
                player.sendMessage("[scarlet]Usage: /tpslimit <on/off>");
                return;
            }

            tpsLimitEnabled = "on".equalsIgnoreCase(args[0]);
            saveTpsLimitConfig();
            applyTpsLimit();
            player.sendMessage("[green]TPS limit is now [yellow]" + (tpsLimitEnabled ? "on" : "off") + "[green], maxDelta=[yellow]" + Strings.autoFixed(tpsLimitMaxDelta, 2));
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/ext/tpsLimit.kts
        clientCommands.<Player>register("tpslimitmax", "<delta>", "Set TPS delta cap max value.", (args, player) -> {
            if(!player.admin){
                player.sendMessage("[scarlet]Admin required.");
                return;
            }

            float value;
            try{
                value = Float.parseFloat(args[0]);
            }catch(Throwable t){
                player.sendMessage("[scarlet]'delta' must be a number.");
                return;
            }

            tpsLimitMaxDelta = Mathf.clamp(value, 0.1f, 60f);
            saveTpsLimitConfig();
            applyTpsLimit();
            player.sendMessage("[green]TPS max delta set to [yellow]" + Strings.autoFixed(tpsLimitMaxDelta, 2));
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/ext/tpsLimit.kts
        clientCommands.<Player>register("tpslimitstatus", "Show TPS limit status.", (args, player) -> {
            player.sendMessage("[lightgray]TPS limit enabled: [accent]" + tpsLimitEnabled + "[lightgray], maxDelta: [accent]" + Strings.autoFixed(tpsLimitMaxDelta, 2));
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/cmds/vote.kts
        clientCommands.<Player>register("votegameover", "Start a vote to surrender / end this game.", (args, player) -> {
            if(!state.rules.canGameOver){
                player.sendMessage("[scarlet]This map does not allow game over vote.");
                return;
            }

            if(state.rules.pvp){
                Team team = player.team();
                TeamData data = state.teams.get(team);
                if(data == null || data.cores.isEmpty()){
                    player.sendMessage("[scarlet]Your team is already defeated.");
                    return;
                }

                startGenericVote(
                player,
                "[yellow]Surrender vote for team [accent]" + team.coloredName() + "[yellow] (80% required).",
                p -> p.team() == team,
                count -> Math.max(1, Mathf.ceil(count * 0.8f)),
                false,
                () -> {
                    for(var core : team.data().cores){
                        if(core.team == team) core.kill();
                    }
                });
                return;
            }

            startGenericVote(
            player,
            "[yellow]Vote to end current game.",
            p -> true,
            count -> votesRequired(),
            true,
            () -> {
                for(var core : player.team().cores()){
                    Time.run(Mathf.random(60f * 3f), core::kill);
                }
                Events.fire(new GameOverEvent(state.rules.waveTeam));
            });
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/cmds/vote.kts
        clientCommands.<Player>register("voteskipwave", "[count]", "Vote to skip multiple waves quickly.", (args, player) -> {
            if(Groups.player.contains(p -> p.team() == state.rules.waveTeam)){
                player.sendMessage("[scarlet]Skip-wave vote is disabled in this mode.");
                return;
            }

            int count = 10;
            if(args.length > 0){
                if(!Strings.canParseInt(args[0])){
                    player.sendMessage("[scarlet]'count' must be a number.");
                    return;
                }
                count = Mathf.clamp(Strings.parseInt(args[0]), 1, 50);
            }

            int finalCount = count;
            startGenericVote(
            player,
            "[yellow]Vote to skip [accent]" + finalCount + "[yellow] waves.",
            p -> true,
            c -> votesRequired(),
            true,
            () -> {
                for(int i = 0; i < finalCount; i++){
                    logic.runWave();
                }
                Call.sendMessage("[green]Skipped [accent]" + finalCount + "[green] waves.");
            });
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/cmds/vote.kts
        clientCommands.<Player>register("voteclearplans", "Vote to clear your team's build plans.", (args, player) -> {
            Team team = player.team();
            startGenericVote(
            player,
            "[yellow]Vote to clear build plans for team [accent]" + team.coloredName() + "[yellow] (2/5 required).",
            p -> p.team() == team,
            count -> Math.max(1, Mathf.ceil(count * 0.4f)),
            false,
            () -> team.data().plans.clear());
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/cmds/vote.kts
        clientCommands.<Player>register("votetext", "<content...>", "Start a custom text vote.", (args, player) -> {
            String text = args[0] == null ? "" : args[0].trim();
            if(text.isEmpty()){
                player.sendMessage("[scarlet]Vote content cannot be empty.");
                return;
            }

            startGenericVote(
            player,
            "[yellow]Custom vote: [green]" + text,
            p -> true,
            c -> votesRequired(),
            false,
            () -> Call.sendMessage("[green]Custom vote passed."));
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/cmds/voteMap.kts + scripts/wayzer/map/resourceHelper.kts (partial)
        clientCommands.<Player>register("votemap", "<map...>", "Vote to change current map.", (args, player) -> {
            String token = args[0] == null ? "" : args[0].trim();
            if(isResourceMapIdToken(token)){
                int resourceId = Strings.parseInt(token);
                startGenericVote(
                player,
                "[yellow]Vote to change map to resource-site id [accent]" + resourceId,
                p -> true,
                c -> votesRequired(),
                true,
                () -> {
                    Call.sendMessage("[yellow]Downloading voted resource-site map [accent]" + resourceId + "[yellow]...");
                    fetchResourceSiteMapById(resourceId, map -> Core.app.post(() -> {
                        try{
                            world.loadMap(map, map.applyRules(state.rules.mode()));
                            logic.play();
                            Call.sendMessage("[green]Loaded resource-site map [accent]" + map.plainName() + "[green] (id=" + resourceId + ").");
                        }catch(Throwable t){
                            Log.err("Failed to load voted resource-site map " + resourceId + ".", t);
                            Call.sendMessage("[scarlet]Failed to load voted resource-site map id=" + resourceId + ".");
                        }
                    }), err -> Core.app.post(() -> Call.sendMessage("[scarlet]Failed to download resource-site map id=" + resourceId + ": " + err)));
                });
                return;
            }

            Map target = resolveVoteMap(args[0]);
            if(target == null){
                player.sendMessage("[scarlet]Map not found. Use [orange]/maps[] or [orange]/sitemaps[].");
                return;
            }

            Map selected = target;
            startGenericVote(
            player,
            "[yellow]Vote to change map to [accent]" + selected.plainName(),
            p -> true,
            c -> votesRequired(),
            true,
            () -> {
                Call.sendMessage("[yellow]Loading voted map: [accent]" + selected.plainName());
                Core.app.post(() -> {
                    try{
                        world.loadMap(selected, selected.applyRules(state.rules.mode()));
                        logic.play();
                    }catch(Throwable t){
                        Log.err("Failed to load voted map.", t);
                        Call.sendMessage("[scarlet]Failed to load map: " + selected.plainName());
                    }
                });
            });
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/cmds/voteMap.kts (partial, rollback vote)
        clientCommands.<Player>register("votesaves", "List available save slots for /voterollback.", (args, player) -> {
            Seq<Fi> slots = saveDirectory.findAll(file -> file.extension().equals(saveExtension));
            if(slots.isEmpty()){
                player.sendMessage("[scarlet]No save slots found.");
                return;
            }

            slots.sort(Structs.comparing(Fi::nameWithoutExtension));
            StringBuilder out = new StringBuilder();
            out.append("[orange]-- Save Slots --\n");
            for(Fi file : slots){
                out.append("[lightgray]| [accent]").append(file.nameWithoutExtension()).append('\n');
            }
            player.sendMessage(out.toString());
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/map/autoSave.kts (partial, slot view only)
        clientCommands.<Player>register("slots", "List WayZer-style autosave slots [100..105].", (args, player) -> {
            StringBuilder out = new StringBuilder();
            out.append("[green]=== [white]Autosave Slots [100..105] [green]===\n");

            int found = 0;
            for(int id = 100; id < 106; id++){
                Fi file = SaveIO.fileFor(id);
                if(!file.exists()) continue;
                found++;
                Date date = new Date(file.lastModified());
                out.append("[red]").append(id).append("[]: [yellow]Save on ").append(date).append('\n');
            }

            if(found == 0){
                out.append("[scarlet]No autosave slots in [100..105].\n");
            }
            out.append("[green]==============================");
            player.sendMessage(out.toString());
        });

        // Ported from WayZer ScriptAgent4MindustryExt:
        // scripts/wayzer/cmds/voteMap.kts (partial, rollback vote)
        clientCommands.<Player>register("voterollback", "<slot>", "Vote to rollback to a save slot.", (args, player) -> {
            String slot = args[0].trim();
            if(slot.isEmpty()){
                player.sendMessage("[scarlet]Slot cannot be empty.");
                return;
            }

            Fi file = saveDirectory.child(slot + "." + saveExtension);
            if(!SaveIO.isSaveValid(file)){
                player.sendMessage("[scarlet]Invalid or missing save slot: [orange]" + slot);
                return;
            }

            startGenericVote(
            player,
            "[yellow]Vote to rollback to save [accent]" + slot,
            p -> true,
            c -> votesRequired(),
            true,
            () -> {
                Call.sendMessage("[yellow]Loading save slot: [accent]" + slot);
                Core.app.post(() -> {
                    try{
                        SaveIO.load(file);
                        state.rules.sector = null;
                        state.set(State.playing);
                        Call.sendMessage("[green]Rollback successful: [accent]" + slot);
                    }catch(Throwable t){
                        Log.err("Failed to rollback save slot " + slot + ".", t);
                        Call.sendMessage("[scarlet]Rollback failed: [orange]" + slot);
                    }
                });
            });
        });

        clientCommands.<Player>register("t", "<message...>", "Send a message only to your teammates.", (args, player) -> {
            String message = admins.filterMessage(player, args[0]);
            if(message != null){
                String raw = "[#" + player.team().color.toString() + "]<T> " + chatFormatter.format(player, message);
                Groups.player.each(p -> p.team() == player.team(), o -> o.sendMessage(raw, player, message));
            }
        });

        clientCommands.<Player>register("handicap", "Open handicap selection menu (100%-50%).", (args, player) -> {
            if(isObserver(player)){
                player.sendMessage("[scarlet]Observers cannot set handicap.");
                return;
            }
            if(!matchPreviewActive){
                player.sendMessage("[scarlet]Handicap can only be set during map preview.");
                return;
            }
            if(player.con == null){
                player.sendMessage("[scarlet]No connection.");
                return;
            }

            int current = getTeamHandicapPercent(player.team());
            String[][] options = {
            {"100%", "90%", "80%"},
            {"70%", "60%", "50%"}
            };
            Call.menu(player.con, handicapMenuId, "Set Handicap",
            "Select opening HP for your team.\nCurrent: " + current + "%", options);
        });

        clientCommands.<Player>register("start", "Admin: start the current match with public countdown.", (args, player) -> {
            if(!player.admin){
                player.sendMessage("[scarlet]Admin required.");
                return;
            }
            if(!matchPreviewActive){
                player.sendMessage("[scarlet]/start is only available during map preview.");
                return;
            }

            int countdown = resolveStartCountdownSeconds();
            Call.sendMessage("[yellow]Admin [accent]" + player.name + "[yellow] started the match countdown ([accent]" + countdown + "[yellow]s).");
            startMatchFromPreview();
        });

        clientCommands.<Player>register("a", "<message...>", "Send a message only to admins.", (args, player) -> {
            if(!player.admin){
                player.sendMessage("[scarlet]You must be an admin to use this command.");
                return;
            }

            String raw = "[#" + Pal.adminChat.toString() + "]<A> " + chatFormatter.format(player, args[0]);
            Groups.player.each(Player::admin, a -> a.sendMessage(raw, player, args[0]));
        });

        //cooldowns per player
        ObjectMap<String, Timekeeper> cooldowns = new ObjectMap<>();

        clientCommands.<Player>register("votekick", "[player] [reason...]", "Vote to kick a player with a valid reason.", (args, player) -> {
            if(!Config.enableVotekick.bool()){
                player.sendMessage("[scarlet]Vote-kick is disabled on this server.");
                return;
            }

            if(Groups.player.size() < 3){
                player.sendMessage("[scarlet]At least 3 players are needed to start a votekick.");
                return;
            }

            if(player.isLocal()){
                player.sendMessage("[scarlet]Just kick them yourself if you're the host.");
                return;
            }

            if(currentlyKicking != null || currentlyVoting != null){
                player.sendMessage("[scarlet]A vote is already in progress.");
                return;
            }

            if(args.length == 0){
                StringBuilder builder = new StringBuilder();
                builder.append("[orange]Players to kick: \n");

                Groups.player.each(p -> !p.admin && p.con != null && p != player, p -> {
                    String uid = shortUidOf(p);
                    builder.append("[lightgray] ").append(p.name).append("[accent] (#").append(p.id()).append(", uid=").append(uid == null ? "---" : uid).append(")\n");
                });
                player.sendMessage(builder.toString());
            }else if(args.length == 1){
                player.sendMessage("[orange]You need a valid reason to kick the player. Add a reason after the player name.");
            }else{
                Player found;
                if(args[0].length() > 1 && args[0].startsWith("#") && Strings.canParseInt(args[0].substring(1))){
                    int id = Strings.parseInt(args[0].substring(1));
                    found = Groups.player.find(p -> p.id() == id);
                }else{
                    found = findPlayerByUid(args[0]);
                    if(found == null){
                        found = Groups.player.find(p -> matchesDisplayOrBaseName(p, args[0]));
                    }
                }

                if(found != null){
                    if(found == player){
                        player.sendMessage("[scarlet]You can't vote to kick yourself.");
                    }else if(found.admin){
                        player.sendMessage("[scarlet]Did you really expect to be able to kick an admin?");
                    }else if(found.isLocal()){
                        player.sendMessage("[scarlet]Local players cannot be kicked.");
                    }else if(found.team() != player.team()){
                        player.sendMessage("[scarlet]Only players on your team can be kicked.");
                    }else{
                        Timekeeper vtime = cooldowns.get(player.uuid(), () -> new Timekeeper(voteCooldown));

                        if(!vtime.get()){
                            player.sendMessage("[scarlet]You must wait " + voteCooldown/60 + " minutes between votekicks.");
                            return;
                        }

                        VoteSession session = new VoteSession(found);
                        session.vote(player, 1);
                        Call.sendMessage(Strings.format("[lightgray]Reason:[orange] @[lightgray].", args[1]));
                        vtime.reset();
                        currentlyKicking = session;
                    }
                }else{
                    player.sendMessage("[scarlet]No player [orange]'" + args[0] + "'[scarlet] found.");
                }
            }
        });

        clientCommands.<Player>register("vote", "<y/n/c/start>", "Vote on the current vote session. Use 'start' during preview to start match vote.", (arg, player) -> {
            if(arg[0].equalsIgnoreCase("start")){
                if(currentlyVoting != null || currentlyKicking != null){
                    player.sendMessage("[scarlet]A vote is already in progress.");
                    return;
                }
                if(!matchPreviewActive){
                    player.sendMessage("[scarlet]Start vote is only available during map preview.");
                    return;
                }
                if(isObserver(player)){
                    player.sendMessage("[scarlet]Observers cannot start a match vote.");
                    return;
                }

                startGenericVote(
                player,
                "[yellow]Start the match now?",
                p -> !isObserver(p),
                count -> Math.max(1, count),
                true,
                this::startMatchFromPreview
                );
                return;
            }

            if(currentlyVoting != null){
                if(player.admin && arg[0].equalsIgnoreCase("c")){
                    Call.sendMessage(Strings.format("[lightgray]Vote canceled by admin[orange] @[lightgray].", player.name));
                    currentlyVoting.task.cancel();
                    currentlyVoting = null;
                    return;
                }

                if(player.isLocal()){
                    player.sendMessage("[scarlet]Local players can't vote.");
                    return;
                }

                int sign = switch(arg[0].toLowerCase()){
                    case "y", "yes", "1" -> 1;
                    case "n", "no", "0" -> -1;
                    default -> 0;
                };

                if(sign == 0){
                    player.sendMessage("[scarlet]Vote either 'y' (yes) or 'n' (no).");
                    return;
                }

                currentlyVoting.vote(player, sign);
                return;
            }

            if(currentlyKicking == null){
                player.sendMessage("[scarlet]Nobody is being voted on.");
            }else{
                if(player.admin && arg[0].equalsIgnoreCase("c")){
                    Call.sendMessage(Strings.format("[lightgray]Vote canceled by admin[orange] @[lightgray].", player.name));
                    currentlyKicking.task.cancel();
                    currentlyKicking = null;
                    return;
                }

                if(player.isLocal()){
                    player.sendMessage("[scarlet]Local players can't vote. Kick the player yourself instead.");
                    return;
                }

                int sign = switch(arg[0].toLowerCase()){
                    case "y", "yes" -> 1;
                    case "n", "no" -> -1;
                    default -> 0;
                };

                //hosts can vote all they want
                if((currentlyKicking.voted.get(player.uuid(), 2) == sign || currentlyKicking.voted.get(admins.getInfo(player.uuid()).lastIP, 2) == sign)){
                    player.sendMessage(Strings.format("[scarlet]You've already voted @. Sit down.", arg[0].toLowerCase()));
                    return;
                }

                if(currentlyKicking.target == player){
                    player.sendMessage("[scarlet]You can't vote on your own trial.");
                    return;
                }

                if(currentlyKicking.target.team() != player.team()){
                    player.sendMessage("[scarlet]You can't vote for other teams.");
                    return;
                }

                if(sign == 0){
                    player.sendMessage("[scarlet]Vote either 'y' (yes) or 'n' (no).");
                    return;
                }

                currentlyKicking.vote(player, sign);
            }
        });

        clientCommands.<Player>register("sync", "Re-synchronize world state.", (args, player) -> {
            if(player.isLocal()){
                player.sendMessage("[scarlet]Re-synchronizing as the host is pointless.");
            }else{
                if(Time.timeSinceMillis(player.getInfo().lastSyncTime) < 1000 * 5){
                    player.sendMessage("[scarlet]You may only /sync every 5 seconds.");
                    return;
                }

                player.getInfo().lastSyncTime = Time.millis();
                Call.worldDataBegin(player.con);
                netServer.sendWorldData(player);
            }
        });
    }

    public int votesRequired(){
        return 2 + (Groups.player.size() > 4 ? 1 : 0);
    }

    public Team assignTeam(Player current){
        return assignTeam(current, Groups.player);
    }

    public Team assignTeam(Player current, Iterable<Player> players){
        Team preferred = assigner.assign(current, players);
        Team available = firstAvailableTeam(preferred, current, players);
        return available != null ? available : (preferred != null ? preferred : state.rules.defaultTeam);
    }

    private static @Nullable Team firstAvailableTeam(@Nullable Team preferred, @Nullable Player except, Iterable<Player> players){
        if(preferred != null && !isTeamOccupied(preferred, except, players)){
            return preferred;
        }

        for(Team team : Team.all){
            if(!isTeamOccupied(team, except, players)){
                return team;
            }
        }

        return null;
    }

    private static boolean isTeamOccupied(Team team, @Nullable Player except, Iterable<Player> players){
        for(Player other : players){
            if(other != except && other.team() == team){
                return true;
            }
        }
        return false;
    }

    public void sendWorldData(Player player){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DeflaterOutputStream def = new FastDeflaterOutputStream(stream);
        NetworkIO.writeWorld(player, def);
        WorldStream data = new WorldStream();
        data.stream = new ByteArrayInputStream(stream.toByteArray());
        player.con.sendStream(data);

        debug("Packed @ bytes of world data to @ (@ / @)", stream.size(), player.name, player.con.address, player.uuid());
    }

    public void addPacketHandler(String type, Cons2<Player, String> handler){
        customPacketHandlers.get(type, Seq::new).add(handler);
    }

    public Seq<Cons2<Player, String>> getPacketHandlers(String type){
        return customPacketHandlers.get(type, Seq::new);
    }

    public void addBinaryPacketHandler(String type, Cons2<Player, byte[]> handler){
        customBinaryPacketHandlers.get(type, Seq::new).add(handler);
    }

    public Seq<Cons2<Player, byte[]>> getBinaryPacketHandlers(String type){
        return customBinaryPacketHandlers.get(type, Seq::new);
    }

    public void addLogicDataHandler(String type, Cons2<Player, Object> handler){
        logicClientDataHandlers.get(type, Seq::new).add(handler);
    }

    public static void onDisconnect(Player player, String reason){
        //singleplayer multiplayer weirdness
        if(player.con == null){
            player.remove();
            return;
        }

        if(!player.con.hasDisconnected){
            if(player.con.hasConnected){
                Events.fire(new PlayerLeave(player));
                if(Config.showConnectMessages.bool()) Call.sendMessage("[accent]" + player.name + "[accent] has disconnected.");
                Call.playerDisconnect(player.id());
            }

            String message = Strings.format("&lb@&fi&lk has disconnected. [&lb@&fi&lk] (@)", player.plainName(), player.uuid(), reason);
            if(Config.showConnectMessages.bool()) info(message);
        }

        player.remove();
        player.con.hasDisconnected = true;
        netServer.playerNumbers.remove(player.uuid(), 0);
        netServer.identityChallenges.remove(player.uuid());
        netServer.identityChallengeTimes.remove(player.uuid());
    }

    //these functions are for debugging only, and will be removed!

    @Remote(targets = Loc.client, variants = Variant.one)
    public static void requestDebugStatus(Player player){
        int flags =
        (player.con.hasDisconnected ? 1 : 0) |
        (player.con.hasConnected ? 2 : 0) |
        (player.isAdded() ? 4 : 0) |
        (player.con.hasBegunConnecting ? 8 : 0);

        Call.debugStatusClient(player.con, flags, player.con.lastReceivedClientSnapshot, player.con.snapshotsSent);
        Call.debugStatusClientUnreliable(player.con, flags, player.con.lastReceivedClientSnapshot, player.con.snapshotsSent);
    }

    @Remote(variants = Variant.both, priority = PacketPriority.high)
    public static void debugStatusClient(int value, int lastClientSnapshot, int snapshotsSent){
        logClientStatus(true, value, lastClientSnapshot, snapshotsSent);
    }

    @Remote(variants = Variant.both, priority = PacketPriority.high, unreliable = true)
    public static void debugStatusClientUnreliable(int value, int lastClientSnapshot, int snapshotsSent){
        logClientStatus(false, value, lastClientSnapshot, snapshotsSent);
    }

    static void logClientStatus(boolean reliable, int value, int lastClientSnapshot, int snapshotsSent){
        Log.info("@ Debug status received. disconnected = @, connected = @, added = @, begunConnecting = @ lastClientSnapshot = @, snapshotsSent = @",
        reliable ? "[RELIABLE]" : "[UNRELIABLE]",
        (value & 1) != 0, (value & 2) != 0, (value & 4) != 0, (value & 8) != 0,
        lastClientSnapshot, snapshotsSent
        );
    }

    @Remote(targets = Loc.client)
    public static void serverPacketReliable(Player player, String type, String contents){
        if(netServer.customPacketHandlers.containsKey(type)){
            for(Cons2<Player, String> c : netServer.customPacketHandlers.get(type)){
                c.get(player, contents);
            }
        }
    }

    @Remote(targets = Loc.client, unreliable = true)
    public static void serverPacketUnreliable(Player player, String type, String contents){
        serverPacketReliable(player, type, contents);
    }

    @Remote(targets = Loc.client)
    public static void serverBinaryPacketReliable(Player player, String type, byte[] contents){
        if(netServer.customBinaryPacketHandlers.containsKey(type)){
            for(var c : netServer.customBinaryPacketHandlers.get(type)){
                c.get(player, contents);
            }
        }
    }

    @Remote(targets = Loc.client, unreliable = true)
    public static void serverBinaryPacketUnreliable(Player player, String type, byte[] contents){
        serverBinaryPacketReliable(player, type, contents);
    }

    @Remote(targets = Loc.client)
    public static void clientLogicDataReliable(Player player, String channel, Object value){
        Seq<Cons2<Player, Object>> handlers = netServer.logicClientDataHandlers.get(channel);
        if(handlers != null){
            for(Cons2<Player, Object> handler : handlers){
                handler.get(player, value);
            }
        }
    }

    @Remote(targets = Loc.client, unreliable = true)
    public static void clientLogicDataUnreliable(Player player, String channel, Object value){
        clientLogicDataReliable(player, channel, value);
    }

    private void handleIdentityPong(Player player, String contents){
        if(player == null || player.isLocal()) return;

        String id = player.uuid();
        String expected = identityChallenges.get(id);
        if(expected == null) return;

        int split = contents == null ? -1 : contents.indexOf('|');
        if(split <= 0){
            integrityBan(player, "identity-response-malformed");
            return;
        }

        String nonce = contents.substring(0, split);
        String reportedHash = contents.substring(split + 1).trim();
        if(!expected.equals(nonce)){
            integrityBan(player, "identity-response-nonce-mismatch");
            return;
        }

        identityChallenges.remove(id);
        identityChallengeTimes.remove(id);

        if(!id.equals(reportedHash)){
            integrityBan(player, "identity-hash-changed");
        }
    }

    private void sendIdentityPing(Player player){
        if(player == null || player.con == null || player.isLocal() || !player.con.hasConnected) return;

        String id = player.uuid();
        String nonce = Long.toHexString(identityRand.nextLong()) + Long.toHexString(Time.millis());
        identityChallenges.put(id, nonce);
        identityChallengeTimes.put(id, Time.millis());
        Call.clientPacketReliable(player.con, "identity-ping", nonce);
    }

    private void checkIdentityPingTimeouts(){
        long now = Time.millis();
        Groups.player.each(player -> {
            if(player == null || player.isLocal()) return;
            Long sentAt = identityChallengeTimes.get(player.uuid());
            if(sentAt != null && now - sentAt > identityTimeoutMs){
                integrityBan(player, "identity-response-timeout");
            }
        });
    }

    private void integrityBan(Player player, String reason){
        if(player == null || player.con == null) return;

        String id = player.uuid();
        if(integrityBans.add(id)){
            saveBanList();
        }

        admins.banPlayerID(id);
        player.con.kick(KickReason.banned);
    }

    private void loadBanList(){
        if(!banListFile.exists()) return;

        for(String line : banListFile.readString().split("\\r?\\n")){
            String text = line.trim();
            if(text.isEmpty()) continue;
            int split = text.indexOf('|');
            String id = split < 0 ? text : text.substring(0, split);
            if(!id.isEmpty()){
                integrityBans.add(id);
            }
        }
    }

    private void saveBanList(){
        StringBuilder out = new StringBuilder();
        for(String id : integrityBans){
            out.append(id).append('\n');
        }
        banListFile.writeString(out.toString(), false);
    }

    public boolean removeIntegrityBan(String id){
        if(id == null || !integrityBans.remove(id)) return false;
        saveBanList();
        return true;
    }

    public boolean isIntegrityBanned(String id){
        return id != null && integrityBans.contains(id);
    }

    private String formatWelcomeTemplate(String template, Player player){
        String value = template == null ? "" : template;
        value = value.replace("{player.name}", player.name);
        value = value.replace("{name}", player.name);
        return value;
    }

    private String formatUptime(){
        long elapsed = Math.max(0L, Time.timeSinceMillis(serverStartTimeMs));
        long totalSeconds = elapsed / 1000L;
        long days = totalSeconds / 86400L;
        long hours = (totalSeconds % 86400L) / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        if(days > 0){
            return days + "d " + hours + "h " + minutes + "m";
        }else if(hours > 0){
            return hours + "h " + minutes + "m " + seconds + "s";
        }else{
            return minutes + "m " + seconds + "s";
        }
    }

    private static String wrapMapInfoText(String text, int limit){
        if(text.isEmpty() || limit < 4) return text;

        StringBuilder out = new StringBuilder();
        int lineLen = 0;
        for(String token : text.split("\\s+")){
            if(token.isEmpty()) continue;

            int nextLen = lineLen == 0 ? token.length() : lineLen + 1 + token.length();
            if(nextLen > limit){
                if(lineLen > 0){
                    out.append('\n');
                }
                out.append(token);
                lineLen = token.length();
            }else{
                if(lineLen > 0){
                    out.append(' ');
                    lineLen++;
                }
                out.append(token);
                lineLen += token.length();
            }
        }
        return out.toString();
    }

    private void loadAlertConfig(){
        alertMessages.clear();
        String raw = Core.settings.getString("wayzer-alert-list", "");
        if(raw == null || raw.trim().isEmpty()) return;

        for(String line : raw.split("\\r?\\n")){
            String msg = line.trim();
            if(!msg.isEmpty()){
                alertMessages.add(msg);
            }
        }

        if(alertIndex >= alertMessages.size){
            alertIndex = 0;
        }
    }

    private void saveAlertConfig(){
        Core.settings.put("wayzer-alert-enabled", alertEnabled);
        Core.settings.put("wayzer-alert-interval-seconds", alertIntervalSeconds);

        StringBuilder out = new StringBuilder();
        for(int i = 0; i < alertMessages.size; i++){
            if(i > 0) out.append('\n');
            out.append(alertMessages.get(i).replace("\n", " "));
        }
        Core.settings.put("wayzer-alert-list", out.toString());
        Core.settings.forceSave();
    }

    private void sendNextAlert(){
        if(alertMessages.isEmpty()) return;

        if(alertIndex >= alertMessages.size){
            alertIndex = 0;
        }

        String msg = alertMessages.get(alertIndex);
        alertIndex = (alertIndex + 1) % alertMessages.size;
        Call.sendMessage(msg);
    }

    private void loadTransferServers(){
        transferServers.clear();
        String raw = Core.settings.getString("wayzer-go-servers", "");
        if(raw == null || raw.trim().isEmpty()) return;

        for(String line : raw.split("\\r?\\n")){
            String text = line.trim();
            if(text.isEmpty()) continue;

            int split = text.indexOf('=');
            if(split <= 0 || split >= text.length() - 1) continue;

            String name = text.substring(0, split).trim();
            String value = text.substring(split + 1).trim();
            TransferServerInfo info = parseTransferServer(name, value);
            if(info != null){
                transferServers.put(info.name, info);
            }
        }
    }

    private void saveTransferServers(){
        StringBuilder out = new StringBuilder();
        int i = 0;
        for(var entry : transferServers){
            if(i++ > 0) out.append('\n');
            TransferServerInfo info = entry.value;
            out.append(info.name).append('=').append(info.desc).append(';').append(info.address).append(':').append(info.port);
        }
        Core.settings.put("wayzer-go-servers", out.toString());
        Core.settings.forceSave();
    }

    private @Nullable TransferServerInfo parseTransferServer(String name, String value){
        if(name == null || value == null) return null;
        String fixedName = name.trim();
        if(fixedName.isEmpty()) return null;

        String[] sp1 = value.split(";", 2);
        if(sp1.length != 2) return null;

        String desc = sp1[0].trim();
        String addressPort = sp1[1].trim();
        if(addressPort.isEmpty()) return null;

        String address = addressPort;
        int serverPort = Config.port.num();
        int idx = addressPort.lastIndexOf(':');
        if(idx > 0 && idx < addressPort.length() - 1){
            String maybePort = addressPort.substring(idx + 1).trim();
            if(Strings.canParseInt(maybePort)){
                serverPort = Strings.parseInt(maybePort);
                address = addressPort.substring(0, idx).trim();
            }
        }

        if(address.isEmpty()) return null;
        serverPort = Mathf.clamp(serverPort, 1, 65535);
        return new TransferServerInfo(fixedName, desc, address, serverPort);
    }

    private @Nullable String findTransferServerKey(String name){
        if(name == null) return null;
        for(var entry : transferServers){
            if(entry.key.equalsIgnoreCase(name.trim())){
                return entry.key;
            }
        }
        return null;
    }

    private @Nullable TransferServerInfo findTransferServer(String name){
        String key = findTransferServerKey(name);
        return key == null ? null : transferServers.get(key);
    }

    private void initTpsLimit(){
        try{
            baseDeltaProvider = Reflect.get(Time.class, "deltaimpl");
        }catch(Throwable t){
            baseDeltaProvider = null;
            Log.warn("Failed to init TPS limiter provider: @", t.toString());
        }
        applyTpsLimit();
    }

    private void applyTpsLimit(){
        if(baseDeltaProvider == null) return;

        if(tpsLimitEnabled){
            Time.setDeltaProvider(() -> Math.min(baseDeltaProvider.get(), tpsLimitMaxDelta));
        }else{
            Time.setDeltaProvider(baseDeltaProvider);
        }
    }

    private void saveTpsLimitConfig(){
        Core.settings.put("wayzer-tpslimit-enabled", tpsLimitEnabled);
        Core.settings.put("wayzer-tpslimit-maxdelta", tpsLimitMaxDelta);
        Core.settings.forceSave();
    }

    private boolean isResourceMapIdToken(String token){
        if(token == null || !Strings.canParseInt(token)) return false;
        int id = Strings.parseInt(token);
        return id >= 10000 && id <= 99999;
    }

    private void fetchResourceSiteMaps(String search, Cons<Seq<ResourceMapInfo>> onSuccess, Cons<String> onFailure){
        String keyword = search == null ? "" : search.trim();
        String mapped = switch(keyword.toLowerCase()){
            case "", "all", "display", "site" -> "";
            case "pvp", "attack", "survive" -> "@mode:" + Strings.capitalize(keyword.toLowerCase());
            default -> keyword;
        };

        String encoded;
        try{
            encoded = URLEncoder.encode(mapped, "UTF-8");
        }catch(Exception e){
            onFailure.get("Failed to encode search keyword.");
            return;
        }

        String root = resourceSiteApiRoot == null || resourceSiteApiRoot.trim().isEmpty() ? "https://api.mindustry.top" : resourceSiteApiRoot.trim();
        String url = root + "/maps/list?prePage=100&search=" + encoded;
        Http.get(url, res -> {
            try{
                Jval data = Jval.read(res.getResultAsString());
                if(!data.isArray()){
                    onFailure.get("Unexpected API response.");
                    return;
                }

                Seq<ResourceMapInfo> infos = new Seq<>();
                data.asArray().each(item -> {
                    int id = item.getInt("id", -1);
                    if(id < 0) return;
                    String name = item.getString("name", "unknown");
                    String author = item.getString("author", "unknown");
                    String mode = item.getString("mode", "unknown");
                    String description = item.getString("desc", "");
                    infos.add(new ResourceMapInfo(id, name, author, mode, description));
                });
                onSuccess.get(infos);
            }catch(Throwable t){
                Log.err("Failed to parse resource-site map list response.", t);
                onFailure.get("Failed to parse API response.");
            }
        }, err -> onFailure.get(err.getMessage() == null ? err.toString() : err.getMessage()));
    }

    private void fetchResourceSiteMapById(int id, Cons<Map> onSuccess, Cons<String> onFailure){
        String root = resourceSiteApiRoot == null || resourceSiteApiRoot.trim().isEmpty() ? "https://api.mindustry.top" : resourceSiteApiRoot.trim();
        String url = root + "/maps/" + id + ".msav";
        Http.get(url, res -> {
            byte[] bytes = res.getResult();
            if(bytes == null || bytes.length == 0){
                onFailure.get("Empty response body.");
                return;
            }

            try{
                final byte[] mapBytes = bytes;
                Fi data = new Fi("resource-" + id + ".msav"){
                    @Override
                    public InputStream read(){
                        return new ByteArrayInputStream(mapBytes);
                    }
                };

                Map map = MapIO.createMap(data, true);
                onSuccess.get(map);
            }catch(Throwable t){
                Log.err("Failed to decode resource-site map " + id + ".", t);
                onFailure.get("Map file decode failed.");
            }
        }, err -> onFailure.get(err.getMessage() == null ? err.toString() : err.getMessage()));
    }

    private @Nullable Map resolveVoteMap(String token){
        if(token == null) return null;

        String clean = Strings.stripColors(token).trim();
        if(clean.isEmpty()) return null;

        Seq<Map> allMaps = maps.all();
        if(allMaps.isEmpty()) return null;

        if(Strings.canParseInt(clean)){
            int index = Strings.parseInt(clean) - 1;
            if(index >= 0 && index < allMaps.size){
                return allMaps.get(index);
            }
        }

        String normalized = clean.replace('_', ' ').toLowerCase();
        Map exact = allMaps.find(map -> map.plainName().replace('_', ' ').equalsIgnoreCase(normalized));
        if(exact != null) return exact;

        Seq<Map> contains = allMaps.select(map -> map.plainName().replace('_', ' ').toLowerCase().contains(normalized));
        if(contains.size == 1) return contains.first();

        return null;
    }

    private static class ResourceMapInfo{
        final int id;
        final String name, author, mode, description;

        ResourceMapInfo(int id, String name, String author, String mode, String description){
            this.id = id;
            this.name = name;
            this.author = author;
            this.mode = mode;
            this.description = description;
        }
    }

    private void startGenericVote(Player starter, String description, Boolf<Player> canVote, Intf<Integer> requiredVotes, boolean supportSingle, Runnable onPass){
        if(currentlyKicking != null || currentlyVoting != null){
            starter.sendMessage("[scarlet]A vote is already in progress.");
            return;
        }

        int eligible = Groups.player.count(p -> !p.isLocal() && canVote.get(p));
        if(eligible <= 0){
            starter.sendMessage("[scarlet]No eligible voters for this vote.");
            return;
        }

        int required = Math.max(1, requiredVotes.get(eligible));

        if(supportSingle && eligible <= 1){
            Call.sendMessage("[lightgray]Single-player vote auto-passed: " + description);
            try{
                onPass.run();
            }catch(Throwable t){
                Log.err("Generic vote action failed.", t);
            }
            return;
        }

        GenericVoteSession session = new GenericVoteSession(description, canVote, requiredVotes, onPass);
        currentlyVoting = session;
        Call.sendMessage("[lightgray]Vote started: " + description + "\n[lightgray]Type[orange] /vote <y/n>[] to vote.");
        session.vote(starter, 1);
    }

    private static class TransferServerInfo{
        final String name, desc, address;
        final int port;

        TransferServerInfo(String name, String desc, String address, int port){
            this.name = name;
            this.desc = desc;
            this.address = address;
            this.port = port;
        }
    }

    private static boolean invalid(float f){
        return Float.isInfinite(f) || Float.isNaN(f);
    }

    private void loadWayzerEffects(){
        for(Field field : Fx.class.getFields()){
            if(field.getType() != Effect.class) continue;
            try{
                Effect effect = (Effect)field.get(null);
                if(effect != null){
                    wayzerEffects.put(field.getName().toLowerCase(), effect);
                }
            }catch(Throwable ignored){
                // ignored on reflection failure
            }
        }
    }

    private boolean canGatherTeleport(Unit unit, Tile tile){
        if(unit.type.flying) return true;
        if(!unit.canPass(tile.x, tile.y)) return false;
        return Units.count(tile.worldx(), tile.worldy(), unit.physicSize(), other -> other.isGrounded() && other.hitSize > 14f) <= 1;
    }

    private void scheduleRestart(String message, boolean now){
        scheduledRestartMessage = message == null ? "" : message.trim();
        String ext = scheduledRestartMessage.isEmpty() ? "" : "\n[lightgray]" + scheduledRestartMessage;
        Call.sendMessage("[yellow]Server will restart after this game." + ext);

        if(now){
            if(state.isGame()){
                Events.fire(new GameOverEvent(Team.derelict));
            }else{
                executeScheduledRestart();
            }
        }
    }

    private void executeScheduledRestart(){
        if(restarting) return;
        restarting = true;

        String ext = scheduledRestartMessage == null || scheduledRestartMessage.isEmpty() ? "" : "\n[lightgray]" + scheduledRestartMessage;
        Call.sendMessage("[yellow]Server restarting..." + ext);

        Time.runTask(60f, () -> {
            Groups.player.each(p -> p.con != null, p -> p.con.kick(KickReason.serverRestarting));
            Time.runTask(12f, () -> System.exit(2));
        });
    }

    @Remote(targets = Loc.client, unreliable = true, priority = PacketPriority.high)
    public static void clientSnapshot(
    Player player,
    int snapshotID,
    int unitID,
    boolean dead,
    float x, float y,
    float pointerX, float pointerY,
    float rotation, float baseRotation,
    float xVelocity, float yVelocity,
    Tile mining,
    boolean boosting, boolean shooting, boolean chatting, boolean building,
    @Nullable Queue<BuildPlan> plans,
    float viewX, float viewY, float viewWidth, float viewHeight
    ){
        NetConnection con = player.con;
        if(con == null || snapshotID < con.lastReceivedClientSnapshot) return;

        //validate coordinates just in case
        if(invalid(x)) x = 0f;
        if(invalid(y)) y = 0f;
        if(invalid(xVelocity)) xVelocity = 0f;
        if(invalid(yVelocity)) yVelocity = 0f;
        if(invalid(pointerX)) pointerX = 0f;
        if(invalid(pointerY)) pointerY = 0f;
        if(invalid(rotation)) rotation = 0f;
        if(invalid(baseRotation)) baseRotation = 0f;

        boolean verifyPosition = netServer.admins.isStrict() && headless;

        if(con.lastReceivedClientTime == 0) con.lastReceivedClientTime = Time.millis() - 16;

        con.viewX = viewX;
        con.viewY = viewY;
        con.viewWidth = viewWidth;
        con.viewHeight = viewHeight;

        //disable shooting when a mech flies
        if(!player.dead() && player.unit().isFlying() && player.unit() instanceof Mechc){
            shooting = false;
        }

        if(!player.dead() && (player.unit().type.flying || !player.unit().type.canBoost)){
            boosting = false;
        }

        player.mouseX = pointerX;
        player.mouseY = pointerY;
        player.typing = chatting;
        player.shooting = shooting;
        player.boosting = boosting;

        @Nullable var unit = player.unit();

        if(player.isBuilder()){
            unit.clearBuilding();
            unit.updateBuilding(building);

            if(plans != null){
                for(BuildPlan req : plans){
                    if(req == null) continue;
                    Tile tile = world.tile(req.x, req.y);
                    if(tile == null || (!req.breaking && req.block == null)) continue;
                    //auto-skip done requests
                    if(req.breaking && tile.block() == Blocks.air){
                        continue;
                    }else if(!req.breaking && tile.block() == req.block && tile.team() != Team.derelict && (!req.block.rotate || (tile.build != null && tile.build.rotation == req.rotation))){
                        continue;
                    }else if(con.rejectedRequests.contains(r -> r.breaking == req.breaking && r.x == req.x && r.y == req.y)){ //check if request was recently rejected, and skip it if so
                        continue;
                    }else if(!netServer.admins.allowAction(player, req.breaking ? ActionType.breakBlock : ActionType.placeBlock, tile, action -> { //make sure request is allowed by the server
                        action.block = req.block;
                        action.rotation = req.rotation;
                        action.config = req.config;
                    })){
                        //force the player to remove this request if that's not the case
                        Call.removeQueueBlock(player.con, req.x, req.y, req.breaking);
                        con.rejectedRequests.add(req);
                        continue;
                    }
                    player.unit().plans().addLast(req);
                }
            }
        }

        con.rejectedRequests.clear();

        if(!player.dead()){
            unit.controlWeapons(shooting, shooting);
            unit.aim(pointerX, pointerY);
            unit.mineTile = mining;

            long elapsed = Math.min(Time.timeSinceMillis(con.lastReceivedClientTime), 1500);
            float maxSpeed = unit.speed();

            float maxMove = elapsed / 1000f * 60f * maxSpeed * 1.1f;

            //ignore the position if the player thinks they're dead, or the unit is wrong
            boolean ignorePosition = dead || unit.id != unitID;
            float newx = unit.x, newy = unit.y;

            if(!ignorePosition){
                unit.vel.set(xVelocity, yVelocity).limit(maxSpeed);

                vector.set(x, y).sub(unit);
                vector.limit(maxMove);

                float prevx = unit.x, prevy = unit.y;
                if(!unit.isFlying()){
                    unit.move(vector.x, vector.y);
                }else{
                    unit.trns(vector.x, vector.y);
                }

                newx = unit.x;
                newy = unit.y;

                if(!verifyPosition){
                    unit.set(prevx, prevy);
                    newx = x;
                    newy = y;
                }else if(!Mathf.within(x, y, newx, newy, correctDist)){
                    Call.setPosition(player.con, newx, newy); //teleport and correct position when necessary
                }
            }

            //write sync data to the buffer
            fbuffer.limit(20);
            fbuffer.position(0);

            //now, put the new position, rotation and baserotation into the buffer so it can be read
            //TODO this is terrible
            if(unit instanceof Mechc) fbuffer.put(baseRotation); //base rotation is optional
            fbuffer.put(rotation); //rotation is always there
            fbuffer.put(newx);
            fbuffer.put(newy);
            fbuffer.flip();

            //read sync data so it can be used for interpolation for the server
            unit.readSyncManual(fbuffer);
        }else{
            player.x = x;
            player.y = y;
        }

        con.lastReceivedClientSnapshot = snapshotID;
        con.lastReceivedClientTime = Time.millis();
    }

    @Remote(targets = Loc.client, called = Loc.server)
    public static void adminRequest(Player player, Player other, AdminAction action, Object params){
        if(!player.admin && !player.isLocal()){
            warn("ACCESS DENIED: Player @ / @ attempted to perform admin action '@' on '@' without proper security access.",
            player.plainName(), player.con == null ? "null" : player.con.address, action.name(), other == null ? null : other.plainName());
            return;
        }

        if(other == null || ((other.admin && !player.isLocal()) && other != player)){
            warn("@ &fi&lk[&lb@&fi&lk]&fb attempted to perform admin action on nonexistant or admin player.", player.plainName(), player.uuid());
            return;
        }

        Events.fire(new EventType.AdminRequestEvent(player, other, action));

        switch(action){
            case wave -> {
                //no verification is done, so admins can hypothetically spam waves
                //not a real issue, because server owners may want to do just that
                logic.skipWave();
                info("&lc@ &fi&lk[&lb@&fi&lk]&fb has skipped the wave.", player.plainName(), player.uuid());
            }
            case ban -> {
                netServer.admins.banPlayerID(other.con.uuid);
                netServer.admins.banPlayerIP(other.con.address);
                other.kick(KickReason.banned);
                info("&lc@ &fi&lk[&lb@&fi&lk]&fb has banned @ &fi&lk[&lb@&fi&lk]&fb.", player.plainName(), player.uuid(), other.plainName(), other.uuid());
            }
            case kick -> {
                other.kick(KickReason.kick);
                info("&lc@ &fi&lk[&lb@&fi&lk]&fb has kicked @ &fi&lk[&lb@&fi&lk]&fb.", player.plainName(), player.uuid(), other.plainName(), other.uuid());
            }
            case trace -> {
                PlayerInfo stats = netServer.admins.getInfo(other.uuid());
                TraceInfo info = new TraceInfo(other.con.address, other.uuid(), other.locale, other.con.modclient, other.con.mobile, stats.timesJoined, stats.timesKicked, stats.ips.toArray(String.class), stats.names.toArray(String.class));
                if(player.con != null){
                    Call.traceInfo(player.con, other, info);
                }else{
                    NetClient.traceInfo(other, info);
                }
            }
            case switchTeam -> {
                if(params instanceof Team team){
                    if(netServer.isObserverExitLocked() && other.team() != team){
                        player.sendMessage("[scarlet]Team changes are disabled during an active match.");
                        return;
                    }
                    if(isTeamOccupied(team, other, Groups.player)){
                        player.sendMessage("[scarlet]That team already has a player.");
                        return;
                    }
                    other.team(team);
                }
            }
        }
    }

    @Remote(targets = Loc.client, priority = PacketPriority.high)
    public static void connectConfirm(Player player){
        if(player.con.kicked) return;

        player.add();

        Events.fire(new PlayerConnectionConfirmed(player));

        if(player.con == null || player.con.hasConnected) return;

        player.con.hasConnected = true;

        if(Config.showConnectMessages.bool()){
            Call.sendMessage("[accent]" + player.name + "[accent] has connected.");
            String message = Strings.format("&lb@&fi&lk has connected. &fi&lk[&lb@&fi&lk]", player.plainName(), player.uuid());
            info(message);
        }

        if(!Config.motd.string().equalsIgnoreCase("off")){
            player.sendMessage(Config.motd.string());
        }

        if(netServer.scheduledRestartMessage != null){
            player.sendMessage("[yellow]Server restart is scheduled after this game: [lightgray]" + netServer.scheduledRestartMessage);
        }

        Events.fire(new PlayerJoin(player));
    }

    public boolean isWaitingForPlayers(){
        if(state.rules.pvp && !state.gameOver){
            int used = 0;
            for(TeamData t : state.teams.getActive()){
                if(Groups.player.count(p -> p.team() == t.team) > 0){
                    used++;
                }
            }
            return used < 2;
        }
        return false;
    }

    @Override
    public void update(){
        if(net.server() && state.isMenu()){
            gatherTileX = Integer.MIN_VALUE;
            gatherTileY = Integer.MIN_VALUE;
        }

        if(net.server() && state.isMenu() && scheduledRestartMessage != null && !restarting){
            executeScheduledRestart();
        }

        if(!headless && !closing && net.server() && state.isMenu()){
            closing = true;
            ui.loadfrag.show("@server.closing");
            Time.runTask(5f, () -> {
                net.closeServer();
                ui.loadfrag.hide();
                closing = false;
            });
        }

        if(state.isGame() && net.server()){
            if(matchPreviewActive && !state.isPaused()){
                state.set(State.paused);
            }

            if(state.rules.pvp && state.rules.pvpAutoPause && !matchPreviewActive && !startCountdownActive){
                boolean waiting = isWaitingForPlayers(), paused = state.isPaused();
                if(waiting != paused){
                    if(waiting){
                        //is now waiting, enable pausing, flag it correctly
                        pvpAutoPaused = true;
                        state.set(State.paused);
                    }else if(pvpAutoPaused){
                        //no longer waiting, stop pausing
                        state.set(State.playing);
                        pvpAutoPaused = false;
                    }
                }
            }

            sync();

            if(timer.get(2, identityPingInterval)){
                Groups.player.each(p -> !p.isLocal(), this::sendIdentityPing);
            }

            checkIdentityPingTimeouts();

            if(alertEnabled && !alertMessages.isEmpty() && timer.get(3, alertIntervalSeconds * 60f)){
                sendNextAlert();
            }
        }
    }

    //TODO I don't like where this is, move somewhere else?
    /** Queues a building health update. This will be sent in a Call.buildHealthUpdate packet later. */
    public void buildHealthUpdate(Building build){
        buildHealthChanged.add(build.pos());
    }

    /** Should only be used on the headless backend. */
    public void openServer(){
        try{
            net.host(Config.port.num());
            info("Opened a server on port @.", Config.port.num());
        }catch(BindException e){
            err("Unable to host: Port " + Config.port.num() + " already in use! Make sure no other servers are running on the same port in your network.");
            state.set(State.menu);
        }catch(IOException e){
            err(e);
            state.set(State.menu);
        }
    }

    public void kickAll(KickReason reason){
        for(NetConnection con : net.getConnections()){
            con.kick(reason);
        }
    }

    /** Sends a block snapshot to all players. */
    public void writeBlockSnapshots() throws IOException{
        syncStream.reset();

        short sent = 0;
        for(var team : state.teams.present){
            for(var build : indexer.getFlagged(team.team, BlockFlag.synced)){
                sent++;

                dataStream.writeInt(build.pos());
                dataStream.writeShort(build.block.id);
                build.writeSync(dataStreamWrites);

                if(syncStream.size() > maxSnapshotSize){
                    dataStream.close();
                    Call.blockSnapshot(sent, syncStream.toByteArray());
                    sent = 0;
                    syncStream.reset();
                }
            }
        }

        if(sent > 0){
            dataStream.close();
            Call.blockSnapshot(sent, syncStream.toByteArray());
        }
    }

    public void writeEntitySnapshot(Player player) throws IOException{
        byte tps = (byte)Math.min(Core.graphics.getFramesPerSecond(), 255);
        syncStream.reset();
        int activeTeams = (byte)state.teams.present.count(t -> t.cores.size > 0);

        dataStream.writeByte(activeTeams);
        dataWrites.output = dataStream;

        //block data isn't important, just send the items for each team, they're synced across cores
        for(TeamData data : state.teams.present){
            if(data.cores.size > 0){
                dataStream.writeByte(data.team.id);
                data.cores.first().items.write(dataWrites);
            }
        }

        dataStream.close();

        //write basic state data.
        Call.stateSnapshot(player.con, state.wavetime, state.wave, state.enemies, state.isPaused(), state.gameOver,
        universe.seconds(), tps, GlobalVars.rand.seed0, GlobalVars.rand.seed1, syncStream.toByteArray());

        syncStream.reset();

        hiddenIds.clear();
        int sent = 0;
        boolean observer = isObserver(player);

        for(Syncc entity : Groups.sync){
            //TODO write to special list
            if(entity.isSyncHidden(player) || (observer && isOutsideObserverView(player, entity))){
                hiddenIds.add(entity.id());
                continue;
            }

            //write all entities now
            dataStream.writeInt(entity.id()); //write id
            dataStream.writeByte(entity.classId() & 0xFF); //write type ID
            entity.beforeWrite();
            entity.writeSync(dataStreamWrites); //write entity itself

            sent++;

            if(syncStream.size() > maxSnapshotSize){
                dataStream.close();
                Call.entitySnapshot(player.con, (short)sent, syncStream.toByteArray());
                sent = 0;
                syncStream.reset();
            }
        }

        if(sent > 0){
            dataStream.close();

            Call.entitySnapshot(player.con, (short)sent, syncStream.toByteArray());
        }

        if(hiddenIds.size > 0){
            Call.hiddenSnapshot(player.con, hiddenIds);
        }

        player.con.snapshotsSent++;
    }

    public String fixName(String name){
        name = name.trim().replace("\n", "").replace("\t", "");
        if(name.equals("[") || name.equals("]")){
            return "";
        }

        for(int i = 0; i < name.length(); i++){
            if(name.charAt(i) == '[' && i != name.length() - 1 && name.charAt(i + 1) != '[' && (i == 0 || name.charAt(i - 1) != '[')){
                String prev = name.substring(0, i);
                String next = name.substring(i);
                String result = checkColor(next);

                name = prev + result;
            }
        }

        StringBuilder result = new StringBuilder();
        int curChar = 0;
        while(curChar < name.length() && result.toString().getBytes(Strings.utf8).length < maxNameLength){
            result.append(name.charAt(curChar++));
        }
        return result.toString();
    }

    private Player findPlayerByUid(String token){
        String uid = token;
        if(uid.startsWith("|")){
            uid = uid.substring(1);
        }

        if(uid.length() != 3) return null;

        String targetUid = uid;
        return Groups.player.find(p -> {
            String shortUid = shortUidOf(p);
            return shortUid != null && shortUid.equalsIgnoreCase(targetUid);
        });
    }

    private String shortUidOf(Player player){
        var info = admins.getInfoOptional(player.uuid());
        return info == null ? null : info.shortUid;
    }

    private boolean matchesDisplayOrBaseName(Player player, String token){
        String plainDisplay = Strings.stripColors(player.name);
        String plainToken = Strings.stripColors(token);
        if(plainDisplay.equalsIgnoreCase(plainToken)) return true;

        String uid = shortUidOf(player);
        if(uid == null) return false;

        String pipeSuffix = "|" + uid;
        if(plainDisplay.endsWith(pipeSuffix)){
            String base = plainDisplay.substring(0, plainDisplay.length() - pipeSuffix.length()).trim();
            if(base.equalsIgnoreCase(plainToken)) return true;
        }

        String wzSuffix = " " + uid;
        if(plainDisplay.endsWith(wzSuffix)){
            String base = plainDisplay.substring(0, plainDisplay.length() - wzSuffix.length()).trim();
            if(base.equalsIgnoreCase(plainToken)) return true;
        }

        return false;
    }

    private String withUidSuffix(String name, String uid){
        String base = name;
        // WayZer style suffix: a space + gray UID.
        String suffix = " [gray]" + uid + "[]";
        int maxBaseBytes = Math.max(1, maxNameLength - suffix.getBytes(Strings.utf8).length);

        while(base.getBytes(Strings.utf8).length > maxBaseBytes && base.length() > 1){
            base = base.substring(0, base.length() - 1);
        }

        return base + suffix;
    }

    private void enterMapPreview(){
        cancelStartCountdown();
        matchPreviewActive = true;
        if(!state.isPaused()){
            state.set(State.paused);
        }

        Call.sendMessage("[yellow]Map preview started. Damage is disabled and buildings are paused.");
        Call.sendMessage("[lightgray]Use [accent]/handicap[] to set team HP (100%-50%), then [accent]/vote start[] (or admin [accent]/start[]) to begin.");
    }

    private void resetTeamHandicaps(){
        teamHandicapPercent.clear();
        for(Team team : Team.all){
            teamHandicapPercent.put(team.id, 100);
        }
    }

    private int getTeamHandicapPercent(@Nullable Team team){
        if(team == null) return 100;
        return teamHandicapPercent.get(team.id, 100);
    }

    private void handleHandicapMenu(Player player, int option){
        if(player == null || option < 0 || option >= handicapOptions.length) return;

        if(isObserver(player)){
            player.sendMessage("[scarlet]Observers cannot set handicap.");
            return;
        }
        if(!matchPreviewActive){
            player.sendMessage("[scarlet]Handicap can only be set during map preview.");
            return;
        }

        int percent = handicapOptions[option];
        teamHandicapPercent.put(player.team().id, percent);
        Call.sendMessage("[accent]" + player.name + "[] set [yellow]" + player.team().name + "[] handicap to [orange]" + percent + "%[]");
    }

    private IntIntMap copyTeamHandicap(){
        IntIntMap out = new IntIntMap();
        for(IntIntMap.Entry entry : teamHandicapPercent){
            out.put(entry.key, entry.value);
        }
        return out;
    }

    private void startMatchFromPreview(){
        if(state.map == null){
            Call.sendMessage("[scarlet]Cannot start match: no map is loaded.");
            return;
        }

        IntIntMap savedHandicap = copyTeamHandicap();
        skipNextPreview = true;
        matchPreviewActive = false;
        handicappedUnits.clear();

        Core.app.post(() -> {
            try{
                Map map = state.map;
                world.loadMap(map, map.applyRules(state.rules.mode()));
                logic.play();

                teamHandicapPercent.clear();
                for(IntIntMap.Entry entry : savedHandicap){
                    teamHandicapPercent.put(entry.key, entry.value);
                }

                applyAllTeamHandicaps();
                startMatchCountdown(resolveStartCountdownSeconds());
            }catch(Throwable t){
                Log.err("Failed to start match from preview.", t);
                enterMapPreview();
            }
        });
    }

    private void startMatchCountdown(int seconds){
        int countdown = Mathf.clamp(seconds, 1, maxStartCountdownSeconds);
        int token = ++startCountdownToken;
        startCountdownActive = true;
        state.set(State.paused);
        Call.sendMessage("[accent]Match starts in [yellow]" + countdown + "[accent] seconds...");

        for(int i = countdown; i >= 1; i--){
            int value = i;
            Time.runTask((countdown - i) * 60f, () -> {
                if(token != startCountdownToken || !state.isGame() || matchPreviewActive) return;
                Call.announce("[accent]" + value);
            });
        }

        Time.runTask(countdown * 60f, () -> {
            if(token != startCountdownToken || !state.isGame() || matchPreviewActive) return;
            startCountdownActive = false;
            state.set(State.playing);
            pvpAutoPaused = false;
            Call.announce("[green]Start!");
        });
    }

    private void cancelStartCountdown(){
        startCountdownActive = false;
        startCountdownToken++;
    }

    private int resolveStartCountdownSeconds(){
        if(state.map == null) return defaultStartCountdownSeconds;

        String description = state.map.tags.get("description", "");
        if(description == null || description.isEmpty()) return defaultStartCountdownSeconds;

        Matcher matcher = startTimePattern.matcher(description);
        if(!matcher.find()) return defaultStartCountdownSeconds;

        try{
            return Mathf.clamp(Integer.parseInt(matcher.group(1)), 1, maxStartCountdownSeconds);
        }catch(Throwable ignored){
            return defaultStartCountdownSeconds;
        }
    }

    private void applyAllTeamHandicaps(){
        for(Unit unit : Groups.unit){
            applyTeamHandicap(unit);
        }
    }

    private void applyTeamHandicap(@Nullable Unit unit){
        if(unit == null || !unit.isValid()) return;

        int percent = getTeamHandicapPercent(unit.team);
        if(percent >= 100){
            handicappedUnits.remove(unit.id);
            return;
        }

        if(handicappedUnits.contains(unit.id)) return;

        float baseMax = unit.maxHealth();
        float targetMax = Math.max(1f, Mathf.floor(baseMax * percent / 100f));
        float baseHealth = Math.min(unit.health(), baseMax);
        float targetHealth = Math.max(1f, Mathf.floor(baseHealth * percent / 100f));

        unit.maxHealth(targetMax);
        unit.health(Math.min(targetMax, targetHealth));
        handicappedUnits.add(unit.id);
    }

    public String checkColor(String str){
        for(int i = 1; i < str.length(); i++){
            if(str.charAt(i) == ']'){
                String color = str.substring(1, i);

                if(Colors.get(color.toUpperCase()) != null || Colors.get(color.toLowerCase()) != null){
                    Color result = (Colors.get(color.toLowerCase()) == null ? Colors.get(color.toUpperCase()) : Colors.get(color.toLowerCase()));
                    if(result.a < 1f){
                        return str.substring(i + 1);
                    }
                }else{
                    try{
                        Color result = Color.valueOf(color);
                        if(result.a < 1f){
                            return str.substring(i + 1);
                        }
                    }catch(Exception e){
                        return str;
                    }
                }
            }
        }
        return str;
    }

    private int snapshotIntervalFor(Player player){
        if(isObserver(player)){
            int observerInterval = Config.spectatorSnapshotInterval.num();
            if(observerInterval > 0){
                return Math.max(16, observerInterval);
            }
        }

        int baseInterval = Math.max(16, Config.snapshotInterval.num());
        int activeInterval = Math.max(16, Config.snapshotIntervalActive.num());
        if(activeInterval > baseInterval){
            activeInterval = baseInterval;
        }
        return isSnapshotActive(player) ? activeInterval : baseInterval;
    }

    private boolean isObserver(Player player){
        if(player == null) return false;
        Team team = player.team();
        return team == Team.derelict || (team != null && !team.data().isAlive());
    }

    private boolean isObserverExitLocked(){
        return state.isGame() && !state.gameOver && !matchPreviewActive;
    }

    private boolean isOutsideObserverView(Player player, Syncc entity){
        if(player == null || player.con == null || !Config.spectatorCull.bool()) return false;
        if(!(entity instanceof Posc pos)) return false;

        float viewWidth = player.con.viewWidth, viewHeight = player.con.viewHeight;
        if(viewWidth <= 1f || viewHeight <= 1f) return false;

        float margin = Math.max(0f, Config.spectatorCullMargin.num());
        float halfWidth = viewWidth / 2f + margin;
        float halfHeight = viewHeight / 2f + margin;
        float radius = entity instanceof Hitboxc hit ? hit.hitSize() / 2f : 0f;

        return Math.abs(pos.x() - player.con.viewX) > halfWidth + radius ||
            Math.abs(pos.y() - player.con.viewY) > halfHeight + radius;
    }

    private boolean isSnapshotActive(Player player){
        if(player == null || player.dead()) return false;
        Unit unit = player.unit();
        if(unit == null) return false;
        if(player.shooting || player.boosting) return true;
        if(unit.activelyBuilding() || unit.isBuilding() || unit.mining()) return true;
        return unit.vel.len2() > 0.01f;
    }

    void sync(){
        try{
            Groups.player.each(p -> !p.isLocal(), player -> {
                if(player.con == null || !player.con.isConnected()){
                    onDisconnect(player, "disappeared");
                    return;
                }

                var connection = player.con;
                int interval = snapshotIntervalFor(player);

                if(Time.timeSinceMillis(connection.syncTime) < interval || !connection.hasConnected) return;

                connection.syncTime = Time.millis();

                try{
                    writeEntitySnapshot(player);
                }catch(IOException e){
                    Log.err(e);
                }
            });

            if(Groups.player.size() > 0 && Core.settings.getBool("blocksync") && timer.get(timerBlockSync, blockSyncTime)){
                writeBlockSnapshots();
            }

            if(Groups.player.size() > 0 && buildHealthChanged.size > 0 && timer.get(timerHealthSync, healthSyncTime)){
                healthSeq.clear();

                var iter = buildHealthChanged.iterator();
                while(iter.hasNext){
                    int next = iter.next();
                    var build = world.build(next);

                    //pack pos + health into update list
                    if(build != null){
                        healthSeq.add(next, Float.floatToRawIntBits(build.health));
                    }

                    //if size exceeds snapshot limit, send it out and begin building it up again
                    if(healthSeq.size * 4 >= maxSnapshotSize){
                        Call.buildHealthUpdate(healthSeq);
                        healthSeq.clear();
                    }
                }

                //send any residual health updates
                if(healthSeq.size > 0){
                    Call.buildHealthUpdate(healthSeq);
                }

                buildHealthChanged.clear();
            }
        }catch(IOException e){
            Log.err(e);
        }
    }

    public class VoteSession{
        Player target;
        ObjectIntMap<String> voted = new ObjectIntMap<>();
        Timer.Task task;
        int votes;

        public VoteSession(Player target){
            this.target = target;
            this.task = Timer.schedule(() -> {
                if(!checkPass()){
                    Call.sendMessage(Strings.format("[lightgray]Vote failed. Not enough votes to kick[orange] @[lightgray].", target.name));
                    currentlyKicking = null;
                    task.cancel();
                }
            }, voteDuration);
        }

        void vote(Player player, int d){
            int lastVote = voted.get(player.uuid(), 0) | voted.get(admins.getInfo(player.uuid()).lastIP, 0);
            votes -= lastVote;

            votes += d;
            voted.put(player.uuid(), d);
            voted.put(admins.getInfo(player.uuid()).lastIP, d);

            Call.sendMessage(Strings.format("[lightgray]@[lightgray] has voted on kicking[orange] @[lightgray].[accent] (@/@)\n[lightgray]Type[orange] /vote <y/n>[] to agree.",
            player.name, target.name, votes, votesRequired()));

            checkPass();
        }

        boolean checkPass(){
            if(votes >= votesRequired()){
                Call.sendMessage(Strings.format("[orange]Vote passed.[scarlet] @[orange] will be banned from the server for @ minutes.", target.name, (kickDuration / 60)));
                Groups.player.each(p -> p.uuid().equals(target.uuid()), p -> p.kick(KickReason.vote, kickDuration * 1000));
                currentlyKicking = null;
                task.cancel();
                return true;
            }
            return false;
        }
    }

    public class GenericVoteSession{
        String description;
        Boolf<Player> canVote;
        Intf<Integer> requiredVotes;
        Runnable onPass;
        ObjectIntMap<String> voted = new ObjectIntMap<>();
        Timer.Task task;
        int votes;

        public GenericVoteSession(String description, Boolf<Player> canVote, Intf<Integer> requiredVotes, Runnable onPass){
            this.description = description;
            this.canVote = canVote;
            this.requiredVotes = requiredVotes;
            this.onPass = onPass;

            this.task = Timer.schedule(() -> {
                if(!checkPass()){
                    Call.sendMessage("[lightgray]Vote failed:[orange] " + description + "[lightgray] (" + votes + "/" + required() + ").");
                    currentlyVoting = null;
                    task.cancel();
                }
            }, voteDuration);
        }

        int eligible(){
            return Math.max(1, Groups.player.count(p -> !p.isLocal() && canVote.get(p)));
        }

        int required(){
            return Math.max(1, requiredVotes.get(eligible()));
        }

        void vote(Player player, int d){
            if(!canVote.get(player)){
                player.sendMessage("[scarlet]You are not eligible for this vote.");
                return;
            }

            int lastVote = voted.get(player.uuid(), 0) | voted.get(admins.getInfo(player.uuid()).lastIP, 0);
            if(lastVote == d){
                player.sendMessage("[scarlet]You've already voted " + (d > 0 ? "yes" : "no") + ".");
                return;
            }
            votes -= lastVote;
            votes += d;
            voted.put(player.uuid(), d);
            voted.put(admins.getInfo(player.uuid()).lastIP, d);

            Call.sendMessage("[lightgray]" + player.name + "[lightgray] voted on [orange]" + description + "[lightgray]. [accent](" + votes + "/" + required() + ")");
            checkPass();
        }

        boolean checkPass(){
            if(votes >= required()){
                Call.sendMessage("[green]Vote passed:[accent] " + description);
                try{
                    onPass.run();
                }catch(Throwable t){
                    Log.err("Generic vote action failed.", t);
                }
                currentlyVoting = null;
                task.cancel();
                return true;
            }
            return false;
        }
    }

    public interface TeamAssigner{
        Team assign(Player player, Iterable<Player> players);
    }

    public interface ChatFormatter{
        /** @return text to be placed before player name */
        String format(@Nullable Player player, String message);
    }

    public interface InvalidCommandHandler{
        String handle(Player player, CommandResponse response);
    }
}
