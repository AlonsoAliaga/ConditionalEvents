package ce.ajneb97.utils;

import ce.ajneb97.ConditionalEvents;
import ce.ajneb97.api.ConditionalEventsCallEvent;
import ce.ajneb97.libs.actionbar.ActionBarAPI;
import ce.ajneb97.libs.titles.TitleAPI;
import ce.ajneb97.managers.MessagesManager;
import ce.ajneb97.managers.dependencies.DiscordSRVManager;
import ce.ajneb97.model.StoredVariable;
import ce.ajneb97.model.internal.ExecutedEvent;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.*;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public class ActionUtils {

    public static void message(Player player,String actionLine){
        player.sendMessage(MessagesManager.getColoredMessage(actionLine));
    }

    public static void centeredMessage(Player player,String actionLine){
        actionLine = MessagesManager.getColoredMessage(actionLine);
        player.sendMessage(MessagesManager.getCenteredMessage(actionLine));
    }

    public static void consoleMessage(String actionLine){
        Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage(actionLine));
    }

    public static void jsonMessage(Player player,String actionLine){
        BaseComponent[] base = ComponentSerializer.parse(actionLine);
        player.spigot().sendMessage(base);
    }

    public static void consoleCommand(String actionLine){
        ConsoleCommandSender sender = Bukkit.getConsoleSender();
        Bukkit.dispatchCommand(sender, actionLine);
    }

    public static void playerCommand(Player player,String actionLine){
        player.performCommand(actionLine);
    }

    public static void playerCommandAsOp(Player player,String actionLine){
        boolean isOp = player.isOp();
        player.setOp(true);
        player.performCommand(actionLine);
        if(!isOp) {
            player.setOp(false);
        }
    }

    public static void playerSendChat(Player player,String actionLine){
        player.chat(MessagesManager.getColoredMessage(actionLine));
    }

    public static void sendToServer(Player player,String actionLine,ConditionalEvents plugin){
        plugin.getBungeeMessagingManager().sendToServer(player,actionLine);
    }

    public static void teleport(Player player, String actionLine, Event minecraftEvent){
        String[] sep = actionLine.split(";");
        World world = Bukkit.getWorld(sep[0]);
        double x = Double.valueOf(sep[1]);
        double y = Double.valueOf(sep[2]);
        double z = Double.valueOf(sep[3]);
        float yaw = Float.valueOf(sep[4]);
        float pitch = Float.valueOf(sep[5]);
        Location l = new Location(world,x,y,z,yaw,pitch);

        if(minecraftEvent instanceof PlayerRespawnEvent) {
            PlayerRespawnEvent respawnEvent = (PlayerRespawnEvent) minecraftEvent;
            respawnEvent.setRespawnLocation(l);
        }else {
            player.teleport(l);
        }
    }

    public static void removeItem(Player player,String actionLine){
        // remove_item: <id>;<amount>;datavalue: <datavalue>;name: <name>;lorecontains: <lore_line>
        // remove_item: %checkitem_remove...%
        if(actionLine.equals("no") || actionLine.equals("yes")) {
            //Variable already replaced and executed.
            return;
        }

        String[] sep = actionLine.split(";");
        String material = sep[0];
        int amount = Integer.valueOf(sep[1]);
        short datavalue = 0;
        String name = null;
        String loreContainsLoreLine = null;

        for(String sepLine : sep) {
            if(sepLine.startsWith("datavalue: ")) {
                datavalue = Short.valueOf(sepLine.replace("datavalue: ", ""));
            }else if(sepLine.startsWith("name: ")) {
                name = sepLine.replace("name: ", "");
            }else if(sepLine.startsWith("lorecontains: ")) {
                loreContainsLoreLine = sepLine.replace("lorecontains: ", "");
            }
        }

        ItemStack[] contents = player.getInventory().getContents();
        for(int i=0;i<contents.length;i++) {
            if(contents[i] != null && !contents[i].getType().equals(Material.AIR)) {
                if(!contents[i].getType().name().equals(material)) {
                    continue;
                }
                if(contents[i].getDurability() != datavalue) {
                    continue;
                }
                if(contents[i].hasItemMeta()) {
                    ItemMeta meta = contents[i].getItemMeta();
                    if(meta.hasDisplayName() && name != null
                        && !ChatColor.stripColor(meta.getDisplayName()).equals(ChatColor.stripColor(name))) {
                        continue;
                    }

                    if(meta.hasLore()) {
                        if(loreContainsLoreLine != null) {
                            List<String> lore = meta.getLore();
                            boolean contains = false;
                            for(String linea : lore) {
                                if(ChatColor.stripColor(linea).contains(loreContainsLoreLine)) {
                                    contains = true;
                                    break;
                                }
                            }
                            if(!contains) {
                                continue;
                            }
                        }
                    }
                }else {
                    if(name != null || loreContainsLoreLine != null) {
                        continue;
                    }
                }

                int currentAmount = contents[i].getAmount();
                if(currentAmount > amount) {
                    contents[i].setAmount(currentAmount-amount);
                    break;
                }else {
                    amount = amount-currentAmount;
                    if(!OtherUtils.isLegacy()) {
                        contents[i].setAmount(0);
                    }else {
                        player.getInventory().setItem(i, null);
                    }
                }
            }
        }
    }

    public static void givePotionEffect(Player player,String actionLine){
        String[] sep = actionLine.split(";");
        PotionEffectType potionEffectType = PotionEffectType.getByName(sep[0]);
        int duration = Integer.valueOf(sep[1]);
        int level = Integer.valueOf(sep[2])-1;
        boolean showParticles = true;
        if(sep.length >= 4) {
            showParticles = Boolean.valueOf(sep[3]);
        }
        PotionEffect effect = new PotionEffect(potionEffectType,duration,level,false,showParticles);
        player.addPotionEffect(effect);
    }

    public static void removePotionEffect(Player player,String actionLine){
        PotionEffectType potionEffectType = PotionEffectType.getByName(actionLine);
        player.removePotionEffect(potionEffectType);
    }

    public static void cancelEvent(String actionLine,Event minecraftEvent){
        boolean cancel = Boolean.valueOf(actionLine);
        if(minecraftEvent != null && minecraftEvent instanceof Cancellable) {
            Cancellable cancellableEvent = (Cancellable) minecraftEvent;
            cancellableEvent.setCancelled(cancel);
        }
    }

    public static void kick(Player player,String actionLine){
        player.kickPlayer(MessagesManager.getColoredMessage(actionLine));
    }

    public static void playSound(Player player,String actionLine){
        String[] sep = actionLine.split(";");
        Sound sound = null;
        int volume = 0;
        float pitch = 0;
        try {
            sound = Sound.valueOf(sep[0]);
            volume = Integer.valueOf(sep[1]);
            pitch = Float.valueOf(sep[2]);
        }catch(Exception e ) {
            Bukkit.getConsoleSender().sendMessage(ConditionalEvents.prefix+
                    MessagesManager.getColoredMessage(" &7Sound Name: &c"+sep[0]+" &7is not valid. Change it in the config!"));
            return;
        }

        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public static void playSoundResourcePack(Player player,String actionLine){
        String[] sep = actionLine.split(";");
        String sound = sep[0];
        int volume = Integer.valueOf(sep[1]);
        float pitch = Float.valueOf(sep[2]);
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public static void giveItem(Player player,String actionLine){
        // give_item: id:<id>;amount:<amount>;durability:<durability>;custom_model_data:<data>;name:<name>;
        // lore:<lore_line1>|<lore_lineN>;enchants:<name1>-<level1>|<nameN>-<levelN>;
        // flags:<flag1>|<flag2>

        String[] sep = actionLine.replace("give_item: ","").split(";");
        ItemStack item = ItemUtils.getItemFromProperties(sep);

        //Give Item
        player.getInventory().addItem(item);
    }

    public static void dropItem(String actionLine) {
        // drop_item: location:<x>,<y>,<z>,<world>;id:<id>;amount:<amount>;...

        String[] sep = actionLine.replace("drop_item: ","").split(";");
        ItemStack item = ItemUtils.getItemFromProperties(sep);
        Location location = null;

        // Find location
        for(String property : sep){
            if(property.startsWith("location:")){
                String[] locationSplit = property.replace("location:", "").split(",");
                location = new Location(
                        Bukkit.getWorld(locationSplit[3]),
                        Double.parseDouble(locationSplit[0]),
                        Double.parseDouble(locationSplit[1]),
                        Double.parseDouble(locationSplit[2])
                );
                break;
            }
        }

        //Drop Item
        if(location != null){
            location.getWorld().dropItemNaturally(location,item);
        }
    }

    public static void actionbar(Player player,String actionLine,ConditionalEvents plugin){
        String[] sep = actionLine.split(";");
        String text = sep[0];
        int duration = Integer.valueOf(sep[1]);
        ActionBarAPI.sendActionBar(player,text,duration,plugin);
    }

    public static void title(Player player,String actionLine){
        String[] sep = actionLine.split(";");
        int fadeIn = Integer.valueOf(sep[0]);
        int stay = Integer.valueOf(sep[1]);
        int fadeOut = Integer.valueOf(sep[2]);

        String title = sep[3];
        String subtitle = sep[4];
        if(title.equals("none")) {
            title = "";
        }
        if(subtitle.equals("none")) {
            subtitle = "";
        }
        TitleAPI.sendTitle(player,fadeIn,stay,fadeOut,title,subtitle);
    }

    public static void firework(Player player,String actionLine,ConditionalEvents plugin){
        // firework: colors:<color1>,<color2> type:<type> fade:<color1>,<color2> power:<power> location(optional):<x>;<y>;<z>;<world>
        ArrayList<Color> colors = new ArrayList<Color>();
        FireworkEffect.Type type = null;
        ArrayList<Color> fadeColors = new ArrayList<Color>();
        Location location = null;
        int power = 0;

        String[] sep = actionLine.split(" ");
        for(String s : sep) {
            if(s.startsWith("colors:")) {
                s = s.replace("colors:", "");
                String[] colorsSep = s.split(",");
                for(String colorSep : colorsSep) {
                    colors.add(OtherUtils.getFireworkColorFromName(colorSep));
                }
            }else if(s.startsWith("type:")) {
                s = s.replace("type:", "");
                type = FireworkEffect.Type.valueOf(s);
            }else if(s.startsWith("fade:")) {
                s = s.replace("fade:", "");
                String[] colorsSep = s.split(",");
                for(String colorSep : colorsSep) {
                    fadeColors.add(OtherUtils.getFireworkColorFromName(colorSep));
                }
            }else if(s.startsWith("power:")) {
                s = s.replace("power:", "");
                power = Integer.valueOf(s);
            }else if(s.startsWith("location:")) {
                String[] sep2 = s.replace("location:", "").split(";");
                location = new Location(
                   Bukkit.getWorld(sep2[3]), Double.parseDouble(sep2[0]), Double.parseDouble(sep2[1]), Double.parseDouble(sep2[2])
                );
            }
        }

        if(location == null){
            location = player.getLocation();
        }

        Firework firework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
        FireworkMeta fireworkMeta = firework.getFireworkMeta();
        FireworkEffect effect = FireworkEffect.builder().flicker(false)
                .withColor(colors)
                .with(type)
                .withFade(fadeColors)
                .build();
        fireworkMeta.addEffect(effect);
        fireworkMeta.setPower(power);
        firework.setFireworkMeta(fireworkMeta);
        firework.setMetadata("conditionalevents", new FixedMetadataValue(plugin, "no_damage"));
    }

    public static void particle(Player player,String actionLine){
        // particle: effect:<effect_name> offset:<x>;<y>;<z> speed:<speed> amount:<amount> location(optional): <x>;<y>;<z>;<world>
        String effectName = null;
        double offsetX = 0;double offsetY = 0;double offsetZ = 0;
        double speed = 0;
        int amount = 1;
        Location location = null;

        String[] sep = actionLine.split(" ");
        for(String s : sep) {
            if(s.startsWith("effect:")) {
                effectName = s.replace("effect:", "");
            }else if(s.startsWith("speed:")) {
                speed = Double.parseDouble(s.replace("speed:", ""));
            }else if(s.startsWith("amount:")) {
                amount = Integer.parseInt(s.replace("amount:", ""));
            }else if(s.startsWith("offset:")) {
                String[] sep2 = s.replace("offset:", "").split(";");
                offsetX = Double.parseDouble(sep2[0]);
                offsetY = Double.parseDouble(sep2[1]);
                offsetZ = Double.parseDouble(sep2[2]);
            }else if(s.startsWith("location:")) {
                String[] sep2 = s.replace("location:", "").split(";");
                location = new Location(
                        Bukkit.getWorld(sep2[3]), Double.parseDouble(sep2[0]), Double.parseDouble(sep2[1]), Double.parseDouble(sep2[2])
                );
            }
        }

        if(location == null){
            location = player.getLocation();
        }
        if(Bukkit.getVersion().contains("1.8")) {
            return;
        }

        try {
            if(effectName.startsWith("REDSTONE;")) {
                String[] effectSeparated = effectName.split(";");
                int red = Integer.parseInt(effectSeparated[1]);
                int green = Integer.parseInt(effectSeparated[2]);
                int blue = Integer.parseInt(effectSeparated[3]);
                Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(red,green,blue), 1);
                location.getWorld().spawnParticle(
                        Particle.REDSTONE,location,amount,offsetX,offsetY,offsetZ,speed,dustOptions,false);
            }else {
                location.getWorld().spawnParticle(
                        Particle.valueOf(effectName),location,amount,offsetX,offsetY,offsetZ,speed,null,false);
            }
        }catch(Exception e) {
            Bukkit.getConsoleSender().sendMessage(ConditionalEvents.prefix+
                    MessagesManager.getColoredMessage(" &7Particle Name: &c"+effectName+" &7is not valid. Change it in the config!"));
        }
    }

    public static void damage(Player player,String actionLine){
        player.damage(Double.parseDouble(actionLine));
    }

    public static void gamemode(Player player,String actionLine){
        player.setGameMode(GameMode.valueOf(actionLine));
    }

    public static void closeInventory(Player player){
        player.closeInventory();
    }

    public static void setOnFire(Player player,String actionLine){
        // set_on_fire: <duration_on_ticks>
        player.setFireTicks(Integer.parseInt(actionLine));
    }

    public static void wait(String actionLine, ExecutedEvent executedEvent){
        executedEvent.setOnWait(true);
        int timeSeconds = Integer.valueOf(actionLine);

        new BukkitRunnable(){
            @Override
            public void run() {
                executedEvent.continueWithActions();
            }
        }.runTaskLater(executedEvent.getPlugin(), timeSeconds*20);
    }

    public static void waitTicks(String actionLine, ExecutedEvent executedEvent){
        executedEvent.setOnWait(true);
        long timeTicks = Long.valueOf(actionLine);

        new BukkitRunnable(){
            @Override
            public void run() {
                executedEvent.continueWithActions();
            }
        }.runTaskLater(executedEvent.getPlugin(), timeTicks);
    }

    public static void keepItems(String actionLine,Event minecraftEvent){
        if(minecraftEvent instanceof PlayerDeathEvent) {
            PlayerDeathEvent deathEvent = (PlayerDeathEvent) minecraftEvent;
            if(actionLine.equals("items") || actionLine.equals("all")) {
                deathEvent.setKeepInventory(true);
                deathEvent.getDrops().clear();
            }
            if(actionLine.equals("xp") || actionLine.equals("all")) {
                deathEvent.setKeepLevel(true);
                deathEvent.setDroppedExp(0);
            }
        }
    }

    public static void cancelDrop(String actionLine,Event minecraftEvent){
        if(OtherUtils.isLegacy()){
            return;
        }
        if(minecraftEvent instanceof BlockBreakEvent) {
            BlockBreakEvent blockBreakEvent = (BlockBreakEvent) minecraftEvent;
            boolean cancel = Boolean.valueOf(actionLine);
            if(cancel){
                blockBreakEvent.setDropItems(false);
                blockBreakEvent.setExpToDrop(0);
            }
        }
    }

    public static void setDamage(String actionLine,Event minecraftEvent){
        if(minecraftEvent instanceof EntityDamageEvent) {
            EntityDamageEvent damageEvent = (EntityDamageEvent) minecraftEvent;
            double damage = damageEvent.getDamage();
            if(actionLine.contains("%")){
                double modifier = Double.parseDouble(actionLine.substring(0,actionLine.length()-1));
                double finalModifier = modifier/100;
                damage = damage*finalModifier;
            }else{
                damage = Double.parseDouble(actionLine);
            }

            damageEvent.setDamage(damage);
        }
    }

    public static void hideJoinMessage(String actionLine,Event minecraftEvent){
        if(minecraftEvent instanceof PlayerJoinEvent) {
            PlayerJoinEvent joinEvent = (PlayerJoinEvent) minecraftEvent;
            boolean hideMessage = Boolean.parseBoolean(actionLine);
            if(hideMessage){
                joinEvent.setJoinMessage(null);
            }
        }
    }

    public static void hideLeaveMessage(String actionLine,Event minecraftEvent){
        if(minecraftEvent instanceof PlayerQuitEvent) {
            PlayerQuitEvent quitEvent = (PlayerQuitEvent) minecraftEvent;
            boolean hideMessage = Boolean.parseBoolean(actionLine);
            if(hideMessage){
                quitEvent.setQuitMessage(null);
            }
        }
    }

    public static void setDeathMessage(String actionLine,Event minecraftEvent){
        if(minecraftEvent instanceof PlayerDeathEvent) {
            PlayerDeathEvent deathEvent = (PlayerDeathEvent) minecraftEvent;
            if(actionLine.equals("no")){
                deathEvent.setDeathMessage(null);
            }else{
                deathEvent.setDeathMessage(MessagesManager.getColoredMessage(actionLine));
            }
        }
    }

    public static void preventJoin(String actionLine,Event minecraftEvent){
        // prevent_join: <message>
        if(minecraftEvent instanceof AsyncPlayerPreLoginEvent) {
            AsyncPlayerPreLoginEvent preJoinEvent = (AsyncPlayerPreLoginEvent) minecraftEvent;
            preJoinEvent.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
            preJoinEvent.setKickMessage(MessagesManager.getColoredMessage(actionLine));
        }
    }

    public static void discordSRVEmbed(String actionLine,ConditionalEvents plugin){
        DiscordSRVManager discordSRVManager = plugin.getDependencyManager().getDiscordSRVManager();
        if(discordSRVManager != null){
            discordSRVManager.sendEmbedMessage(actionLine);
        }
    }

    public static boolean callEvent(String actionLine,Player player,ConditionalEvents plugin){
        // call_event: <event>;%variable1%=<value1>;%variable2%=<value2>
        ArrayList<StoredVariable> variables = new ArrayList<>();
        String eventName = null;
        try{
            String[] sep = actionLine.split(";");
            eventName = sep[0];
            if(sep.length > 1){
                for(int i=1;i<sep.length;i++){
                    String[] variableLineSep = sep[i].split("=");
                    variables.add(new StoredVariable(variableLineSep[0],variableLineSep[1]));
                }
            }
        }catch(Exception e){
            return false;
        }

        ConditionalEventsCallEvent event = new ConditionalEventsCallEvent(player,variables,eventName);
        plugin.getServer().getPluginManager().callEvent(event);
        return true;
    }

    public static void executeActionGroup(String actionLine,ExecutedEvent executedEvent,ConditionalEvents plugin){
        // execute_action_group: <group1>:<prob1>;<group2>:<prob2>
        ArrayList<String> actionGroups = new ArrayList<String>();
        ArrayList<Integer> probs = new ArrayList<Integer>();

        String[] sep = actionLine.split(";");
        for(String line : sep){
            String[] sep2 = line.split(":");
            actionGroups.add(sep2[0]);
            probs.add(Integer.parseInt(sep2[1]));
        }

        for(int i=0;i<probs.size();i++){
            if(i != 0){
                probs.set(i, probs.get(i-1)+probs.get(i));
            }
        }
        String actionGroup = null;

        Random random = new Random();
        int max = probs.get(probs.size()-1);
        int randomNumber = random.nextInt(max);
        for(int i=0;i<probs.size();i++){
            if(i==0){
                if(randomNumber < probs.get(i)){
                    actionGroup = actionGroups.get(i);
                }
            }else{
                if(randomNumber > probs.get(i-1) && randomNumber <= probs.get(i)){
                    actionGroup = actionGroups.get(i);
                }
            }
        }
        if(actionGroup == null){
            actionGroup = actionGroups.get(0);
        }

        //Clone ExecutedEvent
        new ExecutedEvent(
                executedEvent.getPlayer(),
                executedEvent.getEventVariables(),
                executedEvent.getEvent(),
                actionGroup,
                executedEvent.getMinecraftEvent(),
                executedEvent.getTarget(),
                plugin
        ).executeActions();
    }

    /*
    public static void setTabCompletions(String actionLine,Event minecraftEvent){
        // set_tab_completions: arg1,arg2,arg3
        if(minecraftEvent instanceof TabCompleteEvent) {
            TabCompleteEvent tabCompleteEvent = (TabCompleteEvent) minecraftEvent;



            preJoinEvent.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
            preJoinEvent.setKickMessage(MessagesManager.getColoredMessage(actionLine));
        }
    }
     */
}
