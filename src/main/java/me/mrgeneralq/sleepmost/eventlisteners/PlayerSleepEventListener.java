package me.mrgeneralq.sleepmost.eventlisteners;

import me.mrgeneralq.sleepmost.enums.ConfigMessage;
import me.mrgeneralq.sleepmost.Sleepmost;
import me.mrgeneralq.sleepmost.interfaces.*;
import me.mrgeneralq.sleepmost.repositories.SleepFlagRepository;
import me.mrgeneralq.sleepmost.runnables.NightcycleAnimationTask;
import me.mrgeneralq.sleepmost.statics.DataContainer;
import me.mrgeneralq.sleepmost.statics.ServerVersion;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;


public class PlayerSleepEventListener implements Listener {

    private final Sleepmost main;
    private final ISleepService sleepService;
    private final IMessageService messageService;
    private final ICooldownService cooldownService;
    private final DataContainer dataContainer;

    public PlayerSleepEventListener(Sleepmost main, ISleepService sleepService, IMessageService messageService, ICooldownService cooldownService) {
        this.sleepService = sleepService;
        this.messageService = messageService;
        this.cooldownService = cooldownService;
        this.main = main;
        this.dataContainer = DataContainer.getContainer();
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerSleep(PlayerBedEnterEvent e) {

        Player player = e.getPlayer();
        World world = player.getWorld();

        //used to calculate sleeping players different for lower versions
        if (ServerVersion.CURRENT_VERSION.sleepCalculatedDifferently())
            DataContainer.getContainer().addSleepingPlayer(player);


        if (!sleepService.enabledForWorld(world)) {
            return;
        }

        if (!sleepService.resetRequired(world)) {
            return;
        }

        if (dataContainer.getRunningWorldsAnimation().contains(world))
            return;

        if (world.isThundering() && !SleepFlagRepository.getInstance().getStormSleepFlag().getController().getValueAt(world)) {

            String preventSleepStormMessage = messageService.getConfigMessage(ConfigMessage.NO_SLEEP_THUNDERSTORM);

            String stormSkipMessage = messageService.newBuilder(preventSleepStormMessage)
                    .usePrefix(false)
                    .setPlayer(player)
                    .setWorld(world)
                    .build();
            player.sendMessage(stormSkipMessage);

            e.setCancelled(true);
            return;
        }

        if(SleepFlagRepository.getInstance().getPreventSleepFlag().getController().getValueAt(world)) {

            String sleepPreventedConfigMessage = messageService.getConfigMessage(ConfigMessage.SLEEP_PREVENTED);

            player.sendMessage(messageService.newPrefixedBuilder(sleepPreventedConfigMessage)
                    .setPlayer(player)
                    .setWorld(world)
                    .build());

            e.setCancelled(true);
            return;
        }

        // check if player is cooling down, if not send message to world and start cooldown of player
        if (cooldownService.cooldownEnabled() && !cooldownService.isCoolingDown(player)) {
            messageService.sendPlayerLeftMessage(player, sleepService.getSleepSkipCause(world));
            cooldownService.startCooldown(player);
        }

        if (!sleepService.sleepPercentageReached(world))
            return;

        String lastSleeperName = e.getPlayer().getName();
        String lastSleeperDisplayName = e.getPlayer().getDisplayName();
		
        if (SleepFlagRepository.getInstance().getNightcycleAnimationFlag().getController().getValueAt(world)) {
        	if(world.isThundering() && !sleepService.isNight(world)){
        		sleepService.resetDay(world, lastSleeperName, lastSleeperDisplayName);
        		return;
			}

            //store running world
            dataContainer.getRunningWorldsAnimation().add(world);
            new NightcycleAnimationTask(sleepService, messageService, world, lastSleeperName).runTaskTimer(main, 0, 1);
            return;
        }

        sleepService.resetDay(world, lastSleeperName, lastSleeperDisplayName);
    }

    @EventHandler
    public void onPlayerBedLeave(PlayerBedLeaveEvent e) {

        Player player = e.getPlayer();

        //used to calculate sleeping players different for lower versions
        if (ServerVersion.CURRENT_VERSION.sleepCalculatedDifferently())
            DataContainer.getContainer().removeSleepingPlayer(player);

    }
}
